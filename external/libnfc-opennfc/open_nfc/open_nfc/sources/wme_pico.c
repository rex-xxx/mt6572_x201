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
   Contains the implementation of the Picopass and iClass Tags functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( PICO )

#include "wme_context.h"


#ifndef P_INCLUDE_PICOPASS

/*
 * Dummy functions that return W_ERROR_FUNCTION_NOT_IMPLEMENTED when PICOPASS support is not enabled
 */

/* See API */
W_ERROR PPicoGetConnectionInfo  (
   tContext * pContext,
   W_HANDLE  hConnection,
   tWPicoConnectionInfo *  pConnectionInfo )
{
   if (pConnectionInfo != null)
   {
      CMemoryFill(pConnectionInfo, 0, sizeof(* pConnectionInfo));
   }

   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See API */
W_ERROR PPicoIsWritable  (
   tContext * pContext,
   W_HANDLE  hConnection   )
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See API */
void PPicoRead  (
   tContext * pContext,
   W_HANDLE  hConnection,
   tPBasicGenericCallbackFunction *  pCallback,
   void *  pCallbackParameter,
   uint8_t *   pBuffer,
   uint32_t    nOffset,
   uint32_t    nLength,
   W_HANDLE *  phOperation
 )
{
   tDFCCallbackContext sCallbackContext;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &sCallbackContext);

   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

/* See API */
W_ERROR WPicoReadSync(
   W_HANDLE hConnection,
   uint8_t *pBuffer,
   uint32_t nOffset,
   uint32_t nLength )
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See API */

void PPicoWrite  (
   tContext * pContext,
   W_HANDLE  hConnection,
   tPBasicGenericCallbackFunction *  pCallback,
   void *  pCallbackParameter,
   const uint8_t *  pBuffer,
   uint32_t  nOffset,
   uint32_t  nLength,
   bool_t  bLockCard,
   W_HANDLE *  phOperation
 )
{
   tDFCCallbackContext sCallbackContext;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &sCallbackContext);

   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

W_ERROR WPicoWriteSync  ( W_HANDLE  hConnection,
  const uint8_t *  pBuffer,
  uint32_t  nOffset,
  uint32_t  nLength,
  bool_t  bLockCard
 )
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

#else

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) && (defined P_INCLUDE_PICOPASS)
/* Picopass page info */
#define P_PICO_PAGE_FIRST                    0x00
#define P_PICO_PAGE_SECOND                   0x10

/* Picopass block number */
#define P_PICO_BLOCK_NUMBER_2K               (256  / P_PICO_BLOCK_SIZE)
#define P_PICO_BLOCK_NUMBER_16K              (2048 / P_PICO_BLOCK_SIZE)
#define P_PICO_BLOCK_NUMBER_32K              (4096 / P_PICO_BLOCK_SIZE)


/* Picopass block info & size */
#define P_PICO_BLOCK_SERIAL_NUMBER           0x00
#define P_PICO_BLOCK_CONFIG                  0x01
#define P_PICO_BLOCK_APPLICATION             0x02
#define P_PICO_BLOCK_DATA                    0x03
#define P_FIRTS_FIVE_BLOCK_NBR               0x05

/* Picopass read or write actions */
#define P_PICO_ACTION_RESET                  0x01
#define P_PICO_ACTION_READ                   0x02
#define P_PICO_ACTION_UPDATE                 0x03
#define P_PICO_ACTION_UPDATE_CONFIG          0x04

/* Picopass command types */
#define P_PICO_COMMAND_SELECT_PAGE           0x03
#define P_PICO_COMMAND_UPDATE_CONFIG         0x05
#define P_PICO_COMMAND_READ                  0x06
#define P_PICO_COMMAND_UPDATE                0x07

/* Picopass protocol types */
#define P_PICO_PROTOCOL_TYPE0                0x00
#define P_PICO_PROTOCOL_TYPE1                0x01
#define P_PICO_PROTOCOL_TYPE2                0x02

/* Picopass ISO 14443 B level */
#define P_PICO_PROTOCOL_LEVEL2               0x00
#define P_PICO_PROTOCOL_LEVEL3               0x01

/* Picopass serial number length */
#define P_PICO_SERIAL_NUMBER_LENGTH          8

#define P_PICO_IDENTIFIER_LEVEL              ZERO_IDENTIFIER_LEVEL
#define P_PICO_HEADER_INDEX                  0x00
#define P_PICO_DATA_INDEX                    0x01
#define P_PICO_TAG_INFO_INDEX                0x10
#define P_PICO_HEADER_LENGTH                 P_PICO_SERIAL_NUMBER_LENGTH + (P_PICO_BLOCK_SIZE * 4)
#define P_PICO_CONFIG_STORED                 0x01
#define P_PICO_SERIAL_NUMBER_STORED          0x02
#define P_PICO_BLOCK_NUMBER_MAX              P_PICO_BLOCK_NUMBER_32K
#define P_PICO_TAG_INFO_LENGTH               P_PICO_BLOCK_NUMBER_MAX + 4


#define PICO_COMMAND_READ                    1
#define PICO_COMMAND_READ4                   2
#define PICO_COMMAND_UPDATE                  3

/* Queued operation type */
#define P_PICO_QUEUED_NONE                   0
#define P_PICO_QUEUED_READ                   1
#define P_PICO_QUEUED_LOCK                   2
#define P_PICO_QUEUED_WRITE                  3
#define P_PICO_QUEUED_WRITE_LOCK             4

/* Declare a Picopass exchange data structure */
typedef struct __tPicoConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;
   /* Connection handle */
   W_HANDLE                   hConnection;

   /* Connection information */
   uint8_t                    nSerialNumberLength;
   uint8_t                    aUID[P_PICO_SERIAL_NUMBER_LENGTH];
   uint8_t                    aSerialNumber[P_PICO_SERIAL_NUMBER_LENGTH];
   uint16_t                   nSectorSize;
   uint16_t                   nSectorNumber;
   bool_t                       bIsWritable;
   bool_t                       bLockCard;
   bool_t                       bIsSecured;
   bool_t                       bIsMultiApp;


   uint8_t                    nType;                     /**< Type of the card (2K, 16k, 32K) */
   uint8_t                    nPage;                     /**< Page selected (only available in 32K cards) */
   uint8_t                    nProtocolType;             /**< Protocol type of the card (15693-2 and/or 14443B-2 and/or 14443B-3) */
   uint8_t                    nProtocolLevel;            /**< Protocol level of the card (14443B-2 or 14443B-3) */

   uint8_t                    nCurrentLock[2];           /**< Current lock value, one per book */
   tSmartCache                sSmartCache;               /**< Smart cache */
   tDFCCallbackContext        sCacheCallbackContext;     /**< Callback context used by smart cache operations */

      /* used when a read or a write operation has been split in two parts */

   uint16_t                   nOffset;                   /* next operation start */
   uint16_t                   nLength;                   /* next operation length */
   uint8_t                  * pBuffer;                   /* next operation buffer */

   struct
   {
      uint8_t                 nOperation;                /**< The current smart cache operation (READ, READ4, UPDATE... ) */
      uint32_t                nSectorOffset;             /**< The offset of the first sector concerned by the current operation */
      uint32_t                nSectorNumber;             /**< The number of sectors concerned by the current operation */
      uint8_t               * pReadBuffer;               /**< The user buffer for the read operation */
      const uint8_t         * pWriteBuffer;              /**< The user buffer for the write operation */

   } sSmartCacheCurrentOperation;


   /* Command buffer */
   uint8_t                    aReaderToCardBuffer[NAL_MESSAGE_MAX_LENGTH];
   /* Response buffer */
   uint8_t                    aCardToReaderBuffer[NAL_MESSAGE_MAX_LENGTH];


   /* Operation handle */
   W_HANDLE hCurrentOperation;

   /* Callback context used by method called from user */
   tDFCCallbackContext        sCallbackContext;
   /* Callback context used by method called from another connection */
   tDFCCallbackContext        sCallbackContextInternal;

   /* Hold data of the queued operation which will be executed after the polling completion */
   struct __tQueuedOperation
   {
      /* Type of operation: Read, Lock, Write... */
      uint32_t             nType;
      /* Data */
      uint8_t*             pBuffer;
      uint32_t             nOffset;
      uint32_t             nLength;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hCurrentOperation;
   } sQueuedOperation;

} tPicoConnection;

/* Destroy connection callback */
static uint32_t static_PPicoDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static uint32_t static_PPicoGetPropertyNumber(
            tContext* pContext,
            void* pObject);

/* Get properties connection callback */
static bool_t static_PPicoGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_PPicoCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* Get identifier length */
static uint32_t static_PPicoGetIdentifierLength(
            tContext* pContext,
            void* pObject);

/* Get identifier */
static void static_PPicoGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer);

/* Send polling command */
static void static_PPicoPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

/* Execute the queued operation (after polling) */
static void static_PPicoExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult);

/* Handle registry Picopass connection type */
tHandleType g_sPicoConnection = {   static_PPicoDestroyConnection,
                                    null,
                                    static_PPicoGetPropertyNumber,
                                    static_PPicoGetProperties,
                                    static_PPicoCheckProperties,
                                    static_PPicoGetIdentifierLength,
                                    static_PPicoGetIdentifier,
                                    null,
                                    static_PPicoPoll };

#define P_HANDLE_TYPE_PICO_CONNECTION (&g_sPicoConnection)


static void static_PPicoExchangeData(
            tContext* pContext,
            tPicoConnection* pPicoConnection,
            uint32_t nCommandLength,
            tPBasicGenericDataCallbackFunction * pCallback);


static void static_PPicoReadConfigCompleted(
               tContext * pContext,
               void     * pCallbackParameter,
               W_ERROR    nError);

static void static_PPicoReadConfigCompleted2(
               tContext * pContext,
               void     * pCallbackParameter,
               W_ERROR    nError);

static void static_PPicoReadInternalCompleted(
               tContext * pContext,
               void * pCallbackParameter,
               W_ERROR nError);

static void static_PPicoWriteInternalCompleted(
               tContext * pContext,
               void * pCallbackParameter,
               W_ERROR nError);

static void static_PPicoSendResultInternal(
            tContext* pContext,
            tPicoConnection* pPicoConnection,
            W_ERROR nError);

static void static_PPicoAtomicRead(
               tContext * pContext,
               void * pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               uint8_t * pBuffer,
               tPBasicGenericCallbackFunction * pCallback,
               void * pCallbackParameter);

static void static_PPicoAtomicRead1(
               tContext * pContext,
               tPicoConnection * pPicoConnection);

static void static_PPicoAtomicReadPageChanged(
      tContext    * pContext,
      void        * pCallbackParameter,
      uint32_t      nDataLength,
      W_ERROR       nError
      );

static void static_PPicoAtomicReadCompleted(
               tContext * pContext,
               void * pCallbackParameter,
               uint32_t nDataLength,
               W_ERROR nError);

static void static_PPicoAtomicWrite(
               tContext * pContext,
               void * pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               const uint8_t * pBuffer,
               tPBasicGenericCallbackFunction * pCallback,
               void * pCallbackParameter);

static void static_PPicoAtomicWrite1(
               tContext * pContext,
               tPicoConnection * pPicoConnection);

static void static_PPicoAtomicWritePageChanged(
      tContext    * pContext,
      void        * pCallbackParameter,
      uint32_t      nDataLength,
      W_ERROR       nError
      );

static void static_PPicoAtomicWriteCompleted(
               tContext * pContext,
               void * pCallbackParameter,
               uint32_t nDataLength,
               W_ERROR nError);

static void static_PPicoSelectPage(
   tContext                         * pContext,
   tPicoConnection                  * pPicoConnection,
   tPBasicGenericDataCallbackFunction        * pCallback,
   uint32_t                           nPageNumber);

static void static_PPicoBook0Locked(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError);

static void static_PPicoBook1Locked(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError);

extern tSmartCacheSectorSize g_sSectorSize8;

static tSmartCacheDescriptor g_PicoSmartCacheDescriptor = {

   P_PICO_IDENTIFIER_LEVEL, &g_sSectorSize8,
   {
      { 1, 1, static_PPicoAtomicRead },      /* e.g READ operation */
      { 4, 4, static_PPicoAtomicRead }       /* e.g READ4 operation */
   },
   {
      { 1, 1, static_PPicoAtomicWrite },      /* e.g UPDATE operation */
      { 0, 0, null }
   },
};


/**
 * @brief Reads from a PicoPass Tag
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

static void static_PPicoAtomicRead(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      uint8_t                          * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tPicoConnection* pPicoConnection = (tPicoConnection*)pConnection;
   W_ERROR                    nError;

   PDebugTrace("static_PPicoAtomicRead [%d-%d]", nSectorOffset*P_PICO_BLOCK_SIZE, (nSectorOffset+nSectorNumber)*P_PICO_BLOCK_SIZE - 1);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pPicoConnection->sCacheCallbackContext);

   if ( (pPicoConnection->hCurrentOperation != W_NULL_HANDLE) && (PBasicGetOperationState(pContext, pPicoConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      /* The current read operation has been cancelled, do not perform the write operation */
      nError = W_ERROR_CANCEL;
      goto error;
   }

   switch (nSectorNumber)
   {
      case 1:

         /* Generates a READ command */
         pPicoConnection->sSmartCacheCurrentOperation.nOperation = PICO_COMMAND_READ;
         break;

      case 4 :

         /* Generates a READ4 command */
         pPicoConnection->sSmartCacheCurrentOperation.nOperation = PICO_COMMAND_READ4;
         break;

      default :
         PDebugError("static_PPicoAtomicRead : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   pPicoConnection->sSmartCacheCurrentOperation.nSectorOffset = nSectorOffset;
   pPicoConnection->sSmartCacheCurrentOperation.nSectorNumber = nSectorNumber;
   pPicoConnection->sSmartCacheCurrentOperation.pReadBuffer   = pBuffer;

   /* Check if we need to switch to the other page with a Picopass 32K card */

   /* We do not need to check if the read overlaps on 2 pages since we force an alignement of 4 sectors for READ4 operations,
      and the page size is multiple of 4 blocks ... */

   if ( pPicoConnection->nType == W_PROP_PICOPASS_32K )
   {
      if  (((nSectorOffset >= P_PICO_BLOCK_NUMBER_16K) && (pPicoConnection->nPage == P_PICO_PAGE_FIRST)) ||
           ((nSectorOffset < P_PICO_BLOCK_NUMBER_16K) && (pPicoConnection->nPage == P_PICO_PAGE_SECOND)))
         {

         /* The current page must be changed */

         if ( pPicoConnection->nPage == P_PICO_PAGE_FIRST )
         {
            static_PPicoSelectPage(pContext,  pPicoConnection, static_PPicoAtomicReadPageChanged, P_PICO_PAGE_SECOND);
         }
         else
         {
            static_PPicoSelectPage(pContext,  pPicoConnection, static_PPicoAtomicReadPageChanged, P_PICO_PAGE_FIRST);
         }

         return;
        }
   }

   /* if we reach this point, no need to change page, perform the read operation ... */
   static_PPicoAtomicRead1(pContext, pConnection);
   return;

error:

   /* Post the result */
   pPicoConnection->sSmartCacheCurrentOperation.pReadBuffer = null;
   PDFCPostContext2(&pPicoConnection->sCacheCallbackContext, nError);
}

/**
 * @brief Completion callback called when a page change operation has been completed
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallbackParameter  The callback parameter, eg pPicoConnection
 *
 * @param[in]  nError The result of the read operation
 *
 **/

static void static_PPicoAtomicReadPageChanged(
      tContext    * pContext,
      void        * pCallbackParameter,
      uint32_t      nDataLength,
      W_ERROR       nError
      )
{
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoAtomicReadPageChanged");

   if (nError == W_SUCCESS)
   {
      if ( (pPicoConnection->hCurrentOperation != W_NULL_HANDLE) && (PBasicGetOperationState(pContext, pPicoConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         /* The current read operation has been cancelled, do not perform the read operation */
         nError = W_ERROR_CANCEL;
      }
   }

   if (nError == W_SUCCESS)
   {

      /* The page change request has been performed, continue the read operation */
      static_PPicoAtomicRead1(pContext, pPicoConnection);
   }
   else
   {
      PDebugError("static_PPicoAtomicReadPageChanged failed %s", PUtilTraceError(nError));

      /* Post the result */
      pPicoConnection->sSmartCacheCurrentOperation.pReadBuffer   = null;
      PDFCPostContext2(&pPicoConnection->sCacheCallbackContext, nError);
   }
}

/**
 * @brief Performs the smart carche atomic read operation currently registered
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pPicoConnection  The connection
 *
 **/

static void static_PPicoAtomicRead1(
      tContext                         * pContext,
      tPicoConnection                  * pPicoConnection)
{
   static const uint8_t pRead[]        = { 0x0C, 0x00 };
   static const uint8_t pRead4[]       = { 0x06, 0x00 };

   PDebugTrace("static_PPicoAtomicRead1");

   if (pPicoConnection->sSmartCacheCurrentOperation.nSectorNumber == 1)
   {
      /* Generates a READ command */
      CMemoryCopy(& pPicoConnection->aReaderToCardBuffer[0], pRead, sizeof(pRead));
   }
   else
   {
      /* Generates a READ4 command */
      CMemoryCopy(& pPicoConnection->aReaderToCardBuffer[0], pRead4, sizeof(pRead4));
   }

      /* Get the first block */
   pPicoConnection->aReaderToCardBuffer[1] = (uint8_t)(pPicoConnection->sSmartCacheCurrentOperation.nSectorOffset % P_PICO_BLOCK_NUMBER_16K);

   /* Send the command */
   static_PPicoExchangeData( pContext, pPicoConnection, sizeof(pRead4), static_PPicoAtomicReadCompleted);

   return;
}

/**
 * @brief Completion callback called when a read operation has been completed
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallbackParameter  The callback parameter, eg pPicoConnection
 *
 * @param[in]  nDataLength  The length of data received
 *
 * @param[in]  nError The result of the read operation
 *
 **/

static void static_PPicoAtomicReadCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t   nDataLength,
   W_ERROR    nError)
{
   tPicoConnection* pPicoConnection = (tPicoConnection*)pCallbackParameter;

   PDebugTrace("static_PPicoAtomicReadCompleted");

   if (nError == W_SUCCESS)
   {
      switch (pPicoConnection->sSmartCacheCurrentOperation.nOperation)
      {
         case PICO_COMMAND_READ  :

            if (nDataLength != 8)
            {
               /* Invalid READ answer length */
               PDebugError("static_PPicoAtomicReadCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            CMemoryCopy(pPicoConnection->sSmartCacheCurrentOperation.pReadBuffer, & pPicoConnection->aCardToReaderBuffer[0], 8);
            break;

         case PICO_COMMAND_READ4 :

            if (nDataLength != 32)
            {
               /* Invalid READ4 answer length ! */
               PDebugError("static_PPicoAtomicReadCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            CMemoryCopy(pPicoConnection->sSmartCacheCurrentOperation.pReadBuffer, & pPicoConnection->aCardToReaderBuffer[0], 32);
            break;

         default :
            PDebugError("static_PPicoAtomicReadCompleted - unexpected event");
            nError = W_ERROR_BAD_STATE;
            break;
      }
   }
   else
   {
      PDebugError("static_PPicoAtomicReadCompleted %s", PUtilTraceError(nError));
   }

   pPicoConnection->sSmartCacheCurrentOperation.pReadBuffer = null;
   PDFCPostContext2(&pPicoConnection->sCacheCallbackContext, nError);
}


/**
 * @brief Writes in a Picopass chip
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

static void static_PPicoAtomicWrite(
   tContext                         * pContext,
   void                             * pConnection,
   uint32_t                           nSectorOffset,
   uint32_t                           nSectorNumber,
   const uint8_t                    * pBuffer,
   tPBasicGenericCallbackFunction   * pCallback,
   void                             * pCallbackParameter)
{
   tPicoConnection * pPicoConnection = (tPicoConnection *) pConnection;
   W_ERROR nError;

   PDebugTrace("static_PPicoAtomicWrite [%d-%d]", nSectorOffset*P_PICO_BLOCK_SIZE, (nSectorOffset+nSectorNumber) *P_PICO_BLOCK_SIZE - 1);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pPicoConnection->sCacheCallbackContext);

   if ( (pPicoConnection->hCurrentOperation != W_NULL_HANDLE) && (PBasicGetOperationState(pContext, pPicoConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      /* The current write operation has been cancelled, do not perform the write operation */
      nError = W_ERROR_CANCEL;
      goto error;
   }

   switch (nSectorNumber)
   {
      case 1:

         /* Generates a UPDATE command */
         pPicoConnection->sSmartCacheCurrentOperation.nOperation = PICO_COMMAND_UPDATE;
         break;

      default :
         PDebugError("static_PPicoAtomicWrite : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   pPicoConnection->sSmartCacheCurrentOperation.nSectorOffset = nSectorOffset;
   pPicoConnection->sSmartCacheCurrentOperation.nSectorNumber = nSectorNumber;
   pPicoConnection->sSmartCacheCurrentOperation.pWriteBuffer   = pBuffer;

   /* Check if we need to switch to the other page with a Picopass 32K card */

   /* We do not need to check if the read overlaps on 2 pages since we force an alignement of 4 sectors for READ4 operations,
      and the page size is multiple of 4 blocks ... */

   if ( pPicoConnection->nType == W_PROP_PICOPASS_32K )
   {
      if  (((nSectorOffset >= P_PICO_BLOCK_NUMBER_16K) && (pPicoConnection->nPage == P_PICO_PAGE_FIRST)) ||
           ((nSectorOffset < P_PICO_BLOCK_NUMBER_16K) && (pPicoConnection->nPage == P_PICO_PAGE_SECOND)))
         {
         /* The current page must be changed */

         if ( pPicoConnection->nPage == P_PICO_PAGE_FIRST )
         {
            static_PPicoSelectPage(pContext,  pPicoConnection, static_PPicoAtomicWritePageChanged, P_PICO_PAGE_SECOND);
         }
         else
         {
            static_PPicoSelectPage(pContext,  pPicoConnection, static_PPicoAtomicWritePageChanged, P_PICO_PAGE_FIRST);
         }

         return;
        }
   }

   /* if we reach this point, no need to change page, perform the write operation ... */
   static_PPicoAtomicWrite1(pContext, pConnection);
   return;

error:

   /* Post the result */
   pPicoConnection->sSmartCacheCurrentOperation.pWriteBuffer   = null;
   PDFCPostContext2(&pPicoConnection->sCacheCallbackContext, nError);
}


/**
 * @brief Completion callback called when a page change operation has been completed
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallbackParameter  The callback parameter, eg pPicoConnection
 *
 * @param[in]  nError The result of the read operation
 *
 **/

static void static_PPicoAtomicWritePageChanged(
      tContext    * pContext,
      void        * pCallbackParameter,
      uint32_t      nDataLength,
      W_ERROR       nError
      )
{
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoAtomicWritePageChanged");

   if (nError == W_SUCCESS)
   {

      if ( (pPicoConnection->hCurrentOperation != W_NULL_HANDLE) && (PBasicGetOperationState(pContext, pPicoConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         /* The current write operation has been cancelled, do not perform the write operation */
         nError = W_ERROR_CANCEL;
      }
   }

   if (nError == W_SUCCESS)
   {
      /* The page change request has been performed, continue the write operation */

      static_PPicoAtomicWrite1(pContext, pPicoConnection);
   }
   else
   {
      PDebugError("static_PPicoAtomicWritePageChanged failed %s", PUtilTraceError(nError));

      /* Post the result */
      pPicoConnection->sSmartCacheCurrentOperation.pWriteBuffer = null;
      PDFCPostContext2(&pPicoConnection->sCacheCallbackContext, nError);
   }
}


/**
 * @brief Performs the smart cache atomic write operation currently registered
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pPicoConnection  The connection
 *
 **/

static void static_PPicoAtomicWrite1(
      tContext                         * pContext,
      tPicoConnection                  * pPicoConnection)
{
   static const uint8_t pUpdate[]      = { 0x07 | 0x80, 0x00 };

   PDebugTrace("static_PPicoAtomicWrite1");

   /* Generates a UPDATE command */
   CMemoryCopy(& pPicoConnection->aReaderToCardBuffer[0], pUpdate, sizeof(pUpdate));

      /* Get the first block */
   pPicoConnection->aReaderToCardBuffer[1] = (uint8_t)(pPicoConnection->sSmartCacheCurrentOperation.nSectorOffset % P_PICO_BLOCK_NUMBER_16K);

   CMemoryCopy(&pPicoConnection->aReaderToCardBuffer[2], pPicoConnection->sSmartCacheCurrentOperation.pWriteBuffer, 8);

   /* Send the command */
   static_PPicoExchangeData( pContext, pPicoConnection, sizeof(pUpdate) + 8, static_PPicoAtomicWriteCompleted);
}

/**
 * @brief Called when a Write operation has been completed on a Picopass
 *
 * @param[in]  pContext The context.
 *
 * @param[in]  pCallbackParameter The callback parameter, eg the Type 1 Chip connection.
 *
 * @param[in]  nDataLength The read data length
 *
 * @param[in]  nError The operation result code.
 */

static void static_PPicoAtomicWriteCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t   nDataLength,
   W_ERROR    nError)
{

   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoAtomicWriteCompleted");

   if (nError == W_SUCCESS)
   {
      switch (pPicoConnection->sSmartCacheCurrentOperation.nOperation)
      {
         case PICO_COMMAND_UPDATE  :

            if (nDataLength != 8)
            {
               /* Invalid WRITE answer length */
               PDebugError("static_PPicoAtomicWriteCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }
            break;

         default :
            PDebugError("static_PPicoAtomicWriteCompleted - unexpected event");
            nError = W_ERROR_BAD_STATE;
            break;
      }
   }
   else
   {
      PDebugError("static_PPicoAtomicWriteCompleted %s", PUtilTraceError(nError));
   }

   pPicoConnection->sSmartCacheCurrentOperation.pWriteBuffer = null;
   PDFCPostContext2(&pPicoConnection->sCacheCallbackContext, nError);
}

/**
 * @brief Changes the current page for PicoPass 32K
 *
 * @param[in] pContext The context.
 *
 * @param[in] pPicoConnection The connection
 *
 * @param[in] pCallback The callback function to be called once change page has been completed
 *
 * @param[in] pCallbackParameter The callback parameter
 *
 * @param[in] nPageNumber The page number to select
 */

static void static_PPicoSelectPage(
   tContext                         * pContext,
   tPicoConnection                  * pPicoConnection,
   tPBasicGenericDataCallbackFunction        * pCallback,
   uint32_t                           nPageNumber
   )
{
   static const uint8_t pSelectPage[]  = { 0x04 | 0x80, 0x00 }; /* Tag + book/page number 0 */

   PDebugTrace("static_PPicoSelectPage: P_PICO_COMMAND_SELECT_PAGE %d", nPageNumber);

   /* Prepare the command */

   CMemoryCopy(pPicoConnection->aReaderToCardBuffer, pSelectPage, sizeof(pSelectPage) );

   pPicoConnection->nPage = (uint8_t)nPageNumber;

   /* Get the first block */
   pPicoConnection->aReaderToCardBuffer[1] = (uint8_t)nPageNumber;

   static_PPicoExchangeData( pContext, pPicoConnection, sizeof(pSelectPage), pCallback);
}


/**
 * @brief   Destroyes a Picopass connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/

static uint32_t static_PPicoDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tPicoConnection* pPicoConnection = (tPicoConnection*)pObject;

   PDebugTrace("static_PPicoDestroyConnection");

      /* destroy the cache */
   PSmartCacheDestroyCache(pContext, & pPicoConnection->sSmartCache);

      /* flush the callbacks */
   PDFCFlushCall(&pPicoConnection->sCallbackContext);

   /* Free the Picopass connection structure */
   CMemoryFree( pPicoConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the Picopass connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static uint32_t static_PPicoGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/**
 * @brief   Gets the Picopass connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PPicoGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tPicoConnection* pPicoConnection = (tPicoConnection*)pObject;

   PDebugTrace("static_PPicoGetProperties");

   if ( pPicoConnection->nType == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_PPicoGetProperties: no property");
      return W_FALSE;
   }

   pPropertyArray[0] = pPicoConnection->nType;
   return W_TRUE;
}

/**
 * @brief   Checkes the Picopass connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PPicoCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tPicoConnection* pPicoConnection = (tPicoConnection*)pObject;

   PDebugTrace(
      "static_PPicoCheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if ( pPicoConnection->nType != P_PROTOCOL_STILL_UNKNOWN )
   {
      if ( nPropertyValue == pPicoConnection->nType )
      {
         return W_TRUE;
      }
      else
      {
         return W_FALSE;
      }
   }
   else
   {
      PDebugError("static_PPicoCheckProperties: no property");
      return W_FALSE;
   }
}

/**
 * @brief   Parses the configuration file.
 *
 * @return  pPicoConnection  The Picopass connection structure.
 *
 * @param[in]  pCardToReaderBuffer  The reception buffer to parse.
 *
 * @param[in]  nDataLength  The actual length in bytes of the buffer to parse.
 **/
static W_ERROR static_PPicoParseConfig(
            tPicoConnection* pPicoConnection,
            uint8_t* pCardToReaderBuffer,
            uint32_t nDataLength )
{
   PDebugTrace("static_PPicoParseConfig: pCardToReaderBuffer");
   PDebugTraceBuffer(pCardToReaderBuffer, nDataLength);

   /* Byte 3: Get the read only mode */
   if ( pCardToReaderBuffer[3] & 0x80 )
   {
      PDebugTrace("static_PPicoParseConfig: card not locked");
      pPicoConnection->bIsWritable = W_TRUE;
   }
   else
   {
      PDebugTrace("static_PPicoParseConfig: card locked");
      pPicoConnection->bIsWritable = W_FALSE;
   }

   /* Get the current lock value */
   pPicoConnection->nCurrentLock[0] = pCardToReaderBuffer[3];

   /* Set the sector size */
   pPicoConnection->nSectorSize = P_PICO_BLOCK_SIZE;

   /* Byte 7: Fuses */
   /* Store the protocol type */
   pPicoConnection->nProtocolType = ( pCardToReaderBuffer[7] >> 5 ) & 0x03;
   switch ( pPicoConnection->nProtocolType )
   {
      case P_PICO_PROTOCOL_TYPE0:
         PDebugTrace("static_PPicoParseConfig: ISO 14443-3 B");
         break;
      case P_PICO_PROTOCOL_TYPE1:
         PDebugTrace("static_PPicoParseConfig: ISO 15693-2 / ISO 14443-3 B");
         break;
      case P_PICO_PROTOCOL_TYPE2:
         PDebugTrace("static_PPicoParseConfig: ISO 15693-2");
         break;
   }
   /* Check the security level */
   if ( ( pCardToReaderBuffer[7] & 0x18 ) != 0x08 )
   {
      PDebugError("static_PPicoParseConfig: secured");
      pPicoConnection->bIsSecured = W_TRUE;
   }
   else
   {
      PDebugTrace("static_PPicoParseConfig: not secured");
      pPicoConnection->bIsSecured = W_FALSE;
   }

   /* Byte 5: Memory configuration */
   /* ISO14443-2 or 3 */
   if ( pCardToReaderBuffer[5] & 0x40 )
   {
      pPicoConnection->nProtocolLevel = P_PICO_PROTOCOL_LEVEL2;
      PDebugTrace("static_PPicoParseConfig: ISO 14443 B 2");
   }
   else
   {
      pPicoConnection->nProtocolLevel = P_PICO_PROTOCOL_LEVEL3;
      PDebugTrace("static_PPicoParseConfig: W_PROP_ISO_14443_3_B");
   }
   /* Check the 16k bit */
   if ( pCardToReaderBuffer[5] & 0x80 )
   {
      /* Check if the multi-application mode is not configured */
      if ( ( pCardToReaderBuffer[4] & 0x10 ) == 0x10 )
      {
         PDebugTrace("static_PPicoParseConfig: multi-application mode configured");
         pPicoConnection->bIsMultiApp = W_TRUE;
      }
      else
      {
         PDebugTrace("static_PPicoParseConfig: multi-application mode not configured");
         pPicoConnection->bIsMultiApp = W_FALSE;
      }

      /* Check the number of book */
      if ( pCardToReaderBuffer[5] & 0x20 )
      {
         pPicoConnection->nType = W_PROP_PICOPASS_32K;
         pPicoConnection->nSectorNumber = P_PICO_BLOCK_NUMBER_32K;
         PDebugTrace(
            "static_PPicoParseConfig: 32Kbits (%d sector number)",
            pPicoConnection->nSectorNumber );
      }
      else
      {
         pPicoConnection->nType = W_PROP_ICLASS_16K;
         pPicoConnection->nSectorNumber = P_PICO_BLOCK_NUMBER_16K;
         PDebugTrace(
            "static_PPicoParseConfig: 16Kbits (%d sector number)",
            pPicoConnection->nSectorNumber );
      }
   }
   else
   {
      /* Check the 2k bit */
      if ( pCardToReaderBuffer[5] & 0x10 )
      {
         /* If the card is IOS15693-2 only */
         if (pPicoConnection->nProtocolType == P_PICO_PROTOCOL_TYPE2 )
         {
            /* Wait for the serial number information to differentiate */
            /* the Picotag 2K & iClass 2K cards */
            pPicoConnection->nType = W_PROP_ICLASS_2K;
         }
         else
         {
            pPicoConnection->nType = W_PROP_PICOPASS_2K;
         }
         pPicoConnection->nSectorNumber = P_PICO_BLOCK_NUMBER_2K;
         PDebugTrace(
            "static_PPicoParseConfig: 2Kbits (%d sector number)",
            pPicoConnection->nSectorNumber );
      }
      else
      {
         pPicoConnection->nType = P_PROTOCOL_STILL_UNKNOWN;
         PDebugError("static_PPicoParseConfig: unknown size");
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }
   }

   return W_SUCCESS;
}


static W_ERROR static_PPicoParseConfig2(
            tPicoConnection* pPicoConnection,
            uint8_t* pCardToReaderBuffer,
            uint32_t nDataLength )
{
   PDebugTrace("static_PPicoParseConfig2: pCardToReaderBuffer");
   PDebugTraceBuffer(pCardToReaderBuffer, nDataLength);

#if 0 /* @todo */

   /* Do not update the bIsWritable */

   if (pPicoConnection->bIsWritable != W_FALSE)
   {
      /* Byte 3: Get the read only mode */
      if ( (pCardToReaderBuffer[3] & 0x80) == 0)
      {
         PDebugTrace("static_PPicoParseConfig2: card locked");
         pPicoConnection->bIsWritable = W_FALSE;
      }
   }

#endif /* 0 */

   /* Get the current lock value */
   pPicoConnection->nCurrentLock[1] = pCardToReaderBuffer[3];

   return W_SUCCESS;
}

/**
 * @brief   Parses the serial number file.
 *
 * @return  pPicoConnection  The Picopass connection structure.
 *
 * @param[in]  pSerialNumber  The serial number to parse.
 *
 * @param[in]  nLength  The actual length in bytes of the serial number to parse.
 *
 * @reference: "INS_TST_002", v.1.1.
 **/
static W_ERROR static_PPicoParseSerialNumber(
            tPicoConnection* pPicoConnection,
            uint8_t* pSerialNumber )
{
   uint16_t nClientId;

   PDebugTrace("static_PPicoParseSerialNumber: pSerialNumber");
   PDebugTraceBuffer(pSerialNumber, P_PICO_SERIAL_NUMBER_LENGTH);

   /* Store the UID */
   CMemoryCopy(
      pPicoConnection->aUID,
      pSerialNumber,
      P_PICO_SERIAL_NUMBER_LENGTH );

   /* Check if it is a correct Registration Category, Allocation Class */
   /* and IC Manufacturer Registration Number */
   if (  (  ( pSerialNumber[7] == 0xE0 )      /* since 07/03/2002 */
         && ( pSerialNumber[6] == 0x12 ) )
      || (  ( pSerialNumber[7] == 0x00 )      /* 10/08/1999 - 07/03/2002 */
         && ( pSerialNumber[6] == 0x0C ) )
      || (  ( pSerialNumber[7] == 0x00 )      /* before 10/08/1999 */
         && ( pSerialNumber[6] == 0x00 ) ) )
   {
      /* Get the client identifier */
      nClientId   = (uint16_t)( pSerialNumber[5] << 4 )
                  + (uint16_t)( ( pSerialNumber[4] & 0xF0 ) >> 4 );
      /* Check if it is an Inside or a HID card */
      switch (nClientId)
      {
         /* Inside */
         case 0x0000:
            PDebugTrace("static_PPicoParseSerialNumber: Inside");
            /* Check the type consistency */
            if (  ( pPicoConnection->nType != W_PROP_PICOPASS_2K )
               && ( pPicoConnection->nType != W_PROP_PICOPASS_32K ) )
            {
               PDebugError(
                  "static_PPicoParseSerialNumber: incorrect type 0x%02X",
                  pPicoConnection->nType );
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }
            break;
         /* Others */
         default:
            /* Check if it is a known or new HID identification client code */
            if ( pSerialNumber[5] == 0xFF )
            {
               /* Known values */
               if (  ( nClientId == 0x0FFF )
                  || ( nClientId == 0x0FFE )
                  || ( nClientId == 0x0FFD )
                  || ( nClientId == 0x0FFC ) )
               {
                  PDebugTrace("static_PPicoParseSerialNumber: known HID code");
               }
               else
               {
                  PDebugWarning(
                     "static_PPicoParseSerialNumber: new HID code (0x%04X)?",
                     nClientId );
               }
               /* Set the correct card type if needed */
               if ( pPicoConnection->nProtocolType == P_PICO_PROTOCOL_TYPE2 )
               {
                  pPicoConnection->nType = W_PROP_ICLASS_2K;
               }
               /* Check the type consistency */
               if (  ( pPicoConnection->nType != W_PROP_ICLASS_2K )
                  && ( pPicoConnection->nType != W_PROP_ICLASS_16K ) )
               {
                  PDebugError(
                     "static_PPicoParseSerialNumber: incorrect type 0x%02X",
                     pPicoConnection->nType );
                  return W_ERROR_CONNECTION_COMPATIBILITY;
               }
               return W_SUCCESS;
            }
            PDebugError(
               "static_PPicoParseSerialNumber: incorrect identification 0x%02X",
               pSerialNumber[5] );
            return W_ERROR_CONNECTION_COMPATIBILITY;
      }
   }
   else
   {
      PDebugError(
         "static_PPicoParseSerialNumber: incorrect header 0x%02X%02X",
         pSerialNumber[7],
         pSerialNumber[6] );
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   return W_SUCCESS;
}

/**
 * @brief   Locks the card.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pPicoConnection  The Picopass connection.
 **/
static void static_PPicoLockCard(
            tContext* pContext,
            tPicoConnection* pPicoConnection )
{
   PDebugTrace("static_PPicoLockCard");

   pPicoConnection->nCurrentLock[0] = pPicoConnection->nCurrentLock[0] & 0x7F;

   PSmartCacheWrite(pContext, &pPicoConnection->sSmartCache, P_PICO_BLOCK_CONFIG * P_PICO_BLOCK_SIZE + 3, 1, &pPicoConnection->nCurrentLock[0], static_PPicoBook0Locked, pPicoConnection);
}

/**
  * @brief Completion routine called when the Book0 lock operation has been completed
  *
  * @param[in] pContext The context
  *
  * @param[in] pCallbackParameter The callback parameter, eg pPicoConnection
  *
  * @param[in] nError The result of the operation
  */

static void static_PPicoBook0Locked(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoBook0Locked");

   if (nError == W_SUCCESS)
   {

      if ( pPicoConnection->nType == W_PROP_PICOPASS_32K )
      {
         pPicoConnection->nCurrentLock[1] = pPicoConnection->nCurrentLock[1] & 0x7F;

         PSmartCacheWrite(pContext, & pPicoConnection->sSmartCache, (P_PICO_BLOCK_NUMBER_16K + P_PICO_BLOCK_CONFIG) * P_PICO_BLOCK_SIZE + 3, 1, & pPicoConnection->nCurrentLock[1], static_PPicoBook1Locked, pPicoConnection);
         return;
      }

      pPicoConnection->bIsWritable = W_FALSE;
   }
   else
   {
      PDebugError("static_PPicoBook0Locked failed %s", PUtilTraceError(nError));
   }

   static_PPicoSendResultInternal(pContext, pPicoConnection, nError);
}

/**
  * @brief Completion routine called when the Book1 lock operation has been completed
  *
  * @param[in] pContext The context
  *
  * @param[in] pCallbackParameter The callback parameter, eg pPicoConnection
  *
  * @param[in] nError The result of the operation
  */


static void static_PPicoBook1Locked(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoBook1Locked");

   if (nError == W_SUCCESS)
   {
      pPicoConnection->bIsWritable = W_FALSE;
   }
   else
   {
      PDebugError("static_PPicoBook1Locked failed %s", PUtilTraceError(nError));
   }

   /* send the result of the operation */
   static_PPicoSendResultInternal(pContext, pPicoConnection, nError);
}

/**
  * Cancels the current read/write operation
  */

static void static_PPicoCancelOperation(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   PDebugTrace("static_PPicoCancelOperation");
}

/* See tWBasicGenericCallbackFunction */
static void static_PPicoOperationCompleted(
            tContext* pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoOperationCompleted");

   if (pPicoConnection->hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pPicoConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PPicoOperationCompleted: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pPicoConnection->hCurrentOperation);
      PHandleClose(pContext, pPicoConnection->hCurrentOperation);
      pPicoConnection->hCurrentOperation = W_NULL_HANDLE;
   }

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, pPicoConnection->hConnection);

   /* Send the error */
   PDFCPostContext2(&pPicoConnection->sCallbackContext, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pPicoConnection);
}

/**
 * Sends the result of a read/write operation
 *
 * @param[in]  pContext  The current constext.
 *
 * @param[in]  pPicoConnection  the connection
 *
 * @param[in]  nError  The error code.
 **/
static void static_PPicoSendResultInternal(
            tContext* pContext,
            tPicoConnection* pPicoConnection,
            W_ERROR nError)
{

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PPicoSendResultInternal: Returning %s", PUtilTraceError(nError));
   }
   else
   {
      PDebugTrace("static_PPicoSendResultInternal");
   }

   if (pPicoConnection->hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pPicoConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PPicoSendResultInternal: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pPicoConnection->hCurrentOperation);
      PHandleClose(pContext, pPicoConnection->hCurrentOperation);
      pPicoConnection->hCurrentOperation = W_NULL_HANDLE;
   }

   /* Send the error */
   PDFCPostContext2(&pPicoConnection->sCallbackContextInternal, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pPicoConnection);
}

/**
 * @brief Exchanges data with the Picopass card.
 *
 * @param[in]  pContext  The current context.
 *
 * @param(in]  pPicoConnection  The connection.
 *
 * @param[in]  nCommandLength  The length in bytes of the command sent to the card.
 *
 * @param[in]  nResponseMaxLength  The maximum length in bytes of the response
 *             returned by the card.
 *
 * @param[in]  bTrueCall  Set to W_TRUE to perform a W_TRUE call or W_FALSE to simulate
 *             a call.
 **/
static void static_PPicoExchangeData(
            tContext* pContext,
            tPicoConnection* pPicoConnection,
            uint32_t nCommandLength,
            tPBasicGenericDataCallbackFunction * pCallback
            )
{
   W_HANDLE hSubOperation = W_NULL_HANDLE;
   W_HANDLE* phSubOperation;

   PDebugTrace("static_PPicoExchangeData");

   if(pPicoConnection->hCurrentOperation != W_NULL_HANDLE)
   {
      phSubOperation = &hSubOperation;
   }
   else
   {
      phSubOperation = null;
   }

   P14P3UserExchangeData(
      pContext,
      pPicoConnection->hConnection,
      pCallback,
      pPicoConnection,
      pPicoConnection->aReaderToCardBuffer,
      nCommandLength,
      pPicoConnection->aCardToReaderBuffer,
      sizeof(pPicoConnection->aCardToReaderBuffer),
      phSubOperation,
      W_TRUE,
      W_FALSE);

   if((pPicoConnection->hCurrentOperation != W_NULL_HANDLE)
      && (hSubOperation != W_NULL_HANDLE))
   {
      if(PBasicAddSubOperationAndClose(pContext,
         pPicoConnection->hCurrentOperation, hSubOperation) != W_SUCCESS)
      {
         PDebugError(
         "static_PPicoExchangeData: erro returned by PBasicAddSubOperationAndClose(), ignored");
      }
   }
}

/** See tPReaderUserRemoveSecondaryConnection */
void PPicoRemoveConnection(
            tContext* pContext,
            W_HANDLE hUserConnection )
{
   tPicoConnection* pPicoConnection = (tPicoConnection*)PHandleRemoveLastHeir(
            pContext, hUserConnection,
            P_HANDLE_TYPE_PICO_CONNECTION);

   PDebugTrace("PPicoRemoveConnection");

   /* Remove the connection object */
   if(pPicoConnection != null)
   {
      CMemoryFree(pPicoConnection);
   }
}

/* See Header file */
void PPicoCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty )
{
   tPicoConnection* pPicoConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PPicoCreateConnection");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the NDEF buffer */
   pPicoConnection = (tPicoConnection*)CMemoryAlloc( sizeof(tPicoConnection) );
   if ( pPicoConnection == null )
   {
      PDebugError("PPicoCreateConnection: pPicoConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto send_error;
   }
   CMemoryFill(pPicoConnection, 0, sizeof(tPicoConnection));

   /* Store the expected property */
   pPicoConnection->nType = nProperty;

   /* Store the connection information */
   pPicoConnection->hConnection = hConnection;

   /* create a smart cache */
   nError = PSmartCacheCreateCache(pContext, &pPicoConnection->sSmartCache, P_PICO_BLOCK_NUMBER_32K, &g_PicoSmartCacheDescriptor, pPicoConnection);
   if (nError != W_SUCCESS)
   {
      PDebugError("PPicoCreateConnection : PSmartCacheCreateCache failed %s", PUtilTraceError(nError));
      goto send_error;
   }

   /* Get the serial number */
   if ( ( nError = P14P3UserCheckPico(
                     pContext,
                     hConnection,
                     pPicoConnection->aSerialNumber,
                     &pPicoConnection->nSerialNumberLength ) ) != W_SUCCESS )
   {
      PDebugError("PPicoCreateConnection: P144433GetPicoSerialNumber returns an error");
      goto send_error;
   }
   else
   {
      /* check the serial number length */
      if ( pPicoConnection->nSerialNumberLength == P_PICO_SERIAL_NUMBER_LENGTH )
      {
         /* Parse the serial number information */
         if ( ( nError = static_PPicoParseSerialNumber(
                           pPicoConnection,
                           pPicoConnection->aSerialNumber ) ) != W_SUCCESS )
         {
            PDebugError("PPicoCreateConnection: static_PPicoParseSerialNumber returns an error");
            goto send_error;
         }
      }
      else
      {
         PDebugError("PPicoCreateConnection: wrong serial number length 0x%02X",
            pPicoConnection->nSerialNumberLength );
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         goto send_error;
      }
   }

   /* Add the Pico connection structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hConnection,
                     pPicoConnection,
                     P_HANDLE_TYPE_PICO_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("PPicoCreateConnection: could not add the Pico buffer");
      goto send_error;
   }

   /* Store the callback context */
   pPicoConnection->sCallbackContextInternal = sCallbackContext;

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PPicoSendResultInternal when the operation is completed */
   PHandleIncrementReferenceCount(pPicoConnection);

   /* Get the config information */
   PSmartCacheRead(pContext, &pPicoConnection->sSmartCache, P_PICO_BLOCK_CONFIG * P_PICO_BLOCK_SIZE, P_PICO_BLOCK_SIZE,
                        null /* no buffer, just fill in the cache */,
                        static_PPicoReadConfigCompleted, pPicoConnection);

   return;

send_error:

   PDebugError(
      "PPicoCreateConnection: Sending the error %s",
      PUtilTraceError(nError) );

   /* Send the error */
   PDFCPostContext2(
      &sCallbackContext,
      nError );

   CMemoryFree(pPicoConnection);
}

/**
 * @brief Comletion routine called when the configuration sector has been read
 *
 * @param[in] pContext The context
 *
 * @param[in] pCallbackParameter The callback parameter, eg pPicoConnection
 *
 * @param[in] nError The result of the operation
 */

static void static_PPicoReadConfigCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   uint8_t * pBuffer;
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoReadConfigCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PPicoReadConfigCompleted failed %s\n", PUtilTraceError(nError));
      goto send_result;
   }

   pBuffer = PSmartCacheGetBuffer(pContext, &pPicoConnection->sSmartCache, P_PICO_BLOCK_CONFIG * P_PICO_BLOCK_SIZE, P_PICO_BLOCK_SIZE);

   if (pBuffer == null)
   {
      /* should not occur */
      PDebugError("static_PPicoReadConfigCompleted = PSmartCacheGetBuffer failed");

      nError = W_ERROR_BAD_STATE;
      goto send_result;
   }

   /* Parse the information */
   nError = static_PPicoParseConfig(pPicoConnection, pBuffer, P_PICO_BLOCK_SIZE);

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PPicoReadConfigCompleted = static_PPicoParseConfig %s", PUtilTraceError(nError));
      goto send_result;
   }


   /* read the configuration of the second book */

   if (pPicoConnection->nType == W_PROP_PICOPASS_32K )
   {
      PSmartCacheRead(pContext, &pPicoConnection->sSmartCache,
                           (P_PICO_BLOCK_NUMBER_16K + P_PICO_BLOCK_CONFIG) * P_PICO_BLOCK_SIZE, P_PICO_BLOCK_SIZE,
                           null, static_PPicoReadConfigCompleted2, pPicoConnection);

      return;
   }

   /* all is fine */
   nError = W_SUCCESS;

send_result:

   switch(nError)
   {
      case W_SUCCESS:
         break;

      case W_ERROR_RF_COMMUNICATION:
      case W_ERROR_RF_PROTOCOL_NOT_SUPPORTED:
      case W_ERROR_TIMEOUT:
         nError = W_ERROR_RF_COMMUNICATION;
         break;

      default:
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         break;
   }

   static_PPicoSendResultInternal(pContext, pPicoConnection, nError);
}


static void static_PPicoReadConfigCompleted2(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   uint8_t * pBuffer;
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoReadConfigCompleted2");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PPicoReadConfigCompleted2 failed %s\n", PUtilTraceError(nError));
      goto send_result;
   }

   pBuffer = PSmartCacheGetBuffer(pContext, &pPicoConnection->sSmartCache, (P_PICO_BLOCK_NUMBER_16K + P_PICO_BLOCK_CONFIG) * P_PICO_BLOCK_SIZE, P_PICO_BLOCK_SIZE);

   if (pBuffer == null)
   {
      /* should not occur */
      PDebugError("static_PPicoReadConfigCompleted2 = PSmartCacheGetBuffer failed");

      nError = W_ERROR_BAD_STATE;
      goto send_result;
   }

   /* Parse the information */
   nError = static_PPicoParseConfig2(pPicoConnection, pBuffer, P_PICO_BLOCK_SIZE);

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PPicoReadConfigCompleted2 = static_PPicoParseConfig %s", PUtilTraceError(nError));
      goto send_result;
   }


send_result:
   switch(nError)
   {
      case W_SUCCESS:
         break;

      case W_ERROR_RF_COMMUNICATION:
      case W_ERROR_RF_PROTOCOL_NOT_SUPPORTED:
      case W_ERROR_TIMEOUT:
         nError = W_ERROR_RF_COMMUNICATION;
         break;

      default:
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         break;
   }
   static_PPicoSendResultInternal(pContext, pPicoConnection, nError);
}


/* See Header file */
W_ERROR PPicoCheckType5(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumSpaceSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable )
{
   tPicoConnection* pPicoConnection;
   W_ERROR nError;

   PDebugTrace("PPicoCheckType5");

   /* Reset the maximum tag size */
   *pnMaximumSpaceSize = 0;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_PICO_CONNECTION, (void**)&pPicoConnection);
   if ( nError == W_SUCCESS )
   {
      /* Check if the multi-application mode is not configured */
      if ( pPicoConnection->bIsMultiApp != W_FALSE )
      {
         PDebugError("PPicoCheckType5: multi-application mode configured");
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }
      else
      {
         PDebugTrace("PPicoCheckType5: multi-application mode not configured");
      }

      /* Check the security level */
      if ( pPicoConnection->bIsSecured != W_FALSE )
      {
         PDebugError("PPicoCheckType5: secured");
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }
      else
      {
         PDebugTrace("PPicoCheckType5: not secured");
      }

      /* Get the read only mode */
      if ( pPicoConnection->bIsWritable != W_FALSE )
      {
         PDebugTrace("PPicoCheckType5: card not locked");
         *pbIsLocked = W_FALSE;
         *pbIsLockable = W_TRUE;
         * pbIsFormattable = W_TRUE;
      }
      else
      {
         PDebugTrace("PPicoCheckType5: card locked");
         *pbIsLocked = W_TRUE;
         *pbIsLockable = W_FALSE;
      }

      /* Check the card type */
      switch (pPicoConnection->nType)
      {
         case W_PROP_PICOPASS_32K:
            PDebugTrace("PPicoCheckType5: 32K");
            *pnMaximumSpaceSize = (pPicoConnection->nSectorNumber - 1 /* CC */ - (2 * P_PICO_BLOCK_DATA)) * P_PICO_BLOCK_SIZE;
            break;
         case W_PROP_PICOPASS_2K:
            PDebugTrace("PPicoCheckType5: 2K");
            *pnMaximumSpaceSize = (pPicoConnection->nSectorNumber - 1 /* CC */  - P_PICO_BLOCK_DATA) * P_PICO_BLOCK_SIZE;
            break;
         default:
            PDebugError("PPicoCheckType5: wrong card");
            return W_ERROR_CONNECTION_COMPATIBILITY;
      }

      return W_SUCCESS;
   }
   else
   {
      PDebugError("PPicoCheckType5: could not get pPicoConnection buffer");
      return nError;
   }
}

static uint32_t static_PPicoGetIdentifierLength(
         tContext* pContext,
         void* pObject)
{
   return P_PICO_SERIAL_NUMBER_LENGTH;
}

/* Get identifier */
static void static_PPicoGetIdentifier(
         tContext* pContext,
         void* pObject,
         uint8_t* pIdentifierBuffer)
{
   tPicoConnection* pPicoConnection = (tPicoConnection*)pObject;

   CMemoryCopy(pIdentifierBuffer, pPicoConnection->aUID, P_PICO_SERIAL_NUMBER_LENGTH);
}

/* See Client API Specifications */
W_ERROR PPicoGetConnectionInfo(
            tContext* pContext,
            W_HANDLE hConnection,
            tWPicoConnectionInfo *pConnectionInfo )
{
   tPicoConnection* pPicoConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_PICO_CONNECTION, (void**)&pPicoConnection);

   PDebugTrace("PPicoGetConnectionInfo");

   /* Check the parameters */
   if ( pConnectionInfo == null )
   {
      PDebugError("PPicoGetConnectionInfo: pConnectionInfo == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ( nError == W_SUCCESS )
   {
      /* UID */
      CMemoryCopy(
         pConnectionInfo->UID,
         pPicoConnection->aUID,
         P_PICO_SERIAL_NUMBER_LENGTH );
      /* Sector size */
      pConnectionInfo->nSectorSize = pPicoConnection->nSectorSize;
      /* Sector number */
      pConnectionInfo->nSectorNumber = pPicoConnection->nSectorNumber;
   }
   else
   {
      PDebugError("PPicoGetConnectionInfo: could not get pPicoConnection buffer");

      /* Fill in the structure with zeros */
      CMemoryFill(pConnectionInfo, 0, sizeof(tWPicoConnectionInfo));
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR PPicoIsWritable(
            tContext* pContext,
            W_HANDLE hConnection )
{
   tPicoConnection* pPicoConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_PICO_CONNECTION, (void**)&pPicoConnection);

   PDebugTrace("PPicoIsWritable");

   if ( nError == W_SUCCESS )
   {
      if ( pPicoConnection->bIsWritable != W_FALSE )
      {
         return W_SUCCESS;
      }
      else
      {
         return W_ERROR_ITEM_LOCKED;
      }
   }
   else
   {
      PDebugError("PPicoIsWritable: could not get pPicoConnection buffer");
      return nError;
   }
}

/* Returns W_TRUE if blocks are on several books */
static bool_t static_PPicoAreBlocksOnSeveralBooks(uint32_t nType, uint32_t nBlockBegin, uint32_t nBlockEnd)
{
   return ((nType == W_PROP_PICOPASS_32K) && (nBlockBegin < P_PICO_BLOCK_NUMBER_16K) && (nBlockEnd >= P_PICO_BLOCK_NUMBER_16K));
}

/* Computes the fisrt and last blocks' number of data */
static void static_PPicoComputeBlockBeginEnd(tPicoConnection* pPicoConnection, uint32_t nOffset, uint32_t nLength, uint32_t* pnBlockBegin, uint32_t* pnBlockEnd)
{
   *pnBlockBegin = nOffset / pPicoConnection->nSectorSize;
   *pnBlockEnd   = (nOffset + nLength - 1) / pPicoConnection->nSectorSize;

   /* specific case for PICOPASS 32K, if the operation overlaps the two books, we skip the non-data blocks of the second book */
   if ((pPicoConnection->nType == W_PROP_PICOPASS_32K) && (*pnBlockBegin < P_PICO_BLOCK_NUMBER_16K) && (*pnBlockEnd >= P_PICO_BLOCK_NUMBER_16K))
   {
      *pnBlockEnd += P_PICO_BLOCK_DATA;
   }
}

/* Returns W_TRUE if data is on several books */
static void static_PPicoSplitDataOnBooks(tPicoConnection* pPicoConnection, const uint8_t *pBuffer, uint32_t nOffset, uint32_t* pnLength)
{
   uint32_t nBlockBegin, nBlockEnd;

   static_PPicoComputeBlockBeginEnd(pPicoConnection, nOffset, *pnLength, &nBlockBegin, &nBlockEnd);

   /* Specific case for PICOPASS 32, when the operation overlaps the two books, split the operation in two steps */
   if (static_PPicoAreBlocksOnSeveralBooks(pPicoConnection->nType, nBlockBegin, nBlockEnd) != W_FALSE)
   {
      /* Save the whole length */
      pPicoConnection->nLength = (uint16_t) *pnLength;
      /* Compute the length for the first operation */
      *pnLength = P_PICO_BLOCK_NUMBER_16K * pPicoConnection->nSectorSize - nOffset;
      /* prepare the next operation */
      pPicoConnection->nOffset = (uint16_t) ((P_PICO_BLOCK_NUMBER_16K + P_PICO_BLOCK_DATA) * pPicoConnection->nSectorSize);
      pPicoConnection->nLength -= (uint16_t) *pnLength;
      pPicoConnection->pBuffer = (uint8_t*)pBuffer + *pnLength;
   }
   else
   {
      /* There is not any next operation */
      pPicoConnection->nOffset = 0;
      pPicoConnection->nLength = 0;
      pPicoConnection->pBuffer = null;
   }
}

void static_PPicoReadInternalEx(
            tContext* pContext,
            tPicoConnection* pPicoConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength)
{
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t nBlockBegin, nBlockEnd;

   PDebugTrace("static_PPicoReadInternalEx");

   /* Build callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check connection object */
   if (pPicoConnection == null)
   {
      PDebugError("static_PPicoReadInternalEx: null handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check the protocol type */
   if ( pPicoConnection->nType == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_PPicoReadInternalEx: Protocol stil unknown");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) || (nLength == 0))
   {
      PDebugError("static_PPicoReadInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   static_PPicoComputeBlockBeginEnd(pPicoConnection, nOffset, nLength, &nBlockBegin, &nBlockEnd);

   if ((nBlockBegin >= pPicoConnection->nSectorNumber) || (nBlockEnd >= pPicoConnection->nSectorNumber))
   {
      PDebugError("static_PPicoReadInternalEx: the data to read is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Prepare data */
   static_PPicoSplitDataOnBooks(pPicoConnection, pBuffer, nOffset, &nLength);

   /* Store the callback context */
   pPicoConnection->sCallbackContextInternal = sCallbackContext;

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PPicoSendResultInternal when the operation is completed */
   PHandleIncrementReferenceCount(pPicoConnection);

   /* Read */
   PSmartCacheRead(pContext, &pPicoConnection->sSmartCache, nOffset, nLength, pBuffer, static_PPicoReadInternalCompleted, pPicoConnection);

   return;

return_error:
   PDebugError("static_PPicoReadInternalEx: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError );
}

/* See header */
void PPicoReadInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            W_HANDLE *phOperation )
{
   tPicoConnection* pPicoConnection = null;
   W_ERROR nError;

   PDebugTrace("PPicoReadInternal [%d-%d]", nOffset, nOffset + nLength - 1);

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_PICO_CONNECTION, (void**)&pPicoConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PPicoReadInternal: wrong connection handle");
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, static_PPicoCancelOperation, null)) == W_NULL_HANDLE)
      {
         PDebugError("PPicoReadInternal: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &pPicoConnection->hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PPicoReadInternal: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Read */
   static_PPicoReadInternalEx(
         pContext,
         pPicoConnection,
         pCallback,
         pCallbackParameter,
         pBuffer,
         nOffset,
         nLength);

   return;

return_error:
   PDebugError("static_PPicoReadInternalEx: return %s", PUtilTraceError(nError));
   /* Send error */
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, nError);
   }

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
void PPicoRead(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            W_HANDLE *phOperation )
{
   tPicoConnection* pPicoConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t nBlockBegin, nBlockEnd;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;

   PDebugTrace("PPicoRead [%d-%d]", nOffset, nOffset + nLength - 1);

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_PICO_CONNECTION, (void**)&pPicoConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PPicoRead: wrong connection handle");
      goto return_error;
   }

   /* Check the protocol type */
   if ( pPicoConnection->nType == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("PPicoRead: Protocol stil unknown");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) || (nLength == 0))
   {
      PDebugError("PPicoRead: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   static_PPicoComputeBlockBeginEnd(pPicoConnection, nOffset, nLength, &nBlockBegin, &nBlockEnd);

   if ((nBlockBegin >= pPicoConnection->nSectorNumber) || (nBlockEnd >= pPicoConnection->nSectorNumber))
   {
      PDebugError("PPicoRead: the data to read is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, static_PPicoCancelOperation, null)) == W_NULL_HANDLE)
      {
         PDebugError("PPicoRead: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PPicoRead: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PPicoExecuteQueuedExchange, pPicoConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_PPicoSendResultInternal when the operation is completed */
      PHandleIncrementReferenceCount(pPicoConnection);

      /* Store the operation handle */
      CDebugAssert(pPicoConnection->hCurrentOperation == W_NULL_HANDLE);
      pPicoConnection->hCurrentOperation = hCurrentOperation;

      /* Store the callback context */
      pPicoConnection->sCallbackContext = sCallbackContext;

      /* Read */
      static_PPicoReadInternalEx(
            pContext,
            pPicoConnection,
            static_PPicoOperationCompleted,
            pPicoConnection,
            pBuffer,
            nOffset,
            nLength);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_PPicoSendResultInternal when the operation is completed */
      PHandleIncrementReferenceCount(pPicoConnection);

      /* Save the operation handle */
      CDebugAssert(pPicoConnection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
      pPicoConnection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

      /* Save callback context */
      pPicoConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pPicoConnection->sQueuedOperation.nType = P_PICO_QUEUED_READ;

      /* Save data */
      pPicoConnection->sQueuedOperation.pBuffer = pBuffer;
      pPicoConnection->sQueuedOperation.nOffset = nOffset;
      pPicoConnection->sQueuedOperation.nLength = nLength;

      return;

   default:
      /* Return this error */
      if(hCurrentOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hCurrentOperation);
      }

      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("PPicoRead: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError );

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/*
 * @brief Completion routine called when smart cache read operation has been completed
 *
 * @param[in] pContext The context
 *
 * @param[in] pCallbackParameter The callback parameter, eg pPicoConnection
 *
 * @param[in] nError The result of the read operation
 */

void static_PPicoReadInternalCompleted(
      tContext * pContext,
      void * pCallbackParameter,
      W_ERROR nError)
{
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoReadInternalCompleted");

   if (nError == W_SUCCESS)
   {
      if (pPicoConnection->pBuffer != null)
      {
         PSmartCacheRead(pContext, &pPicoConnection->sSmartCache, pPicoConnection->nOffset, pPicoConnection->nLength, pPicoConnection->pBuffer, static_PPicoReadInternalCompleted, pPicoConnection);

         /* no more operation pending */
         pPicoConnection->pBuffer = null;
         pPicoConnection->nLength = 0;
         pPicoConnection->nOffset = 0;

         return;
      }
   }

   pPicoConnection->pBuffer = null;
   pPicoConnection->nLength = 0;
   pPicoConnection->nOffset = 0;

   static_PPicoSendResultInternal(pContext, pPicoConnection, nError);
}


static void static_PPicoWriteInternalEx(
            tContext* pContext,
            tPicoConnection* pPicoConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockCard)
{
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t  nBlockBegin, nBlockEnd;

   PDebugTrace("static_PPicoWriteInternalEx");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check connection object */
   if (pPicoConnection == null)
   {
      PDebugError("static_PPicoWriteInternalEx: null handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check the protocol type */
   if ( pPicoConnection->nType == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_PPicoWriteInternalEx: Protocol stil unknown");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check if the Picopass card is locked */
   if ( pPicoConnection->bIsWritable == W_FALSE )
   {
      PDebugError("static_PPicoWriteInternalEx: The tag is write-protected");
      nError = W_ERROR_ITEM_LOCKED;
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockCard == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("static_PPicoWriteInternalEx: Bad parameters");

      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockCard == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("static_PPicoWriteInternalEx: Bad parameters");

      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((pBuffer == null) && (nOffset == 0) && (nLength == 0))
   {
      /* specific case for locking the whole TAG (used by NDEF) */
      nBlockBegin = 0;
      nBlockEnd = pPicoConnection->nSectorNumber;
   }
   else
   {
      if (nLength == 0)
      {
         PDebugError("static_PPicoWriteInternalEx: Bad parameters");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }

      static_PPicoComputeBlockBeginEnd(pPicoConnection, nOffset, nLength, &nBlockBegin, &nBlockEnd);

      if ((nBlockBegin >= pPicoConnection->nSectorNumber) || (nBlockEnd >= pPicoConnection->nSectorNumber))
      {
         PDebugError("static_PPicoWriteInternalEx: the data to write is too large");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }
   }

   /* store the operation parameters */
   pPicoConnection->bLockCard = bLockCard;

   /* Store the callback context */
   pPicoConnection->sCallbackContextInternal = sCallbackContext;

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PPicoSendResultInternal when the operation is completed */
   PHandleIncrementReferenceCount(pPicoConnection);

   /* Check if the action is lock and there is no data to write */
   if ((bLockCard != W_FALSE) && (pBuffer == null))
   {
      /* Lock the card */
      static_PPicoLockCard(pContext, pPicoConnection);
   }
   else
   {
      /* Prepare data */
      static_PPicoSplitDataOnBooks(pPicoConnection, pBuffer, nOffset, &nLength);

      /* Write */
      PSmartCacheWrite(pContext, &pPicoConnection->sSmartCache, nOffset, nLength, pBuffer, static_PPicoWriteInternalCompleted, pPicoConnection);
   }

   return;

return_error:
   PDebugError("static_PPicoWriteInternalEx: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See header */
void PPicoWriteInternal(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockCard,
            W_HANDLE *phOperation )
{
   tPicoConnection* pPicoConnection = null;
   W_ERROR nError;

   PDebugTrace("PPicoWriteInternal [%d-%d]", nOffset, nOffset + nLength - 1);

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_PICO_CONNECTION, (void**)&pPicoConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PPicoWriteInternal: wrong connection handle");
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, static_PPicoCancelOperation, null)) == W_NULL_HANDLE)
      {
         PDebugError("PPicoWriteInternal: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &pPicoConnection->hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PPicoWriteInternal: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Write */
   static_PPicoWriteInternalEx(
         pContext,
         pPicoConnection,
         pCallback,
         pCallbackParameter,
         pBuffer,
         nOffset,
         nLength,
         bLockCard);

   return;

return_error:
   PDebugError("PPicoWriteInternal: return %s", PUtilTraceError(nError));
   /* Send error */
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, nError);
   }

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
void PPicoWrite(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockCard,
            W_HANDLE *phOperation )
{
   tPicoConnection* pPicoConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t  nBlockBegin, nBlockEnd;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;

   PDebugTrace("PPicoWrite [%d-%d]", nOffset, nOffset+nLength-1);

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_PICO_CONNECTION, (void**)&pPicoConnection);
   if (nError != W_SUCCESS)
   {
      PDebugError("PPicoWrite: wrong connection handle");
      goto return_error;
   }

   /* Check the protocol type */
   if ( pPicoConnection->nType == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("PPicoWrite: Protocol stil unknown");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check if the Picopass card is locked */
   if ( pPicoConnection->bIsWritable == W_FALSE )
   {
      PDebugError("PPicoWrite: The tag is write-protected");
      nError = W_ERROR_ITEM_LOCKED;
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockCard == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("PPicoWrite: Bad parameters");

      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockCard == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("PPicoWrite: Bad parameters");

      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((pBuffer == null) && (nOffset == 0) && (nLength == 0))
   {
      /* specific case for locking the whole TAG (used by NDEF) */
      nBlockBegin = 0;
      nBlockEnd = pPicoConnection->nSectorNumber;
   }
   else
   {
      if (nLength == 0)
      {
         PDebugError("PPicoWrite: Bad parameters");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }

      static_PPicoComputeBlockBeginEnd(pPicoConnection, nOffset, nLength, &nBlockBegin, &nBlockEnd);

      if ((nBlockBegin >= pPicoConnection->nSectorNumber) || (nBlockEnd >= pPicoConnection->nSectorNumber))
      {
         PDebugError("PPicoWrite: the data to write is too large");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, static_PPicoCancelOperation, null)) == W_NULL_HANDLE)
      {
         PDebugError("PPicoRead: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PPicoRead: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, pPicoConnection->hConnection, static_PPicoExecuteQueuedExchange, pPicoConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_PPicoSendResultInternal when the operation is completed */
      PHandleIncrementReferenceCount(pPicoConnection);

      /* store the operation handle */
      CDebugAssert(pPicoConnection->hCurrentOperation == W_NULL_HANDLE);
      pPicoConnection->hCurrentOperation = hCurrentOperation;

      /* Store the callback context */
      pPicoConnection->sCallbackContext = sCallbackContext;

      /* Write */
      static_PPicoWriteInternalEx(
            pContext,
            pPicoConnection,
            static_PPicoOperationCompleted,
            pPicoConnection,
            pBuffer,
            nOffset,
            nLength,
            bLockCard);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in static_PPicoSendResultInternal when the operation is completed */
      PHandleIncrementReferenceCount(pPicoConnection);

      /* Save the operation handle */
      CDebugAssert(pPicoConnection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
      pPicoConnection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

      /* Save callback context */
      pPicoConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pPicoConnection->sQueuedOperation.nType = P_PICO_QUEUED_WRITE;
      if (bLockCard != W_FALSE)
      {
         /* Check if the operation is lock or write+lock */
         if (pBuffer == null)
         {
            pPicoConnection->sQueuedOperation.nType = P_PICO_QUEUED_LOCK;
         }
         else
         {
            pPicoConnection->sQueuedOperation.nType = P_PICO_QUEUED_WRITE_LOCK;
         }
      }

      /* Save data */
      pPicoConnection->sQueuedOperation.pBuffer = (uint8_t*)pBuffer;
      pPicoConnection->sQueuedOperation.nOffset = nOffset;
      pPicoConnection->sQueuedOperation.nLength = nLength;

      return;

   default:
      /* Return this error */
      if(hCurrentOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hCurrentOperation);
      }

      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:

   PDebugError("PPicoWrite: return %s", PUtilTraceError(nError));

   PDFCPostContext2(
      &sCallbackContext,
      nError );

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/*
 * @brief Completion routine called when smart cache write operation has been completed
 *
 * @param[in] pContext The context
 *
 * @param[in] pCallbackParameter The callback parameter, eg pPicoConnection
 *
 * @param[in] nError The result of the write operation
 */

void static_PPicoWriteInternalCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError)
{
   tPicoConnection * pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoWriteInternalCompleted");

   if (nError == W_SUCCESS) {

      /* perform the pending operation if any */
      if (pPicoConnection->pBuffer != null)
      {
         PSmartCacheWrite(pContext, &pPicoConnection->sSmartCache, pPicoConnection->nOffset, pPicoConnection->nLength, pPicoConnection->pBuffer, static_PPicoWriteInternalCompleted, pPicoConnection);

         /* no more operation pending */
         pPicoConnection->pBuffer = null;
         pPicoConnection->nLength = 0;
         pPicoConnection->nOffset = 0;

         return;
      }

      /* otherwize, lock the card if needed */
      if (pPicoConnection->bLockCard != W_FALSE)
      {
         static_PPicoLockCard(pContext, pPicoConnection);

         return;
      }
   }

   static_PPicoSendResultInternal(pContext, pPicoConnection, nError);
}

/* See Client API Specifications */
W_ERROR WPicoReadSync(
            W_HANDLE hConnection,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WPicoRead(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pBuffer, nOffset, nLength,
            null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR WPicoWriteSync(
            W_HANDLE hConnection,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockCard )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WPicoWrite(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pBuffer, nOffset, nLength, bLockCard,
            null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See header file */
W_ERROR PPicoInvalidateCache(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nOffset,
            uint32_t nLength)
{
   tPicoConnection* pPicoConnection = null;
   W_ERROR nError;

   PDebugTrace("PPicoInvalidateCache");

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_PICO_CONNECTION, (void**)&pPicoConnection);
   if ( nError != W_SUCCESS )
   {
      PDebugError("PPicoInvalidateCache: Bad handle");
      return nError;
   }

   PSmartCacheInvalidateCache(pContext, &pPicoConnection->sSmartCache, nOffset, nLength);
   return W_SUCCESS;
}

/* Polling command's callback */
static void static_PPicoPollCompleted(
      tContext * pContext,
      void * pCallbackParameter,
      uint32_t nLength,
      W_ERROR nError)
{
   tPicoConnection* pPicoConnection = (tPicoConnection *) pCallbackParameter;

   PDebugTrace("static_PPicoPollCompleted");

   /* Send the error */
   PDFCPostContext2(&pPicoConnection->sCallbackContext, nError);

   /* Release the reference after completion. May destroy the object */
   PHandleDecrementReferenceCount(pContext, pPicoConnection);
}

/* Send polling command */
static void static_PPicoPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tPicoConnection* pPicoConnection = (tPicoConnection*) pObject;

   PDebugTrace("static_PPicoPoll");

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PPicoPollCompleted callback  */
   PHandleIncrementReferenceCount(pPicoConnection);

   /* store the callback context */
   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &pPicoConnection->sCallbackContext);

   /* Send the command */
   pPicoConnection->aReaderToCardBuffer[0] = 0x0C;
   pPicoConnection->aReaderToCardBuffer[1] = 0x00;
   static_PPicoExchangeData(pContext, pPicoConnection, 2, static_PPicoPollCompleted);
}

/* Execute the queued operation (after polling) */
static void static_PPicoExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tPicoConnection* pPicoConnection = (tPicoConnection*) pObject;

   PDebugTrace("static_PPicoExecuteQueuedExchange");

   /* Restore operation handle */
   pPicoConnection->hCurrentOperation = pPicoConnection->sQueuedOperation.hCurrentOperation;
   /* Restore callback context */
   pPicoConnection->sCallbackContext = pPicoConnection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (pPicoConnection->hCurrentOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pPicoConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PPicoExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_PPicoOperationCompleted(pContext, pPicoConnection, nResult);
   }
   else
   {
      switch (pPicoConnection->sQueuedOperation.nType)
      {
      case P_PICO_QUEUED_READ:
         /* Read */
         static_PPicoReadInternalEx(
               pContext,
               pPicoConnection,
               static_PPicoOperationCompleted,
               pPicoConnection,
               pPicoConnection->sQueuedOperation.pBuffer,
               pPicoConnection->sQueuedOperation.nOffset,
               pPicoConnection->sQueuedOperation.nLength);

         break;

      case P_PICO_QUEUED_LOCK:
         /* Lock the card */
         static_PPicoLockCard(pContext, pPicoConnection);

         break;

      case P_PICO_QUEUED_WRITE:
      case P_PICO_QUEUED_WRITE_LOCK:
         /* Write */
         static_PPicoWriteInternalEx(
               pContext,
               pPicoConnection,
               static_PPicoOperationCompleted,
               pPicoConnection,
               pPicoConnection->sQueuedOperation.pBuffer,
               pPicoConnection->sQueuedOperation.nOffset,
               pPicoConnection->sQueuedOperation.nLength,
               pPicoConnection->sQueuedOperation.nType == P_PICO_QUEUED_WRITE_LOCK);

         break;

      default:
         /* Return an error */
         PDebugError("static_PPicoExecuteQueuedExchange: unknown type of operation!");
         static_PPicoOperationCompleted(pContext, pPicoConnection, W_ERROR_BAD_STATE);
      }
   }

   /* Reset data */
   CMemoryFill(&pPicoConnection->sQueuedOperation, 0, sizeof(pPicoConnection->sQueuedOperation));
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* ifndef P_INCLUDE_PICOPASS */

