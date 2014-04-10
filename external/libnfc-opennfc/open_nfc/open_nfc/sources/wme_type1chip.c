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
   Contains the implementation of the type1chip functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( TYPE1_CHIP )

#include "wme_context.h"

/* General constants */
#define P_TYPE1_CHIP_DETECTION_MESSAGE_SIZE        8
#define P_TYPE1_CHIP_SMALL_UID_LENGTH              4
#define P_TYPE1_CHIP_UID_LENGTH                    7

#define P_TYPE1_CHIP_BLOCK_SIZE                    0x08

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_TYPE1_CHIP_MAX_NB_BLOCK                  256

/* Queued operation type */
#define P_TYPE1_QUEUED_NONE                   0
#define P_TYPE1_QUEUED_READ                   1
#define P_TYPE1_QUEUED_WRITE                  2


/* Declare a Type1Chip connection */
typedef struct __tPType1ChipUserConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;
   /* Connection handle */
   W_HANDLE                   hUserConnection;
   W_HANDLE                   hDriverConnection;

   uint16_t                   nType1ChipVariant;         /**< The type1 chip variant (IRT5001, .... IRT5006, STATIC TAG.... */
   uint8_t                    nCardSecondaryConnection;

   /* Connection information */
   uint8_t                    aUID[P_TYPE1_CHIP_UID_LENGTH];
   uint8_t                    aHeaderRom[2];


   uint32_t                   nLowMemSize;               /**< The size of the low memory in bytes */
   uint32_t                   nHighMemSize;               /**< The size of the low memory in bytes */


   uint8_t                    aReaderToCardBuffer[NAL_MESSAGE_MAX_LENGTH];
   /* Response buffer */
   uint8_t                    aCardToReaderBuffer[NAL_MESSAGE_MAX_LENGTH];


   uint8_t                    nCurrentReadOperation;     /**< The read operation in progress */
   uint8_t                  * pCurrentReadBuffer;        /**< The user buffer for the read operation */

   uint8_t                    nCurrentWriteOperation;    /**< The current write operation */
   const uint8_t            * pCurrentWriteBuffer;       /**< The user buffer for the write operation */

   tSmartCache                sSmartCache;               /**< The cache used for low memory */
   tSmartCache                sSmartCacheHighMem;        /**< The cache used for high memory */

   const uint8_t            * pLowMemCache;              /**< pointer to the first byte of the low memory cache (read only) */
   const uint8_t            * pHighMemCache;             /**< pointer to the first byte of the high memory cache (read only) */

   tDFCCallbackContext        sCacheCallbackContext;     /**< Callback context used by smart cache operations */

   struct
   {
      uint16_t                nLockId;
      uint16_t                nLockBit;

   } asBlockLock[P_TYPE1_CHIP_MAX_NB_BLOCK];

#  define P_TYPE1_CHIP_NB_MEM_BLOCKS                     2

   struct
   {
      uint16_t                nOffset;
      uint16_t                nLength;
      uint8_t                 aBytes[8];

   } asMemLock[P_TYPE1_CHIP_NB_MEM_BLOCKS];               /**< Copy of locks */


#     define P_TYPE1_LOCK_ID_NONE                        255
#     define P_TYPE1_LOCK_ID_LOW_MEM                     0
#     define P_TYPE1_LOCK_ID_HIGH_MEM                    1


   /** Write operation descriptor */

   struct
   {
      uint8_t       * pBuffer;
      uint16_t        nLowMemOffset;
      uint16_t        nLowMemLength;
      uint16_t        nHighMemOffset;
      uint16_t        nHighMemLength;
      bool_t            bLockBlocks;

   } sReadWriteOperation;


   /* handle on the current operation */
   W_HANDLE                   hOperation;
   W_HANDLE                   hDriverOperation;

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
      bool_t                 bLockBlocks;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hOperation;
   } sQueuedOperation;

} tPType1ChipUserConnection;


static void static_PType1ChipAtomicRead(tContext * pContext, void * pConnection, uint32_t nSectorOffset, uint32_t nSectorNumber,
                     uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_PType1ChipAtomicReadHighMem(tContext * pContext, void * pConnection, uint32_t nSectorOffset, uint32_t nSectorNumber,
                     uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_PType1ChipAtomicReadCompleted(tContext * pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nError);

static void static_PType1ChipAtomicWrite(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     const uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_PType1ChipAtomicWriteHighMem(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     const uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_PType1ChipAtomicWriteCompleted(tContext * pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nError);

static void static_PType1StaticTagAtomicRead(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_PType1StaticTagAtomicReadCompleted(tContext * pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nError);

static void static_PType1StaticTagAtomicWrite(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     const uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_PType1StaticTagAtomicWriteCompleted(tContext * pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nError);

static void static_PType1DynamicTagAtomicReadHighMem(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_PType1DynamicTagAtomicRead(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

static void static_PType1DynamicTagAtomicReadCompleted(tContext * pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nError);

static void static_PType1DynamicTagAtomicWrite(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     const uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback,  void * pCallbackParameter);

static void static_PType1DynamicTagAtomicWriteHighMem(tContext * pContext, void * pConnection,  uint32_t nSectorOffset, uint32_t nSectorNumber,
                     const uint8_t * pBuffer, tPBasicGenericCallbackFunction * pCallback,  void * pCallbackParameter);

static void static_PType1DynamicTagAtomicWriteCompleted(tContext * pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nError);

static void static_PType1ChipReadLowMemCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError);

static void static_PType1ChipReadHighMemCompleted(tContext * pContext,void * pCallbackParameter, W_ERROR nError);

static W_ERROR static_PType1ChipCheckContainerCapability(tContext * pContext, tPType1ChipUserConnection * pType1ChipUserConnection);

static W_ERROR static_PType1ChipGetMemoryAndLockControlTLV(tContext * pContext, tPType1ChipUserConnection * pType1ChipUserConnection);

static void static_PType1ChipWriteLowMemCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError);

static void static_PType1ChipWriteHighMemCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError);

static void static_PType1ChipMemLockBlockCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError);

static void static_PType1ReadOrWriteCancel(tContext* pContext, void* pCancelParameter, bool_t bIsClosing);


static void static_PType1ChipUserCreationCompleted(tContext * pContext, tPType1ChipUserConnection* pType1ChipUserConnection, W_ERROR nError);

static void static_PType1ChipUserCommandCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError);
static void static_PType1ChipInternalOperationCompleted(tContext * pContext, void* pCallbackParameter, W_ERROR nError);

#define P_TYPE1_CHIP_IRT5001                               0x01        /* also known as Jewel */
#define P_TYPE1_CHIP_IRT5001_HR0                           0x01
#define P_TYPE1_CHIP_IRT5001_HR1                           0x3C
#define P_TYPE1_CHIP_IRT5001_BLOCK_NB                      15
#define P_TYPE1_CHIP_IRT5001_HIGHMEM_BLOCK_NB              0

#define P_TYPE1_CHIP_IRT5002                               0x04
#define P_TYPE1_CHIP_IRT5002_HR0                           0x04
#define P_TYPE1_CHIP_IRT5002_BLOCK_NB                      15
#define P_TYPE1_CHIP_IRT5002_HIGHMEM_BLOCK_NB              0

#define P_TYPE1_CHIP_IRT5003                               0x05
#define P_TYPE1_CHIP_IRT5003_HR0                           0x05
#define P_TYPE1_CHIP_IRT5003_BLOCK_NB                      15
#define P_TYPE1_CHIP_IRT5003_HIGHMEM_BLOCK_NB              12

#define P_TYPE1_CHIP_IRT5004                               0x06
#define P_TYPE1_CHIP_IRT5004_HR0                           0x06
#define P_TYPE1_CHIP_IRT5004_BLOCK_NB                      15
#define P_TYPE1_CHIP_IRT5004_HIGHMEM_BLOCK_NB              36

#define P_TYPE1_CHIP_IRT5005                               0x07
#define P_TYPE1_CHIP_IRT5005_HR0                           0x07
#define P_TYPE1_CHIP_IRT5005_BLOCK_NB                      15
#define P_TYPE1_CHIP_IRT5005_HIGHMEM_BLOCK_NB              (7 * 16)

#define P_TYPE1_CHIP_IRT5006                               0x08
#define P_TYPE1_CHIP_IRT5006_HR0                           0x08
#define P_TYPE1_CHIP_IRT5006_BLOCK_NB                      15
#define P_TYPE1_CHIP_IRT5006_HIGHMEM_BLOCK_NB              (15 * 16)

#define P_TYPE1_LOCK_0_OFFSET                            112
#define P_TYPE1_LOCK_2_OFFSET                            120
#define P_TYPE1_LOCK_8_OFFSET                            126

#define P_TYPE1_TAG_STATIC                                 0x11        /* also known as Topaz */
#define P_TYPE1_TAG_STATIC_HR0                             0x11
#define P_TYPE1_TAG_STATIC_BLOCK_NB                        15
#define P_TYPE1_TAG_STATIC_HIGHMEM_BLOCK_NB                0

#define P_TYPE1_TAG_DYNAMIC                                0x1F
#define P_TYPE1_TAG_DYNAMIC_BLOCK_NB                       16

#define P_TYPE1_TAG_TOPAZ_512_HR0                          0x12        /* also known as Topaz 512 */
#define P_TYPE1_TAG_TOPAZ_512_HIGHMEM_BLOCK_NB             48          /* 48 * 8 = 384 bytes */

#define P_TYPE1_CHIP_NDEF_TAG_CC_OFFSET                    8
#define P_TYPE1_CHIP_NDEF_TAG_CC_MNM_OFFSET                (P_TYPE1_CHIP_NDEF_TAG_CC_OFFSET+0)
#define P_TYPE1_CHIP_NDEF_TAG_CC_VNO_OFFSET                (P_TYPE1_CHIP_NDEF_TAG_CC_OFFSET+1)
#define P_TYPE1_CHIP_NDEF_TAG_CC_TMS_OFFSET                (P_TYPE1_CHIP_NDEF_TAG_CC_OFFSET+2)
#define P_TYPE1_CHIP_NDEF_TAG_CC_RWA_OFFSET                (P_TYPE1_CHIP_NDEF_TAG_CC_OFFSET+3)

#define P_TYPE1_CHIP_NDEF_TAG_TLV_START_OFFSET             12
#define P_TYPE1_CHIP_NDEF_NULL_TLV                         0x00
#define P_TYPE1_CHIP_NDEF_LOCK_CONTROL_TLV_TYPE            0x01
#define P_TYPE1_CHIP_NDEF_LOCK_CONTROL_TLV_LENGTH          3
#define P_TYPE1_CHIP_NDEF_RESERVED_MEMORY_TLV_TYPE         0x02
#define P_TYPE1_CHIP_NDEF_LOCK_CONTROL_TLV_LENGTH          3
#define P_TYPE1_CHIP_NDEF_MESSAGE_TLV_TYPE                 0x03
#define P_TYPE1_CHIP_NDEF_PROPRIETARY_TLV_TYPE             0xFD
#define P_TYPE1_CHIP_NDEF_TERMINATOR_TLV_TYPE              0xFE

#define P_TYPE1_CHIP_COMMAND_RALL                          0x00
#define P_TYPE1_CHIP_COMMAND_RALL_ANSWER_LENGTH            (124-2)

#define P_TYPE1_CHIP_COMMAND_WRITE_E                       0x53
#define P_TYPE1_CHIP_COMMAND_WRITE_NE                      0x1A
#define P_TYPE1_CHIP_COMMAND_WRITE_ANSWER_LENGTH           (4-2)

#define P_TYPE1_CHIP_COMMAND_READ4                         0x02
#define P_TYPE1_CHIP_COMMAND_WRITE4_E                      0x54
#define P_TYPE1_CHIP_COMMAND_WRITE4_NE                     0x1B
#define P_TYPE1_CHIP_COMMAND_WRITE4_ANSWER_LENGTH          (7-2)

#define P_TYPE1_CHIP_COMMAND_READ8                         0x03
#define P_TYPE1_CHIP_COMMAND_READ8_ANSWER_LENGTH           (11-2)

#define P_TYPE1_CHIP_COMMAND_WRITE8_E                      0x55
#define P_TYPE1_CHIP_COMMAND_WRITE8_NE                     0x1C
#define P_TYPE1_CHIP_COMMAND_WRITE8_ANSWER_LENGTH          (11-2)

#define P_TYPE1_TAG_COMMAND_RALL                           0x00
#define P_TYPE1_TAG_COMMAND_RALL_ANSWER_LENGTH             (124-2)

#define P_TYPE1_TAG_COMMAND_WRITE_E                        0x53
#define P_TYPE1_TAG_COMMAND_WRITE_NE                       0x1A
#define P_TYPE1_TAG_COMMAND_WRITE_ANSWER_LENGTH            (4-2)

#define P_TYPE1_TAG_COMMAND_READ8                          0x02
#define P_TYPE1_TAG_COMMAND_READ8_ANSWER_LENGTH            (11-2)

#define P_TYPE1_TAG_COMMAND_WRITE8_E                       0x54
#define P_TYPE1_TAG_COMMAND_WRITE8_NE                      0x1B
#define P_TYPE1_TAG_COMMAND_WRITE8_ANSWER_LENGTH           (11-2)

#define P_TYPE1_TAG_COMMAND_READSEG                        0x10
#define P_TYPE1_TAG_COMMAND_READSEG_ANSWER_LENGTH          (131-2)

/*cache Connection defines*/

#define P_TYPE1_IDENTIFIER_LEVEL             0
#define P_TYPE1_IDENTIFIER_LEVEL_HIGH_MEM    1

extern tSmartCacheSectorSize g_sSectorSize1;

   /* For IRT5001, 5005 and 5006 low memory */

static tSmartCacheDescriptor g_sLowMemCacheDescriptor = {

   P_TYPE1_IDENTIFIER_LEVEL, &g_sSectorSize1,
   {
      { 120, 120, static_PType1ChipAtomicRead },   /* e.g RALL operation */
      { 0, 0, null }
   },
   {
      { 1, 1, static_PType1ChipAtomicWrite },      /* e.g WRITE-E and WRITE-NE operartions */
      { 0, 0, null }
   },
};

   /* For IRT5002, IRT5003 and IRT5004 low memory */

static tSmartCacheDescriptor g_sLowMem4CacheDescriptor = {

   P_TYPE1_IDENTIFIER_LEVEL, &g_sSectorSize1,
   {
      { 120, 120, static_PType1ChipAtomicRead },   /* e.g RALL operation */
      { 0, 0, null }
   },
   {
      { 1, 1, static_PType1ChipAtomicWrite },      /* e.g WRITE-E and WRITE-NE operartions */
      { 4, 4, static_PType1ChipAtomicWrite },      /* e.g WRITE4-E and WRITE4-NE operartions */
   },
};


   /* For IRT 5003-5004 chip high memory */

static tSmartCacheDescriptor g_sHighMem4CacheDescriptor = {

   P_TYPE1_IDENTIFIER_LEVEL_HIGH_MEM, &g_sSectorSize1,

   {
      { 4, 4, static_PType1ChipAtomicReadHighMem },      /* e.g READ4 operation */
      { 0, 0, null }
   },

   {
      { 4, 4, static_PType1ChipAtomicWriteHighMem },     /* e.g WRITE4-E and WRITE4-NE operartions */
      { 0, 0, null }
   },
};

   /* For IRT 5005-5006 chip high memory */

static tSmartCacheDescriptor g_sHighMem8CacheDescriptor = {

   P_TYPE1_IDENTIFIER_LEVEL_HIGH_MEM, &g_sSectorSize1,
   {
      { 8, 8, static_PType1ChipAtomicRead },       /* e.g READ8 operation */
      { 0, 0, null }
   },
   {
      { 8, 8, static_PType1ChipAtomicWrite },      /* e.g WRITE8-E and WRITE8-NE operartions */
      { 0, 0, null }
   },
};

   /* For Type1 Static TAG */

static tSmartCacheDescriptor g_sStaticTagCacheDescriptor = {

   P_TYPE1_IDENTIFIER_LEVEL, &g_sSectorSize1,
   {
      { 120, 120, static_PType1StaticTagAtomicRead },       /* e.g READ8 operation */
      { 0, 0, null }
   },
   {
      { 1, 1, static_PType1StaticTagAtomicWrite },      /* e.g WRITE-E and WRITE-NE operartions */
      { 0, 0, null }
   },
};

   /* For Type1 Dynamic TAG high memory */

static tSmartCacheDescriptor g_sDynamicTagCacheDescriptor = {

   P_TYPE1_IDENTIFIER_LEVEL, &g_sSectorSize1,
   {
      { 8, 8, static_PType1DynamicTagAtomicRead },
      { 128, 128, static_PType1DynamicTagAtomicRead },       /* e.g READ SEG operation */
   },
   {
      { 8, 8, static_PType1DynamicTagAtomicWrite },      /* e.g WRITE-E and WRITE-NE operartions */
      { 0, 0, null }
   },
};

static tSmartCacheDescriptor g_sDynamicTagCacheDescriptorHighMem = {

   P_TYPE1_IDENTIFIER_LEVEL_HIGH_MEM, &g_sSectorSize1,
   {
      { 8, 8, static_PType1DynamicTagAtomicReadHighMem },
      { 128, 128, static_PType1DynamicTagAtomicReadHighMem },       /* e.g READ SEG operation */
   },
   {
      { 8, 8, static_PType1DynamicTagAtomicWriteHighMem },      /* e.g WRITE-E and WRITE-NE operartions */
      { 0, 0, null }
   },
};


/* Destroy connection callback */
static uint32_t static_PType1ChipUserDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static uint32_t static_PType1ChipUserGetPropertyNumber(
            tContext* pContext,
            void* pObject);

/* Get properties connection callback */
static bool_t static_PType1ChipUserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_PType1ChipUserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* Get identifier length */
static uint32_t static_PType1ChipGetIdentifierLength(
            tContext* pContext,
            void* pObject);

/* Get identifier */
static void static_PType1ChipGetIdentifier(
            tContext* pContext,
            void* pObject,
            uint8_t* pIdentifierBuffer);

/* Exchange data */
static void static_PType1ChipExchangeData(
            tContext * pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction * pCallback,
            void * pCallbackParameter,
            const uint8_t * pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t * pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE * phOperation);

/* Send polling command */
static void static_PType1ChipPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

/* Execute the queued operation (after polling) */
static void static_PType1ChipExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult);

/* Handle registry Type 1 Chip connection type */
tHandleType g_sType1ChipUserConnection = {   static_PType1ChipUserDestroyConnection,
                                             null,
                                             static_PType1ChipUserGetPropertyNumber,
                                             static_PType1ChipUserGetProperties,
                                             static_PType1ChipUserCheckProperties,
                                             static_PType1ChipGetIdentifierLength,
                                             static_PType1ChipGetIdentifier,
                                             static_PType1ChipExchangeData,
                                             static_PType1ChipPoll };

#define P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION (&g_sType1ChipUserConnection)

/**
 * @brief   Destroyes a Type1Chip connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PType1ChipUserDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pObject;

   PDebugTrace("static_PType1ChipUserDestroyConnection");

   /* Destroy the smart cache */
   PSmartCacheDestroyCache(pContext, &pType1ChipUserConnection->sSmartCache);

   /* Destroy the smart cache */
   PSmartCacheDestroyCache(pContext, &pType1ChipUserConnection->sSmartCacheHighMem);

   PDFCFlushCall(&pType1ChipUserConnection->sCacheCallbackContext);
   PDFCFlushCall(&pType1ChipUserConnection->sCallbackContext);

   /* Free the Type1Chip connection structure */
   CMemoryFree( pType1ChipUserConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the Type1Chip connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 **/
static uint32_t static_PType1ChipUserGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pObject;

   PDebugTrace("static_PType1ChipUserGetPropertyNumber");

   return (pType1ChipUserConnection->nCardSecondaryConnection != 0) ? 2 : 1;
}

/**
 * @brief   Gets the Type1Chip connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PType1ChipUserGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pObject;

   PDebugTrace("static_PType1ChipUserGetProperties");

   if (pType1ChipUserConnection->nCardSecondaryConnection != 0)
   {
      * pPropertyArray++ = pType1ChipUserConnection->nCardSecondaryConnection;
   }

   * pPropertyArray++ = W_PROP_TYPE1_CHIP;

   return W_TRUE;
}

/**
 * @brief   Checks the Type1Chip connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PType1ChipUserCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pObject;

   PDebugTrace("static_PType1ChipUserCheckProperties: nPropertyValue=%s (0x%02X)", PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue);

   if ( (nPropertyValue == W_PROP_TYPE1_CHIP) ||
        ((pType1ChipUserConnection->nCardSecondaryConnection != 0) &&  (nPropertyValue == pType1ChipUserConnection->nCardSecondaryConnection)) )
   {
      return W_TRUE;
   }
   else
   {
      return W_FALSE;
   }
}


static void static_PType1ChipUserCreationCompleted(
            tContext                  * pContext,
            tPType1ChipUserConnection* pType1ChipUserConnection,
            W_ERROR nError )
{
   PDebugTrace("static_PType1ChipUserCreationCompleted: nError %s", PUtilTraceError(nError));
   if( nError == W_SUCCESS)
   {
      /* NOTHING */
   }
   else if( (nError == W_ERROR_RF_COMMUNICATION) ||
       (nError == W_ERROR_RF_PROTOCOL_NOT_SUPPORTED) ||
       (nError == W_ERROR_TIMEOUT))
   {
      nError = W_ERROR_RF_COMMUNICATION;
   }
   else
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Send the error */
   PDFCPostContext2(&pType1ChipUserConnection->sCallbackContext, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pType1ChipUserConnection);
}


/**
 * @brief   Manage completion of a user command. Cleanup and send result.
 *          See tWBasicGenericCallbackFunction
 **/
static void static_PType1ChipUserCommandCompleted(
            tContext*   pContext,
            void*       pCallbackParameter,
            W_ERROR     nError )
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*) pCallbackParameter;

   PDebugTrace("static_PType1ChipUserCommandCompleted: nError %s", PUtilTraceError(nError));

   if (pType1ChipUserConnection->hOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pType1ChipUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PType1ChipUserCommandCompleted: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pType1ChipUserConnection->hOperation);
      PHandleClose(pContext, pType1ChipUserConnection->hOperation);
      pType1ChipUserConnection->hOperation = W_NULL_HANDLE;
   }

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, pType1ChipUserConnection->hUserConnection);

   /* Send the error */
   PDFCPostContext2(&pType1ChipUserConnection->sCallbackContext, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pType1ChipUserConnection);
}

/* See tWBasicGenericCallbackFunction */
static void static_PType1ChipInternalOperationCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*) pCallbackParameter;

   PDebugTrace("static_PType1ChipInternalOperationCompleted: nError %s", PUtilTraceError(nError));

   /* Send the error */
   PDFCPostContext2(&pType1ChipUserConnection->sCallbackContextInternal, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pType1ChipUserConnection);
}


/**
 * @brief   Locks the blocks specified in the last write operation
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pType1ChipUserConnection  The Type 1 Chip connection.
 **/

static void static_PType1ChipUserLockBlock(
            tContext* pContext,
            tPType1ChipUserConnection* pType1ChipUserConnection )
{
   uint16_t  nIndex, nHighIndex;
   uint16_t  nLockId;
   uint16_t  nLockByte;
   uint16_t  nLockBit;
   bool_t      bPerformLock = W_FALSE;

   PDebugTrace("static_PType1ChipUserLockBlock");

   if ((pType1ChipUserConnection->sReadWriteOperation.nLowMemLength == 0) &&
       (pType1ChipUserConnection->sReadWriteOperation.nHighMemLength == 0))
   {
      /* no block to lock, or lock completed */

      static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, W_SUCCESS);
      return;
   }

   if (pType1ChipUserConnection->sReadWriteOperation.nLowMemLength != 0)
   {
      /* First lock the blocks located in the low memory area */

      PDebugTrace("static_PType1ChipUserLockBlock : locking low mem [%d - %d[",
                     pType1ChipUserConnection->sReadWriteOperation.nLowMemOffset,
                     pType1ChipUserConnection->sReadWriteOperation.nLowMemOffset + pType1ChipUserConnection->sReadWriteOperation.nLowMemLength);

      nIndex  = pType1ChipUserConnection->sReadWriteOperation.nLowMemOffset / 8;
      nHighIndex = (pType1ChipUserConnection->sReadWriteOperation.nLowMemOffset + pType1ChipUserConnection->sReadWriteOperation.nLowMemLength - 1) / 8;

      pType1ChipUserConnection->sReadWriteOperation.nLowMemLength = 0;

      do
      {
         nLockId   = pType1ChipUserConnection->asBlockLock[nIndex].nLockId;
         nLockByte = pType1ChipUserConnection->asBlockLock[nIndex].nLockBit / 8;
         nLockBit  = pType1ChipUserConnection->asBlockLock[nIndex].nLockBit & 7;

         CDebugAssert(nLockId != P_TYPE1_LOCK_ID_HIGH_MEM);

         if (nLockId < P_TYPE1_CHIP_NB_MEM_BLOCKS)
         {
            pType1ChipUserConnection->asMemLock[nLockId].aBytes[nLockByte] |= (1 << nLockBit);
            bPerformLock = W_TRUE;
         }

         nIndex++;

      } while (nIndex <= nHighIndex);

      if (bPerformLock)
      {
         nLockId = P_TYPE1_LOCK_ID_LOW_MEM;
      }
   }
   else
   {
      /* All blocks to lock are located in high memory */

      nIndex  = (uint16_t) ((pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset + pType1ChipUserConnection->nLowMemSize) / 8);
      nHighIndex = (uint16_t) ((pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset + pType1ChipUserConnection->sReadWriteOperation.nHighMemLength -1 + pType1ChipUserConnection->nLowMemSize) / 8);

      PDebugTrace("static_PType1ChipUserLockBlock : locking high mem [%d - %d[",
                     pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset,
                     pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset + pType1ChipUserConnection->sReadWriteOperation.nHighMemLength);

      pType1ChipUserConnection->sReadWriteOperation.nHighMemLength = 0;

      do
      {
         nLockId   = pType1ChipUserConnection->asBlockLock[nIndex].nLockId;
         nLockByte = pType1ChipUserConnection->asBlockLock[nIndex].nLockBit / 8;
         nLockBit  = pType1ChipUserConnection->asBlockLock[nIndex].nLockBit & 7;

         CDebugAssert(nLockId != P_TYPE1_LOCK_ID_LOW_MEM);

         if (nLockId < P_TYPE1_CHIP_NB_MEM_BLOCKS)
         {
            pType1ChipUserConnection->asMemLock[nLockId].aBytes[nLockByte] |= (1 << nLockBit);
            bPerformLock = W_TRUE;
         }

         nIndex++;

      } while (nIndex <= nHighIndex);

      if (bPerformLock)
      {
         nLockId = P_TYPE1_LOCK_ID_HIGH_MEM;
      }
   }

   if (nLockId < P_TYPE1_CHIP_NB_MEM_BLOCKS)
   {

      if (pType1ChipUserConnection->asMemLock[nLockId].nOffset < pType1ChipUserConnection->nLowMemSize)
      {
         PSmartCacheWrite(pContext, &pType1ChipUserConnection->sSmartCache,
            pType1ChipUserConnection->asMemLock[nLockId].nOffset,
            pType1ChipUserConnection->asMemLock[nLockId].nLength,
            pType1ChipUserConnection->asMemLock[nLockId].aBytes,
            static_PType1ChipMemLockBlockCompleted,
            pType1ChipUserConnection);
      }
      else
      {
         PSmartCacheWrite(pContext, &pType1ChipUserConnection->sSmartCacheHighMem,
            pType1ChipUserConnection->asMemLock[nLockId].nOffset - pType1ChipUserConnection->nLowMemSize,
            pType1ChipUserConnection->asMemLock[nLockId].nLength,
            pType1ChipUserConnection->asMemLock[nLockId].aBytes,
            static_PType1ChipMemLockBlockCompleted,
            pType1ChipUserConnection);
      }
   }
}

/**
 * @brief   Called when a lock operation has been completed
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter The callback parameter, eg the Type 1 Chip connection.
 *
 * @param[in]  nError The operation result code.
 **/

static void static_PType1ChipMemLockBlockCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = pCallbackParameter;

   PDebugTrace("static_PType1ChipMemLockBlockCompleted");

   if (nError == W_SUCCESS)
   {
      static_PType1ChipUserLockBlock(pContext, pType1ChipUserConnection);
   }
   else
   {
      static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, nError);
   }
}

static W_HANDLE static_PType1ChipDriverExchangeData(
      tContext * pContext,
      W_HANDLE hDriverConnection,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength )
{
   W_ERROR nError;
   W_HANDLE hHandle;

   hHandle = PType1ChipDriverExchangeData(pContext, hDriverConnection,
         pCallback, pCallbackParameter,
         pReaderToCardBuffer, nReaderToCardBufferLength,
         pCardToReaderBuffer, nCardToReaderBufferMaxLength);

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, 0, nError);
   }

   return hHandle;
}

static void static_PType1ChipExchangeDataCompleted(
      tContext * pContext,
      void * pCallbackParameter,
      uint32_t nLength,
      W_ERROR nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection *) pCallbackParameter;
   PDebugTrace("static_PType1ChipExchangeDataCompleted");

   PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
   pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;

   /* there was duplicated */
   PHandleClose(pContext, pType1ChipUserConnection->hOperation);
   pType1ChipUserConnection->hOperation = W_NULL_HANDLE;

   PDFCPostContext3( &pType1ChipUserConnection->sCallbackContext,
                     nLength,
                     nError);

   PHandleDecrementReferenceCount(pContext, pType1ChipUserConnection);
}

static void static_PType1ChipExchangeData(
      tContext * pContext,
      void* pObject,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      const uint8_t * pReaderToCardBuffer,
      uint32_t nReaderToCardBufferLength,
      uint8_t * pCardToReaderBuffer,
      uint32_t nCardToReaderBufferMaxLength,
      W_HANDLE * phOperation)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pObject;
   tDFCCallbackContext        sCallbackContext;
   W_ERROR                    nError;
   W_HANDLE                   hOperation = W_NULL_HANDLE;

   PDebugTrace("static_PType1ChipExchangeData");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if an operation is still pending */
   if ( pType1ChipUserConnection->hOperation != W_NULL_HANDLE)
   {
      PDebugError("static_PType1ChipExchangeData: W_ERROR_BAD_STATE");

      nError = W_ERROR_BAD_STATE;
      goto error_cleanup;
   }

   if ((pReaderToCardBuffer == null) || (nReaderToCardBufferLength == 0)
      || (pCardToReaderBuffer == null) || (nCardToReaderBufferMaxLength == 0))
   {
      PDebugError("static_PType1ChipExchangeData: BAD State");
      nError = W_ERROR_BAD_PARAMETER;
      goto error_cleanup;
   }

   /* Creates an internal operation handle */
   hOperation = PBasicCreateOperation( pContext, static_PType1ReadOrWriteCancel, pType1ChipUserConnection );
   if (hOperation == W_NULL_HANDLE)
   {
      PDebugError("static_PType1ChipExchangeData: PBasicCreateOperation failed");

      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error_cleanup;
   }

   /* Creates a user operation handle if required by user */
   if (phOperation != null)
   {
      nError = PHandleDuplicate(pContext, hOperation, phOperation);

      if (nError != W_SUCCESS)
      {
         PDebugError("static_PType1ChipExchangeData: PHandleDuplicate failed");
         goto error_cleanup;
      }
   }

   /* ok, all is fine, perform the read */
   PDebugTrace("PHandleIncrementReferenceCount(pType1ChipUserConnection");
   PHandleIncrementReferenceCount(pType1ChipUserConnection);

   pType1ChipUserConnection->hOperation = hOperation;
   pType1ChipUserConnection->sCallbackContext   = sCallbackContext;

   PSmartCacheInvalidateCache(pContext, &pType1ChipUserConnection->sSmartCache, 0, pType1ChipUserConnection->nLowMemSize);

   if (pType1ChipUserConnection->nHighMemSize)
   {
      PSmartCacheInvalidateCache(pContext, &pType1ChipUserConnection->sSmartCacheHighMem, 0, pType1ChipUserConnection->nHighMemSize);
   }

   pType1ChipUserConnection->hDriverOperation = PType1ChipDriverExchangeData(
                                                         pContext,
                                                         pType1ChipUserConnection->hDriverConnection,
                                                         static_PType1ChipExchangeDataCompleted,
                                                         pType1ChipUserConnection,
                                                         pReaderToCardBuffer,
                                                         nReaderToCardBufferLength,
                                                         pCardToReaderBuffer,
                                                         nCardToReaderBufferMaxLength);

   return;

error_cleanup:

   if (hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, hOperation);
      PHandleClose(pContext, hOperation);

      if (phOperation != null)
      {
         if (*phOperation != W_NULL_HANDLE)
         {
            PHandleClose(pContext, * phOperation);
            * phOperation = W_NULL_HANDLE;
         }
      }
   }

   PDFCPostContext3(& sCallbackContext, 0, nError );
}

/**
 * @brief Reads from a Type1 chip
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

static void static_PType1ChipAtomicRead(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      uint8_t                          * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pConnection;
   W_ERROR                    nError;
   uint32_t                   i, nCardBufferLength;

   PDebugTrace("static_PType1ChipAtomicRead [%d-%d[", nSectorOffset, nSectorOffset+nSectorNumber);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pType1ChipUserConnection->sCacheCallbackContext);

   switch (nSectorNumber)
   {
      case 120:
         /* Generates a RALL command */
         /* RALL must be used for all Type1 chips to read the first 120 bytes */

         if (nSectorOffset != 0)
         {
            PDebugError("static_PType1ChipAtomicRead : READALL : nSectorOffset != 0");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         pType1ChipUserConnection->nCurrentReadOperation = P_TYPE1_CHIP_COMMAND_RALL;

         nCardBufferLength = 7;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentReadOperation;

         pType1ChipUserConnection->aReaderToCardBuffer[1] = 0;
         pType1ChipUserConnection->aReaderToCardBuffer[2] = 0;

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[3+i] = pType1ChipUserConnection->aUID[i];
         }

         break;

      default :
         PDebugError("static_PType1ChipAtomicRead : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   pType1ChipUserConnection->pCurrentReadBuffer = pBuffer;

   pType1ChipUserConnection->hDriverOperation = static_PType1ChipDriverExchangeData(pContext, pType1ChipUserConnection->hDriverConnection,
                                 static_PType1ChipAtomicReadCompleted, pType1ChipUserConnection,
                                 pType1ChipUserConnection->aReaderToCardBuffer, nCardBufferLength,
                                 pType1ChipUserConnection->aCardToReaderBuffer, NAL_MESSAGE_MAX_LENGTH);
   return;

error:
   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}


/**
 * @brief Reads from High memory of a Type1 chip
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

static void static_PType1ChipAtomicReadHighMem(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      uint8_t                          * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pConnection;
   W_ERROR                    nError;
   uint32_t                   i, nCardBufferLength;

   PDebugTrace("static_PType1ChipAtomicReadHighMem [%d-%d[", nSectorOffset, nSectorOffset+nSectorNumber);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pType1ChipUserConnection->sCacheCallbackContext);

   switch (nSectorNumber)
   {
      case 4  :
         /* Generates a READ4 command */
         /* READ4 is only supported for 5003 and 5004 Type1 chip variants
            READ4 access must be aligned to 4 bytes
            We use separate cache for low area and upper area, so the start address of this block restarts at zero */

         if ( (pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5003) &&
              (pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5004))
         {
            PDebugError("static_PType1ChipAtomicReadHighMem : READ4 not supported by the chip ");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         if ((nSectorOffset & 3) != 0)
         {
            PDebugError("static_PType1ChipAtomicReadHighMem : READ4 : nSectorOffset is not aligned to a 4 bytes boundary");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         pType1ChipUserConnection->nCurrentReadOperation = P_TYPE1_CHIP_COMMAND_READ4;

         nCardBufferLength = 10;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentReadOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) (nSectorOffset / 4);

         CMemoryFill(& pType1ChipUserConnection->aReaderToCardBuffer[2], 0, 4);

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[6+i] = pType1ChipUserConnection->aUID[i];
         }

         break;

      case 8  :
         /* Generates a READ8 command */
         /* READ8 is only supported for 5005 and 5006 Type1 chip variants
            READ8 allows to access only to the chip upper memory area (not the 120 first bytes)
            We use separate cache for low area and upper area, so the start address of this block restarts at zero */

         if ((pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5005) &&
             (pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5006))
         {
            PDebugError("static_PType1ChipAtomicReadHighMem : READ8 not supported by the chip ");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         if ((nSectorOffset & 7) != 0)
         {
            PDebugError("static_PType1ChipAtomicReadHighMem : READ8 : nSectorOffset is not aligned to a 8 bytes boundary");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         pType1ChipUserConnection->nCurrentReadOperation = P_TYPE1_CHIP_COMMAND_READ8;

         nCardBufferLength = 14;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentReadOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) (nSectorOffset / 8);

         CMemoryFill(&pType1ChipUserConnection->aReaderToCardBuffer[2], 0, 8);

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[10+i] = pType1ChipUserConnection->aUID[i];
         }
         break;

      default :
         PDebugError("static_PType1ChipAtomicReadHighMem : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }


   pType1ChipUserConnection->pCurrentReadBuffer = pBuffer;

   pType1ChipUserConnection->hDriverOperation = static_PType1ChipDriverExchangeData(pContext, pType1ChipUserConnection->hDriverConnection,
                                 static_PType1ChipAtomicReadCompleted, pType1ChipUserConnection,
                                 pType1ChipUserConnection->aReaderToCardBuffer, nCardBufferLength,
                                 pType1ChipUserConnection->aCardToReaderBuffer, NAL_MESSAGE_MAX_LENGTH);

   return;

error:
   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}


/**
 * @brief Called when a Read operation has been completed on a Type1 chip
 *
 * @param[in]  pContext The context.
 *
 * @param[in]  pCallbackParameter The callback parameter, eg the Type 1 Chip connection.
 *
 * @param[in]  nDataLength The read data length
 *
 * @param[in]  nError The operation result code.
 */

static void static_PType1ChipAtomicReadCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t   nDataLength,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pCallbackParameter;

   PDebugTrace("static_PType1ChipAtomicReadCompleted");

   if (nError == W_SUCCESS)
   {
      switch (pType1ChipUserConnection->nCurrentReadOperation)
      {
         case P_TYPE1_CHIP_COMMAND_READ4 :

            if (nDataLength != 5)
            {
               /* Invalid READ4 answer length ! */
               PDebugError("static_PType1ChipAtomicReadCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            CMemoryCopy(pType1ChipUserConnection->pCurrentReadBuffer, & pType1ChipUserConnection->aCardToReaderBuffer[1], 4);

            break;

         case P_TYPE1_CHIP_COMMAND_READ8 :

            if (nDataLength != 9)
            {
               /* Invalid READ8 answer length ! */
               PDebugError("static_PType1ChipAtomicReadCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            CMemoryCopy(pType1ChipUserConnection->pCurrentReadBuffer, & pType1ChipUserConnection->aCardToReaderBuffer[1], 8);

            break;

         case P_TYPE1_CHIP_COMMAND_RALL :

            if (nDataLength != 122)
            {
               /* Invalid READ ALL answer length ! */
               PDebugError("static_PType1ChipAtomicReadCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            CMemoryCopy(pType1ChipUserConnection->pCurrentReadBuffer, & pType1ChipUserConnection->aCardToReaderBuffer[2], 120);

            break;

         default :
            PDebugError("static_PType1ChipAtomicReadCompleted - unexpected event");
            nError = W_ERROR_BAD_STATE;
            break;
      }
   }
   else
   {
      PDebugError("static_PType1ChipAtomicReadCompleted %s", PUtilTraceError(nError));
   }

   /* Close driver operation */
   PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
   pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;

   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}


/**
 * @brief Writes in a Type1 chip
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

static void static_PType1ChipAtomicWrite(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      const uint8_t                    * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pConnection;
   W_ERROR                    nError;
   uint32_t                   i, nCardBufferLength;
   uint8_t                  * pCurrentCacheValue;

   PDebugTrace("static_PType1ChipAtomicWrite [%d-%d]", nSectorOffset, nSectorOffset+nSectorNumber);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pType1ChipUserConnection->sCacheCallbackContext);

      /* The current write operation has been canceled by user, do not perform the write operation */

   if ( (pType1ChipUserConnection->hOperation != W_NULL_HANDLE) &&
        (PBasicGetOperationState(pContext, pType1ChipUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      nError = W_ERROR_CANCEL;
      goto error;
   }

   switch (nSectorNumber)
   {
      case 1  :
         /* Generate a WRITE command */
         /* WRITE command is only supported for accessing the 120 first bytes */
         if (nSectorOffset + nSectorNumber > 120)
         {
            PDebugError("static_PType1ChipAtomicWrite : nSectorOffset + nSectorNumber > 120");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         /* depending of the old value of the cache, we can use fast non-erase write */

         pCurrentCacheValue = PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCache, nSectorOffset, 1);

         if (pCurrentCacheValue != null)
         {
            if ((pBuffer[0] | pCurrentCacheValue[0]) != pBuffer[0])
            {
               /* Some bit set to 1 in the previous value need to be reset
                  -> use the erase operation */
               pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE_E;
            }
            else
            {
               /* No bit set to 1 in the previous value need to be reset
                  -> use the non-erase operation */
               pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE_NE;
            }
         }
         else
         {
            /* not in the cache, use the erase operation */
            pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE_E;
         }

         nCardBufferLength = 7;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentWriteOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) nSectorOffset;
         pType1ChipUserConnection->aReaderToCardBuffer[2] = * pBuffer;

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[3+i] = pType1ChipUserConnection->aUID[i];
         }

         break;

      case 4  :
         /* Generates a WRITE4 command */
         /* WRITE4 is only supported for 5002, 5003 and 5004 Type1 chip variants
            WRITE4 allows to access to the entire chip memory range
            WRITE4 must be aligned to a 4 bytes boundary */

         if ((pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5002) &&
             (pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5003) &&
             (pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5004))
         {
            PDebugError("static_PType1ChipAtomicWrite : WRITE4 not supported by the chip ");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         if ((nSectorOffset & 3) != 0)
         {
            PDebugError("static_PType1ChipAtomicWrite : WRITE4 : nSectorOffset is not aligned to a 4 bytes boundary");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         /* depending of the old value of the cache, we can use fast non-erase write */

         if (nSectorOffset < 120)
         {
            pCurrentCacheValue = PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCache, nSectorOffset, 4);
         }
         else
         {
            pCurrentCacheValue = PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCacheHighMem, nSectorOffset - 120, 4);
         }

         if (pCurrentCacheValue != null)
         {
            pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE4_NE;

            for (i = 0; i<4; i++)
            {
               if ((pBuffer[i] | pCurrentCacheValue[i]) != pBuffer[i])
               {
                  /* Some bit set to 1 in the previous value need to be reset
                     -> use the erase operation */
                  pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE4_E;
                  break;
               }
            }
         }
         else
         {
            /* not in the cache, use the erase operation */
            pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE4_E;
         }

         nCardBufferLength = 10;

         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentWriteOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) ( nSectorOffset / 4);

         CMemoryCopy(& pType1ChipUserConnection->aReaderToCardBuffer[2], pBuffer, 4);

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[6+i] = pType1ChipUserConnection->aUID[i];
         }
         break;

      case 8  :
         /* Generates a WRITE8 command */
         /* WRITE8 is only supported for 5005 and 5006 Type1 chip variants
            WRITE8 allows to access only to the chip upper memory area (not the 120 first bytes)
            We use separate cache for low area and upper area, so the start address of this block restarts at zero
            WRITE8 must be aligned to 8 bytes boundary */

         if ((pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5005) &&
             (pType1ChipUserConnection->nType1ChipVariant != P_TYPE1_CHIP_IRT5006))
         {
            PDebugError("static_PType1ChipAtomicWrite : WRITE4 not supported by the chip ");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         if ((nSectorOffset & 7) != 0)
         {
            PDebugError("static_PType1ChipAtomicWrite : WRITE8 : nSectorOffset is not aligned to a 8 bytes boundary");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         /* depending of the old value of the cache, we can use fast non-erase write */

         pCurrentCacheValue = PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCacheHighMem, nSectorOffset, 8);

         if (pCurrentCacheValue != null)
         {
            pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE8_NE;

            for (i = 0; i<8; i++)
            {
               if ((pBuffer[i] | pCurrentCacheValue[i]) != pBuffer[i])
               {
                  /* Some bit set to 1 in the previous value need to be reset
                     -> use the erase operation */
                  pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE8_E;
                  break;
               }
            }
         }
         else
         {
            /* not in the cache, use the erase operation */
            pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_CHIP_COMMAND_WRITE8_E;
         }


         nCardBufferLength = 14;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentWriteOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) (nSectorOffset / 8);

         CMemoryCopy(& pType1ChipUserConnection->aReaderToCardBuffer[2], pBuffer, 8);

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[10+i] = pType1ChipUserConnection->aUID[i];
         }
         break;

      default :
         PDebugError("static_PType1ChipAtomicWrite : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   pType1ChipUserConnection->pCurrentWriteBuffer = pBuffer;

   pType1ChipUserConnection->hDriverOperation = static_PType1ChipDriverExchangeData(pContext, pType1ChipUserConnection->hDriverConnection,
                                 static_PType1ChipAtomicWriteCompleted, pType1ChipUserConnection,
                                 pType1ChipUserConnection->aReaderToCardBuffer, nCardBufferLength,
                                 pType1ChipUserConnection->aCardToReaderBuffer, NAL_MESSAGE_MAX_LENGTH);

   return;

error:
   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}

static void static_PType1ChipAtomicWriteHighMem(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      const uint8_t                    * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   static_PType1ChipAtomicWrite(
            pContext,
            pConnection,
            nSectorOffset + 120,    /* + 120 since high memory starts at 120 */
            nSectorNumber,
            pBuffer,
            pCallback,
            pCallbackParameter);
}

/**
 * @brief Called when a Write operation has been completed on a Type1 chip
 *
 * @param[in]  pContext The context.
 *
 * @param[in]  pCallbackParameter The callback parameter, eg the Type 1 Chip connection.
 *
 * @param[in]  nDataLength The read data length
 *
 * @param[in]  nError The operation result code.
 */

static void static_PType1ChipAtomicWriteCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t   nDataLength,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pCallbackParameter;

   PDebugTrace("static_PType1ChipAtomicWriteCompleted");

   if (nError == W_SUCCESS)
   {
      switch (pType1ChipUserConnection->nCurrentWriteOperation)
      {
         case P_TYPE1_CHIP_COMMAND_WRITE_NE :
         case P_TYPE1_CHIP_COMMAND_WRITE_E :

            if (nDataLength != P_TYPE1_CHIP_COMMAND_WRITE_ANSWER_LENGTH)
            {
               /* Invalid WRITE answer length ! */
               PDebugError("static_PType1ChipAtomicWriteCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            if (CMemoryCompare(pType1ChipUserConnection->pCurrentWriteBuffer,  & pType1ChipUserConnection->aCardToReaderBuffer[1], 1) != 0)
            {
               PDebugError("static_PType1ChipAtomicWriteCompleted : invalid data\n");
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            break;

         case P_TYPE1_CHIP_COMMAND_WRITE4_NE :
         case P_TYPE1_CHIP_COMMAND_WRITE4_E :

            if (nDataLength != P_TYPE1_CHIP_COMMAND_WRITE4_ANSWER_LENGTH)
            {
               /* Invalid READ4 answer length ! */
               PDebugError("static_PType1ChipAtomicWriteCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            if (CMemoryCompare(pType1ChipUserConnection->pCurrentWriteBuffer,  & pType1ChipUserConnection->aCardToReaderBuffer[1], 4) != 0)
            {
               PDebugError("static_PType1ChipAtomicWriteCompleted : invalid data\n");
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            break;

         case P_TYPE1_CHIP_COMMAND_WRITE8_NE :
         case P_TYPE1_CHIP_COMMAND_WRITE8_E :

            if (nDataLength != P_TYPE1_CHIP_COMMAND_WRITE8_ANSWER_LENGTH)
            {
               /* Invalid READ8 answer length ! */
               PDebugError("static_PType1ChipAtomicWriteCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            if (CMemoryCompare(pType1ChipUserConnection->pCurrentWriteBuffer,  & pType1ChipUserConnection->aCardToReaderBuffer[1], 8) != 0)
            {
               PDebugError("static_PType1ChipAtomicWriteCompleted : invalid data\n");
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            break;

         default :
            PDebugError("static_PType1ChipAtomicWriteCompleted - unexpected event");
            nError = W_ERROR_BAD_STATE;
            break;
      }
   }
   else
   {
      PDebugError("static_PType1ChipAtomicWriteCompleted %s", PUtilTraceError(nError));
   }

   /* Close driver operation */
   PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
   pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;

   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}

/**
 * Reads from a Type1 Static TAG
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

static void static_PType1StaticTagAtomicRead(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      uint8_t                          * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pConnection;
   W_ERROR                    nError;
   uint32_t                   i, nCardBufferLength;

   PDebugTrace("static_PType1StaticTagAtomicRead [%d-%d]", nSectorOffset, nSectorOffset+nSectorNumber);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pType1ChipUserConnection->sCacheCallbackContext);

   switch (nSectorNumber)
   {
      case 120 :

         /* Generates a RALL command */
         /* RALL must be used for all Type1 TAG to read the first 120 bytes */

         if (nSectorOffset != 0)
         {
            PDebugError("static_PType1StaticTagAtomicRead : READALL : nSectorOffset != 0");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         pType1ChipUserConnection->nCurrentReadOperation = P_TYPE1_TAG_COMMAND_RALL;

         nCardBufferLength = 7;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentReadOperation;

         pType1ChipUserConnection->aReaderToCardBuffer[1] = 0;
         pType1ChipUserConnection->aReaderToCardBuffer[2] = 0;

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[3+i] = pType1ChipUserConnection->aUID[i];
         }

         break;

      default :
         PDebugError("static_PType1StaticTagAtomicRead : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   pType1ChipUserConnection->pCurrentReadBuffer = pBuffer;

   pType1ChipUserConnection->hDriverOperation = static_PType1ChipDriverExchangeData(pContext, pType1ChipUserConnection->hDriverConnection,
                                 static_PType1StaticTagAtomicReadCompleted, pType1ChipUserConnection,
                                 pType1ChipUserConnection->aReaderToCardBuffer, nCardBufferLength,
                                 pType1ChipUserConnection->aCardToReaderBuffer, NAL_MESSAGE_MAX_LENGTH);

   return;

error:
   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}

/**
 * @brief Called when a Read operation has been completed on a Type1 static TAG
 *
 * @param[in]  pContext The context.
 *
 * @param[in]  pCallbackParameter The callback parameter, eg the Type 1 Chip connection.
 *
 * @param[in]  nDataLength The read data length
 *
 * @param[in]  nError The operation result code.
 */

static void static_PType1StaticTagAtomicReadCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t   nDataLength,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pCallbackParameter;

   PDebugTrace("static_PType1StaticTagAtomicReadCompleted");

   if (nError == W_SUCCESS)
   {
      switch (pType1ChipUserConnection->nCurrentReadOperation)
      {
         case P_TYPE1_TAG_COMMAND_RALL :

            if (nDataLength != P_TYPE1_TAG_COMMAND_RALL_ANSWER_LENGTH)
            {
               /* Invalid READ ALL answer length ! */
               PDebugError("static_PType1StaticTagAtomicReadCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            CMemoryCopy(pType1ChipUserConnection->pCurrentReadBuffer, & pType1ChipUserConnection->aCardToReaderBuffer[2], 120);

            break;

         default :
            PDebugError("static_PType1StaticTagAtomicReadCompleted - unexpected event");
            nError = W_ERROR_BAD_STATE;
            break;
      }
   }
   else
   {
      PDebugError("static_PType1StaticTagAtomicReadCompleted %s", PUtilTraceError(nError));
   }

   /* Close driver operation */
   PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
   pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;

   /* Post result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}


/**
 * Writes in a Type1 Static Tag
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

static void static_PType1StaticTagAtomicWrite(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      const uint8_t                    * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pConnection;
   W_ERROR                    nError;
   uint32_t                   i, nCardBufferLength;
   uint8_t                  * pCurrentCacheValue;

   PDebugTrace("static_PType1StaticTagAtomicWrite [%d-%d]", nSectorOffset, nSectorOffset+nSectorNumber);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pType1ChipUserConnection->sCacheCallbackContext);

      /* The current write operation has been canceled by user, do not perform the write operation */

   if ( (pType1ChipUserConnection->hOperation != W_NULL_HANDLE) &&
        (PBasicGetOperationState(pContext, pType1ChipUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      nError = W_ERROR_CANCEL;
      goto error;
   }

   switch (nSectorNumber)
   {
      case 1 :
         /* Generate a WRITE command */
         /* WRITE command is only supported for accessing the 120 first bytes */

         if (nSectorOffset + nSectorNumber > 120)
         {
            PDebugError("static_PType1StaticTagAtomicWrite : nSectorOffset + nSectorNumber > 120");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         /* depending of the old value of the cache, we can use fast non-erase write */

         pCurrentCacheValue = PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCache, nSectorOffset, 1);

         if (pCurrentCacheValue != null)
         {
            if ((pBuffer[0] | pCurrentCacheValue[0]) != pBuffer[0])
            {
               /* Some bit set to 1 in the previous value need to be reset
                 -> use the erase operation */
               pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_TAG_COMMAND_WRITE_E;
            }
            else
            {
               /* No bit set to 1 in the previous value need to be reset
                 -> use the non-erase operation */
               pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_TAG_COMMAND_WRITE_NE;
            }
         }
         else
         {
            /* not in the cache, use the erase operation */
            pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_TAG_COMMAND_WRITE_E;
         }

         nCardBufferLength = 7;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentWriteOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) nSectorOffset;
         pType1ChipUserConnection->aReaderToCardBuffer[2] = * pBuffer;

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[3+i] = pType1ChipUserConnection->aUID[i];
         }
         break;

      default :
         PDebugError("static_PType1StaticTagAtomicWrite : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   pType1ChipUserConnection->pCurrentWriteBuffer = pBuffer;

   pType1ChipUserConnection->hDriverOperation = static_PType1ChipDriverExchangeData(pContext, pType1ChipUserConnection->hDriverConnection,
                              static_PType1StaticTagAtomicWriteCompleted, pType1ChipUserConnection,
                              pType1ChipUserConnection->aReaderToCardBuffer, nCardBufferLength,
                              pType1ChipUserConnection->aCardToReaderBuffer, NAL_MESSAGE_MAX_LENGTH);

   return;

error:
   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}

/**
 * @brief Called when a Write operation has been completed on a Type1 static TAG
 *
 * @param[in]  pContext The context.
 *
 * @param[in]  pCallbackParameter The callback parameter, eg the Type 1 Chip connection.
 *
 * @param[in]  nDataLength The read data length
 *
 * @param[in]  nError The operation result code.
 */

static void static_PType1StaticTagAtomicWriteCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t   nDataLength,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pCallbackParameter;

   PDebugTrace("static_PType1StaticTagAtomicWriteCompleted");

   if (nError == W_SUCCESS)
   {
      switch (pType1ChipUserConnection->nCurrentWriteOperation)
      {
         case P_TYPE1_TAG_COMMAND_WRITE_NE :
         case P_TYPE1_TAG_COMMAND_WRITE_E :

            if (nDataLength != P_TYPE1_TAG_COMMAND_WRITE_ANSWER_LENGTH)
            {
               /* Invalid WRITE answer length ! */
               PDebugError("static_PType1StaticTagAtomicWriteCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            if (CMemoryCompare(pType1ChipUserConnection->pCurrentWriteBuffer,  & pType1ChipUserConnection->aCardToReaderBuffer[1], 1) != 0)
            {
               PDebugError("static_PType1StaticTagAtomicWriteCompleted : invalid data\n");
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            break;

         default :
            PDebugError("static_PType1StaticTagAtomicWriteCompleted - unexpected event");
            nError = W_ERROR_BAD_STATE;
            break;
      }
   }
   else
   {
      PDebugError("static_PType1StaticTagAtomicWriteCompleted %s", PUtilTraceError(nError));
   }

   /* Close driver operation */
   PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
   pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;

   /* Post result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}

/**
 * Reads from a Type1 Dynamic TAG high memory
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

static void static_PType1DynamicTagAtomicRead(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      uint8_t                          * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pConnection;
   W_ERROR                    nError;
   uint32_t                   i, nCardBufferLength;

   PDebugTrace("static_PType1DynamicTagAtomicRead [%d-%d]", nSectorOffset, nSectorOffset+nSectorNumber);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pType1ChipUserConnection->sCacheCallbackContext);

   switch (nSectorNumber)
   {
      case 8  :
         /* Generates a READ8 command
            The READ8 must be aligned to 8 bytes boundary */

         if ((nSectorOffset & 7) != 0)
         {
            PDebugError("static_PType1DynamicTagAtomicRead : READ8 : nSectorOffset is not aligned to a 8 bytes boundary");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         pType1ChipUserConnection->nCurrentReadOperation = P_TYPE1_TAG_COMMAND_READ8;

         nCardBufferLength = 14;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentReadOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) (nSectorOffset / 8);

         CMemoryFill(&pType1ChipUserConnection->aReaderToCardBuffer[2], 0, 8);

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[10+i] = pType1ChipUserConnection->aUID[i];
         }

         break;

      case 128  :

         /* Generates a READSEG command
            READSEG must be aligned to a 128 bytes boundary */

         if ((nSectorOffset & 127) != 0)
         {
            PDebugError("static_PType1DynamicTagAtomicRead : READSEG : nSectorOffset is not aligned to a 128 bytes boundary");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         pType1ChipUserConnection->nCurrentReadOperation = P_TYPE1_TAG_COMMAND_READSEG;

         nCardBufferLength = 14;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentReadOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) (((nSectorOffset / 128)) << 4);

         CMemoryFill(&pType1ChipUserConnection->aReaderToCardBuffer[2], 0, 8);

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[10+i] = pType1ChipUserConnection->aUID[i];
         }
         break;


      default :
         PDebugError("static_PType1DynamicTagAtomicRead : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }


   pType1ChipUserConnection->pCurrentReadBuffer = pBuffer;

   pType1ChipUserConnection->hDriverOperation = static_PType1ChipDriverExchangeData(pContext, pType1ChipUserConnection->hDriverConnection,
                              static_PType1DynamicTagAtomicReadCompleted, pType1ChipUserConnection,
                              pType1ChipUserConnection->aReaderToCardBuffer, nCardBufferLength,
                              pType1ChipUserConnection->aCardToReaderBuffer, NAL_MESSAGE_MAX_LENGTH);

   return;

error:
   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}


static void static_PType1DynamicTagAtomicReadHighMem(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      uint8_t                          * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{

   /* Wrapper that increments the Sector offset of 128 bytes */

   static_PType1DynamicTagAtomicRead(
      pContext,
      pConnection,
      nSectorOffset + 128,
      nSectorNumber,
      pBuffer,
      pCallback,
      pCallbackParameter);
}

/**
 * @brief Called when a Read operation has been completed on a Type1 dynamic TAG
 *
 * @param[in]  pContext The context.
 *
 * @param[in]  pCallbackParameter The callback parameter, eg the Type 1 Chip connection.
 *
 * @param[in]  nDataLength The read data length
 *
 * @param[in]  nError The operation result code.
 */

static void static_PType1DynamicTagAtomicReadCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t   nDataLength,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pCallbackParameter;

   PDebugTrace("static_PType1DynamicTagAtomicReadCompleted");

   if (nError == W_SUCCESS)
   {
      switch (pType1ChipUserConnection->nCurrentReadOperation)
      {
         case P_TYPE1_TAG_COMMAND_READ8 :

            if (nDataLength != P_TYPE1_TAG_COMMAND_READ8_ANSWER_LENGTH)
            {
               /* Invalid READ8 answer length ! */
               PDebugError("static_PType1DynamicTagAtomicReadCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            CMemoryCopy(pType1ChipUserConnection->pCurrentReadBuffer, & pType1ChipUserConnection->aCardToReaderBuffer[1], 4);

            break;

         case P_TYPE1_TAG_COMMAND_READSEG :

            if (nDataLength != P_TYPE1_TAG_COMMAND_READSEG_ANSWER_LENGTH)
            {
               /* Invalid READ ALL answer length ! */
               PDebugError("static_PType1DynamicTagAtomicReadCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            CMemoryCopy(pType1ChipUserConnection->pCurrentReadBuffer, & pType1ChipUserConnection->aCardToReaderBuffer[1], 128);

            break;

         default :
            PDebugError("static_PType1DynamicTagAtomicReadCompleted - unexpected event");
            nError = W_ERROR_BAD_STATE;
            break;
      }
   }
   else
   {
      PDebugError("static_PType1DynamicTagAtomicReadCompleted %s", PUtilTraceError(nError));
   }

   /* Close driver operation */
   PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
   pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;

   /* Post result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}


/**
 * Writes in a Type1 Dynamic Tag
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

static void static_PType1DynamicTagAtomicWrite(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      const uint8_t                    * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pConnection;
   W_ERROR                    nError;
   uint32_t                   i, nCardBufferLength;
   uint8_t                  * pCurrentCacheValue;

   PDebugTrace("static_PType1DynamicTagAtomicWrite [%d-%d]", nSectorOffset, nSectorOffset+nSectorNumber);

   PDFCFillCallbackContext(pContext, (tDFCCallback *)  pCallback, pCallbackParameter, &pType1ChipUserConnection->sCacheCallbackContext);

     /* The current write operation has been canceled by user, do not perform the write operation */

   if ( (pType1ChipUserConnection->hOperation != W_NULL_HANDLE) &&
        (PBasicGetOperationState(pContext, pType1ChipUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      nError = W_ERROR_CANCEL;
      goto error;
   }

   switch (nSectorNumber)
   {
      case 8  :
         /* Generates a WRITE8 command
            WRITE8 must be aligned to 8 bytes boundary */

         if ((nSectorOffset & 7) != 0)
         {
            PDebugError("static_PType1DynamicTagAtomicWrite : WRITE8 : nSectorOffset is not aligned to a 8 bytes boundary");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         /* depending of the old value of the cache, we can use fast non-erase write */

         if (nSectorOffset <= 127)
         {
            pCurrentCacheValue = PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCache, nSectorOffset, 8);
         }
         else
         {
            pCurrentCacheValue = PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCacheHighMem, nSectorOffset - 128 , 8);
         }


         if (pCurrentCacheValue != null)
         {
            pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_TAG_COMMAND_WRITE8_NE;

            for (i = 0; i<8; i++)
            {
               if ((pBuffer[i] | pCurrentCacheValue[i]) != pBuffer[i])
               {
                  /* Some bit set to 1 in the previous value need to be reset
                     -> use the erase operation */
                  pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_TAG_COMMAND_WRITE8_E;
                  break;
               }
            }
         }
         else
         {
            /* not in the cache, use the erase operation */
            pType1ChipUserConnection->nCurrentWriteOperation = P_TYPE1_TAG_COMMAND_WRITE8_E;
         }

         nCardBufferLength = 14;
         pType1ChipUserConnection->aReaderToCardBuffer[0] = pType1ChipUserConnection->nCurrentWriteOperation;
         pType1ChipUserConnection->aReaderToCardBuffer[1] = (uint8_t) (nSectorOffset / 8);

         CMemoryCopy(&pType1ChipUserConnection->aReaderToCardBuffer[2], pBuffer, 8);

         for (i=0; i<4; i++)
         {
            pType1ChipUserConnection->aReaderToCardBuffer[10+i] = pType1ChipUserConnection->aUID[i];
         }
         break;

      default :
         PDebugError("static_PType1DynamicTagAtomicWrite : invalid nSectorNumber %d\n", nSectorNumber);
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   pType1ChipUserConnection->pCurrentWriteBuffer = pBuffer;

   pType1ChipUserConnection->hDriverOperation = static_PType1ChipDriverExchangeData(pContext, pType1ChipUserConnection->hDriverConnection,
                              static_PType1DynamicTagAtomicWriteCompleted, pType1ChipUserConnection,
                              pType1ChipUserConnection->aReaderToCardBuffer, nCardBufferLength,
                              pType1ChipUserConnection->aCardToReaderBuffer, NAL_MESSAGE_MAX_LENGTH);

   return;

error:
   /* Post the result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}

static void static_PType1DynamicTagAtomicWriteHighMem(
      tContext                         * pContext,
      void                             * pConnection,
      uint32_t                           nSectorOffset,
      uint32_t                           nSectorNumber,
      const uint8_t                    * pBuffer,
      tPBasicGenericCallbackFunction   * pCallback,
      void                             * pCallbackParameter)
{

   /* Wrapper that increments the Sector offset of 128 bytes */

   static_PType1DynamicTagAtomicWrite(
      pContext,
      pConnection,
      nSectorOffset + 128,
      nSectorNumber,
      pBuffer,
      pCallback,
      pCallbackParameter);
}

/**
 * @brief Called when a Write operation has been completed on a Type1 dynamic TAG
 *
 * @param[in]  pContext The context.
 *
 * @param[in]  pCallbackParameter The callback parameter, eg the Type 1 Chip connection.
 *
 * @param[in]  nDataLength The read data length
 *
 * @param[in]  nError The operation result code.
 */

static void static_PType1DynamicTagAtomicWriteCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   uint32_t   nDataLength,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pCallbackParameter;

   PDebugTrace("static_PType1DynamicTagAtomicWriteCompleted");

   if (nError == W_SUCCESS)
   {
      switch (pType1ChipUserConnection->nCurrentWriteOperation)
      {
         case P_TYPE1_TAG_COMMAND_WRITE8_NE :
         case P_TYPE1_TAG_COMMAND_WRITE8_E :

            if (nDataLength != 9)
            {
               /* Invalid WRITE8 answer length ! */
               PDebugError("static_PType1DynamicTagAtomicWriteCompleted - invalid nDataLength %d\n", nDataLength);
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            if (CMemoryCompare(pType1ChipUserConnection->pCurrentWriteBuffer,  & pType1ChipUserConnection->aCardToReaderBuffer[1], 8) != 0)
            {
               PDebugError("static_PType1DynamicTagAtomicWriteCompleted : invalid data\n");
               nError = W_ERROR_TAG_DATA_INTEGRITY;
               break;
            }

            break;

         default :
            PDebugError("static_PType1DynamicTagAtomicWriteCompleted - unexpected event");
            nError = W_ERROR_BAD_STATE;
            break;
      }
   }
   else
   {
      PDebugError("static_PType1DynamicTagAtomicWriteCompleted %s", PUtilTraceError(nError));
   }

   /* Close driver operation */
   PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
   pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;

   /* Post result */
   PDFCPostContext2(&pType1ChipUserConnection->sCacheCallbackContext, nError);
}


/* See Header file */
void PType1ChipUserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tPType1ChipUserConnection * pType1ChipUserConnection = null ;
   tDFCCallbackContext         sCallbackContext;
   W_ERROR                     nError;
   tSmartCacheDescriptor     * pLowMemDescriptor = null;
   tSmartCacheDescriptor     * pHighMemDescriptor = null;
   uint32_t                    i;

   /* Initialize the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the connection */
   pType1ChipUserConnection = (tPType1ChipUserConnection*)CMemoryAlloc( sizeof(tPType1ChipUserConnection) );

   if ( pType1ChipUserConnection == null )
   {
      PDebugError("PType1ChipUserCreateConnection: pType1ChipUserConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }

   CMemoryFill(pType1ChipUserConnection, 0, sizeof(tPType1ChipUserConnection));

   /* Store the callback context */
   pType1ChipUserConnection->sCallbackContext = sCallbackContext;

   /* Store the connection information */
   pType1ChipUserConnection->hUserConnection   = hUserConnection;
   pType1ChipUserConnection->hDriverConnection = hDriverConnection;

   if(nLength != P_TYPE1_CHIP_DETECTION_MESSAGE_SIZE)
   {
      PDebugError("PType1ChipUserCreateConnection: Error in the message length");
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_error;
   }

   pType1ChipUserConnection->aHeaderRom[0] = pBuffer[4];
   pType1ChipUserConnection->aHeaderRom[1] = pBuffer[5];

   /* UID is still incomplete, 4 bytes have been received but 3 bytes are missing */
   /* The first 4 byte of the UID are used for the next command */

   CMemoryCopy(pType1ChipUserConnection->aUID, pBuffer, P_TYPE1_CHIP_SMALL_UID_LENGTH);

   if((pType1ChipUserConnection->aHeaderRom[0] & 0xF0) == 0x10)
   {
      /* Tags variants */

      if ((pType1ChipUserConnection->aHeaderRom[0]) == P_TYPE1_TAG_STATIC_HR0)
      {
         PDebugTrace("PType1ChipUserCreateConnection: NFC Type 1 STATIC TAG ");

         pType1ChipUserConnection->nType1ChipVariant = P_TYPE1_TAG_STATIC;
         pType1ChipUserConnection->nLowMemSize = P_TYPE1_TAG_STATIC_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;
         pType1ChipUserConnection->nHighMemSize = 0;
         pLowMemDescriptor = & g_sStaticTagCacheDescriptor;
         pHighMemDescriptor = null;
      }
      else
      {
         PDebugTrace("PType1ChipUserCreateConnection: NFC Type 1 DYNAMIC TAG ");

         pType1ChipUserConnection->nType1ChipVariant = P_TYPE1_TAG_DYNAMIC;
         pType1ChipUserConnection->nLowMemSize = P_TYPE1_TAG_DYNAMIC_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;

         pLowMemDescriptor = & g_sDynamicTagCacheDescriptor;

         if (pType1ChipUserConnection->aHeaderRom[0] == P_TYPE1_TAG_TOPAZ_512_HR0)
         {
            PDebugTrace("PType1ChipUserCreateConnection: NFC Type 1 DYNAMIC TAG (512 bytes)");

            pType1ChipUserConnection->nHighMemSize = P_TYPE1_TAG_TOPAZ_512_HIGHMEM_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;
            pHighMemDescriptor = & g_sDynamicTagCacheDescriptorHighMem;
         }
         else
         {
            PDebugTrace("PType1ChipUserCreateConnection: NFC Type 1 DYNAMIC TAG (unknown size)");

            /* Unknown Dynamic Type 1 Tag */
            /* we do not know yet the size of high memory, will be compute later from CC */
            pType1ChipUserConnection->nHighMemSize = 0;
            pHighMemDescriptor = null;
         }
      }
   }
   else
   {
      switch(pType1ChipUserConnection->aHeaderRom[0])
      {
         case P_TYPE1_CHIP_IRT5001_HR0:

            /* Do not check the HR1 value since we encountered some tags with HR1 value different from the one specified in the datasheet */

            PDebugTrace("PType1ChipUserCreateConnection: IRT5001 (96 Bytes)");

            pType1ChipUserConnection->nType1ChipVariant = P_TYPE1_CHIP_IRT5001;
            pType1ChipUserConnection->nLowMemSize = P_TYPE1_CHIP_IRT5001_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;
            pType1ChipUserConnection->nHighMemSize = 0;

            pLowMemDescriptor = & g_sLowMemCacheDescriptor;
            pHighMemDescriptor = null;

            break;

         case P_TYPE1_CHIP_IRT5002_HR0:

            PDebugTrace("PType1ChipUserCreateConnection: IRT5002 (96 Bytes with added 4 Byte write)");

            pType1ChipUserConnection->nType1ChipVariant = P_TYPE1_CHIP_IRT5002;
            pType1ChipUserConnection->nLowMemSize = P_TYPE1_CHIP_IRT5002_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;
            pType1ChipUserConnection->nHighMemSize = 0;

            pLowMemDescriptor = & g_sLowMem4CacheDescriptor;
            pHighMemDescriptor = null;
            break;

         case P_TYPE1_CHIP_IRT5003_HR0:
            PDebugTrace("PType1ChipUserCreateConnection: IRT5003 (192 Bytes)");

            pType1ChipUserConnection->nType1ChipVariant = P_TYPE1_CHIP_IRT5003;
            pType1ChipUserConnection->nLowMemSize = P_TYPE1_CHIP_IRT5003_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;
            pType1ChipUserConnection->nHighMemSize = P_TYPE1_CHIP_IRT5003_HIGHMEM_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;

            pLowMemDescriptor = & g_sLowMem4CacheDescriptor;
            pHighMemDescriptor = & g_sHighMem4CacheDescriptor;
            break;

         case P_TYPE1_CHIP_IRT5004_HR0:
            PDebugTrace("PType1ChipUserCreateConnection: IRT5004 (384 Bytes)");

            pType1ChipUserConnection->nType1ChipVariant = P_TYPE1_CHIP_IRT5004;
            pType1ChipUserConnection->nLowMemSize = P_TYPE1_CHIP_IRT5004_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;
            pType1ChipUserConnection->nHighMemSize = P_TYPE1_CHIP_IRT5004_HIGHMEM_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;

            pLowMemDescriptor = & g_sLowMem4CacheDescriptor;
            pHighMemDescriptor = & g_sHighMem4CacheDescriptor;

            break;

         case P_TYPE1_CHIP_IRT5005_HR0:
            PDebugTrace("PType1ChipUserCreateConnection: IRT5005 (1024 Bytes with added 8 Byte write)");

            pType1ChipUserConnection->nType1ChipVariant = P_TYPE1_CHIP_IRT5005;
            pType1ChipUserConnection->nLowMemSize = P_TYPE1_CHIP_IRT5005_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;
            pType1ChipUserConnection->nHighMemSize = P_TYPE1_CHIP_IRT5005_HIGHMEM_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;

            pLowMemDescriptor = & g_sLowMemCacheDescriptor;
            pHighMemDescriptor = & g_sHighMem8CacheDescriptor;
            break;

         case P_TYPE1_CHIP_IRT5006_HR0:
            PDebugTrace("PType1ChipUserCreateConnection: IRT5006 (2048 Bytes)");

            pType1ChipUserConnection->nType1ChipVariant = P_TYPE1_CHIP_IRT5006;
            pType1ChipUserConnection->nLowMemSize = P_TYPE1_CHIP_IRT5006_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;
            pType1ChipUserConnection->nHighMemSize = P_TYPE1_CHIP_IRT5006_HIGHMEM_BLOCK_NB * P_TYPE1_CHIP_BLOCK_SIZE;

            pLowMemDescriptor = & g_sLowMemCacheDescriptor;
            pHighMemDescriptor = & g_sHighMem8CacheDescriptor;
            break;

         default:

            PDebugError("PType1ChipUserCreateConnection: unknown type HR0=0x%02X HR1=0x%02X",
                              pType1ChipUserConnection->aHeaderRom[0], pType1ChipUserConnection->aHeaderRom[1]);
            nError = W_ERROR_BAD_TAG_FORMAT;
            goto return_error;
      }
   }

   /* Creates the smart caches */

   nError = PSmartCacheCreateCache(pContext, &pType1ChipUserConnection->sSmartCache,
               pType1ChipUserConnection->nLowMemSize, pLowMemDescriptor, pType1ChipUserConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PType1ChipUserCreateConnection : PSmartCacheCreateCache() returned %s\n", PUtilTraceError(nError));
      goto return_error;
   }

   if (pType1ChipUserConnection->nHighMemSize != 0)
   {

      nError = PSmartCacheCreateCache(pContext, &pType1ChipUserConnection->sSmartCacheHighMem,
                  pType1ChipUserConnection->nHighMemSize, pHighMemDescriptor, pType1ChipUserConnection);

      if (nError != W_SUCCESS)
      {
         PDebugError("PType1ChipUserCreateConnection : PSmartCacheCreateCache(highmem) returned %s\n", PUtilTraceError(nError));
         goto return_error;
      }
   }

   /* initialize the lock arrays */

   pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_LOW_MEM].nOffset = P_TYPE1_LOCK_0_OFFSET;
   pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_LOW_MEM].nLength = 2;

      /* Block 0x00 - Block 0x0D */

   for (i=0; i<=0x0D; i++)
   {
      pType1ChipUserConnection->asBlockLock[i].nLockId  = P_TYPE1_LOCK_ID_LOW_MEM;
      pType1ChipUserConnection->asBlockLock[i].nLockBit = (uint16_t) i;
   }
      /* Block 0x0E */

   pType1ChipUserConnection->asBlockLock[0x0E].nLockId = P_TYPE1_LOCK_ID_NONE;   /* non lockable */

   switch (pType1ChipUserConnection->nType1ChipVariant)
   {
      case P_TYPE1_CHIP_IRT5003 :

         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset = P_TYPE1_LOCK_2_OFFSET;
         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nLength = 2;

         /* Block 0x0F */
         pType1ChipUserConnection->asBlockLock[0x0F].nLockId = P_TYPE1_LOCK_ID_NONE;      /* non lockable */

         /* Block 0x10 - 0x1B */
         for (i = 0; i < P_TYPE1_CHIP_IRT5003_HIGHMEM_BLOCK_NB; i++)
         {
            pType1ChipUserConnection->asBlockLock[0x10 + i].nLockId = P_TYPE1_LOCK_ID_HIGH_MEM;
            pType1ChipUserConnection->asBlockLock[0x10 + i].nLockBit = (uint16_t) i;
         }

         break;

      case P_TYPE1_CHIP_IRT5004 :

         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset = P_TYPE1_LOCK_2_OFFSET;
         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nLength = 5;

         /* Block 0x0F */
         pType1ChipUserConnection->asBlockLock[0x0F].nLockId = P_TYPE1_LOCK_ID_NONE;      /* non lockable */

         /* Block 0x10 - 0x33 */
         for (i = 0; i < P_TYPE1_CHIP_IRT5004_HIGHMEM_BLOCK_NB; i++)
         {
            pType1ChipUserConnection->asBlockLock[0x10 + i].nLockId = P_TYPE1_LOCK_ID_HIGH_MEM;
            pType1ChipUserConnection->asBlockLock[0x10 + i].nLockBit = (uint16_t) i;
         }
         break;

      case P_TYPE1_CHIP_IRT5005 :

         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset = P_TYPE1_LOCK_8_OFFSET;
         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nLength = 1;

         /* Block 0x0F */
         pType1ChipUserConnection->asBlockLock[0x0F].nLockId = P_TYPE1_LOCK_ID_NONE;      /* non lockable */

         /* Upper Blocks 0x10 - 0x7F */
         for (i = 0; i < P_TYPE1_CHIP_IRT5005_HIGHMEM_BLOCK_NB; i++)
         {
            pType1ChipUserConnection->asBlockLock[0x10 + i].nLockId = P_TYPE1_LOCK_ID_HIGH_MEM;
            pType1ChipUserConnection->asBlockLock[0x10 + i].nLockBit = (uint16_t) (i / 16);
         }

         break;

      case P_TYPE1_CHIP_IRT5006 :

         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset = P_TYPE1_LOCK_8_OFFSET;
         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nLength = 2;

         /* Block 0x0F */
         pType1ChipUserConnection->asBlockLock[0x0F].nLockId = P_TYPE1_LOCK_ID_NONE;      /* non lockable */

         /* Upper Blocks 0x10 - 0xFF */
         for (i = 0; i < P_TYPE1_CHIP_IRT5006_HIGHMEM_BLOCK_NB; i++)
         {
            pType1ChipUserConnection->asBlockLock[0x10 + i].nLockId = P_TYPE1_LOCK_ID_HIGH_MEM;
            pType1ChipUserConnection->asBlockLock[0x10 + i].nLockBit = (uint16_t) (i/16);
         }
         break;


      case P_TYPE1_TAG_DYNAMIC :

         /* specific case for the Topaz 512 */

         if (pType1ChipUserConnection->aHeaderRom[0] == P_TYPE1_TAG_TOPAZ_512_HR0)
         {
            /* 512 bytes capacity */

            pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset = 122;
            pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nLength = 6;

            pType1ChipUserConnection->asBlockLock[0x0F].nLockId = P_TYPE1_LOCK_ID_NONE;      /* non lockable */

            /* upper blocks 0x10 - 0x3F */
            for (i = 0; i < P_TYPE1_TAG_TOPAZ_512_HIGHMEM_BLOCK_NB; i++)
            {
               pType1ChipUserConnection->asBlockLock[0x10 + i].nLockId = P_TYPE1_LOCK_ID_HIGH_MEM;
               pType1ChipUserConnection->asBlockLock[0x10 + i].nLockBit = (uint16_t) i;
            }
         }
         break;
   }

   /* Add the Type1Chip connection structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hUserConnection,
                     pType1ChipUserConnection,
                     P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("PType1ChipUserCreateConnection: could not add the Type1Chip buffer");
      goto return_error;
   }

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PType1ChipUserCreationCompleted() when the operation is completed */
   PHandleIncrementReferenceCount(pType1ChipUserConnection);

   /* Force read of the low memory area of the tag */
   PSmartCacheRead(pContext, &pType1ChipUserConnection->sSmartCache, 0, pType1ChipUserConnection->nLowMemSize, null, static_PType1ChipReadLowMemCompleted, pType1ChipUserConnection);

   return;

return_error:

   PDebugError("PType1ChipUserCreateConnection: returning error %s", PUtilTraceError(nError));

   /* Free the allocated resources if any */
   if (pType1ChipUserConnection != null)
   {

      PSmartCacheDestroyCache(pContext, &pType1ChipUserConnection->sSmartCache);
      PSmartCacheDestroyCache(pContext, &pType1ChipUserConnection->sSmartCacheHighMem);

      CMemoryFree(pType1ChipUserConnection);
   }

   PDFCPostContext2( &sCallbackContext, nError );
}

/*
 * @biref Called when the low memory of the Type1 chip/tag have been read
 *
 * @param[in] pContext              The context.
 * @param[in] pCallbackParameter    The callback parameter (-> pType1ChipUserConnection)
 * @param[in] nError                The result
 *
 * @return    void
 */

void static_PType1ChipReadLowMemCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   tPType1ChipUserConnection * pType1ChipUserConnection = (tPType1ChipUserConnection *) pCallbackParameter ;

   PDebugTrace("static_PType1ChipReadLowMemCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PType1ChipAtomicReadLowMemCompleted : read failed %s!!!", PUtilTraceError(nError));
      goto error;
   }

   /* Retrieve a pointer on the low memory in the cache */

   pType1ChipUserConnection->pLowMemCache = PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCache,  0, pType1ChipUserConnection->nLowMemSize);

   if (pType1ChipUserConnection->pLowMemCache == null)
   {
      /* should not occur */
      PDebugError("static_PType1ChipAtomicReadLowMemCompleted : unable to access to the low memory !!!");
      nError = W_ERROR_BAD_STATE;
      goto error;
   }

   /* Copy the UID in the context */

   CMemoryCopy(pType1ChipUserConnection->aUID, pType1ChipUserConnection->pLowMemCache, P_TYPE1_CHIP_UID_LENGTH);

   /* make a snapshot of the lock */
   CMemoryCopy(pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_LOW_MEM].aBytes,
                  pType1ChipUserConnection->pLowMemCache + pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_LOW_MEM].nOffset,
                  pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_LOW_MEM].nLength);

   /* Specific processing for Type 1 tags */

   if ((pType1ChipUserConnection->nType1ChipVariant == P_TYPE1_TAG_DYNAMIC)  && (pType1ChipUserConnection->nHighMemSize == 0))
   {
      /* Check the presence of the Container Capability */

      nError = static_PType1ChipCheckContainerCapability(pContext, pType1ChipUserConnection);

      switch (nError)
      {
         case W_SUCCESS :
            break;

         case W_ERROR_BAD_TAG_FORMAT :

            PDebugError("static_PType1ChipReadLowMemCompleted : static_PType1ChipCheckContainerCapability failed -> limit access to low memory");

            /* Maybe the tag is not formated. Limit the access to the low memory */
            pType1ChipUserConnection->nHighMemSize = 0;
            break;

         default:

            PDebugError("static_PType1ChipReadLowMemCompleted : static_PType1ChipCheckContainerCapability failed");
            /* unexpected error */
            goto error;
      }

      if (nError == W_SUCCESS)
      {
         /* Check the presence of the Lock and Reserved memory descriptors */

         nError = static_PType1ChipGetMemoryAndLockControlTLV(pContext, pType1ChipUserConnection);

         switch (nError)
         {
            case W_SUCCESS :
               break;

            case W_ERROR_BAD_TAG_FORMAT :

               PDebugError("static_PType1ChipReadLowMemCompleted : static_PType1ChipGetMemoryAndLockControlTLV failed > limit access to low memory");

               /* Maybe the tag is not formated. Limit the access to the low memory */
               pType1ChipUserConnection->nHighMemSize = 0;
               break;

            default:
               PDebugError("static_PType1ChipReadLowMemCompleted : static_PType1ChipGetMemoryAndLockControlTLV failed");               PDebugError("static_PType1ChipReadLowMemCompleted : static_PType1ChipCheckContainerCapability failed");
               /* unexpected error */
               goto error;
         }

         /* now we know the size of the high mem area for dynamic tags */

         if (pType1ChipUserConnection->nHighMemSize != 0)
         {
            nError = PSmartCacheCreateCache(pContext, &pType1ChipUserConnection->sSmartCacheHighMem,
                     pType1ChipUserConnection->nHighMemSize, &g_sDynamicTagCacheDescriptorHighMem, pType1ChipUserConnection);

            if (nError != W_SUCCESS)
            {
               PDebugError("static_PType1ChipReadLowMemCompleted : PSmartCacheCreateCache(highmem) returned %s\n", PUtilTraceError(nError));
               goto error;
            }
         }
      }
   }

   /* read the high memory of the tag */

   if (pType1ChipUserConnection->nHighMemSize != 0)
   {
      PSmartCacheRead(pContext, &pType1ChipUserConnection->sSmartCacheHighMem, 0 /* put only data in the cache, no buffer */,
                        pType1ChipUserConnection->nHighMemSize, null, static_PType1ChipReadHighMemCompleted, pType1ChipUserConnection);
   }
   else
   {
      /* All work is done */
      static_PType1ChipUserCreationCompleted(pContext, pType1ChipUserConnection, W_SUCCESS);
   }

   return;

error:

   /* something goes wrong */
   static_PType1ChipUserCreationCompleted(pContext, pType1ChipUserConnection, nError);

#if 0 /* @todo */
   PSmartCacheDestroyCache(pContext, &pType1ChipUserConnection->sSmartCache);
   PSmartCacheDestroyCache(pContext, &pType1ChipUserConnection->sSmartCacheHighMem);
   CMemoryFree(pType1ChipUserConnection);
#endif /* 0 */
}

/*
 * @brief called when the High memory of the Type1 chip have been read
 *
 * @param[in] pContext              The context.
 * @param[in] pCallbackParameter    The callback parameter (-> pType1ChipUserConnection)
 * @param[in] nError                The result
 *
 * @return    void
 */


void static_PType1ChipReadHighMemCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   tPType1ChipUserConnection * pType1ChipUserConnection = (tPType1ChipUserConnection *) pCallbackParameter ;

   PDebugTrace("static_PType1ChipReadHighMemCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PType1ChipReadHighMemCompleted : read failed %s!!!", PUtilTraceError(nError));
      goto error;
   }

   pType1ChipUserConnection->pHighMemCache =
        PSmartCacheGetBuffer(pContext, &pType1ChipUserConnection->sSmartCacheHighMem, 0, pType1ChipUserConnection->nHighMemSize);

   if (pType1ChipUserConnection->pHighMemCache == null)
   {
      /* should not occur */
      PDebugError("static_PType1ChipReadHighMemCompleted : unable to access to the low memory !!!");
      nError = W_ERROR_BAD_STATE;
      goto error;
   }

   if (pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset < pType1ChipUserConnection->nLowMemSize)
   {
      /* lock is located in low memory */

      CMemoryCopy(pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].aBytes,
         pType1ChipUserConnection->pLowMemCache + pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset,
         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nLength);
   }
   else
   {
      /* lock is located in high memory */

      CMemoryCopy(pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].aBytes,
         pType1ChipUserConnection->pHighMemCache + pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset - pType1ChipUserConnection->nLowMemSize,
         pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nLength);
   }

   static_PType1ChipUserCreationCompleted(pContext, pType1ChipUserConnection, W_SUCCESS);
   return;

error:
   /* something goes wrong */

   static_PType1ChipUserCreationCompleted(pContext, pType1ChipUserConnection, nError);

#if 0 /* @todo */
   PSmartCacheDestroyCache(pContext, &pType1ChipUserConnection->sSmartCache);
   PSmartCacheDestroyCache(pContext, &pType1ChipUserConnection->sSmartCacheHighMem);
   CMemoryFree(pType1ChipUserConnection);
#endif /* @todo */
}

/**
 *  @brief Checks the contents of the Container Capability that must be present at the start of the data area
 *          (first four bytes) of a Type 1 TAG
 *
 *  @param[in] pContext  The context.
 *
 *  @param[in] pType1ChipUserConnection  The Type1 chip connection
 *
 *  @return W_SUCCESS, W_ERROR_BAD_STATE, W_ERROR_BAD_TAG_FORMAT
 */

W_ERROR static_PType1ChipCheckContainerCapability(
   tContext * pContext,
   tPType1ChipUserConnection   * pType1ChipUserConnection
   )
{
   uint8_t   nReadWriteAccess;
   uint32_t   nTagSize;

   PDebugTrace("static_PType1ChipCheckContainerCapability");

   /* Retrieve a pointer on the low memory in the cache */


   /* CC VNO */

   if (pType1ChipUserConnection->pLowMemCache[P_TYPE1_CHIP_NDEF_TAG_CC_VNO_OFFSET] != 0x10)
   {
      PDebugError("static_PType1ChipCheckContainerCapability : Tag with an incompatible version ???");
      return (W_ERROR_BAD_TAG_FORMAT);
   }

   /* CC RWA */

   nReadWriteAccess = pType1ChipUserConnection->pLowMemCache[P_TYPE1_CHIP_NDEF_TAG_CC_RWA_OFFSET];

   if ((nReadWriteAccess != 0x00) &&(nReadWriteAccess != 0x0F))
   {
      PDebugError("static_PType1ChipCheckContainerCapability : invalid RW access value ????");

      return (W_ERROR_BAD_TAG_FORMAT);
   }

   /* CC TMS */

   nTagSize = pType1ChipUserConnection->pLowMemCache[P_TYPE1_CHIP_NDEF_TAG_CC_TMS_OFFSET];

   if (nTagSize <= 0x0E)
   {
      PDebugError("static_PType1ChipCheckContainerCapability : Dynamic tag with a size <= 120 ???");
      return (W_ERROR_BAD_TAG_FORMAT);
   }

   /* update the size of the high memory according to the tag size */
   pType1ChipUserConnection->nHighMemSize = (nTagSize + 1) * P_TYPE1_CHIP_BLOCK_SIZE - pType1ChipUserConnection->nLowMemSize;

   return (W_SUCCESS);
}

/*
 *  @brief Looks for Reserved memory and Lock Control TLV that must be present in the data area of a Type 1 Tag.
 *          We only search in the low memory since we only know the mapping of this memory at this time.
 *
 * @param[in] pContext The context
 *
 *  @param[in] pType1ChipUserConnection  The Type1 chip connection
 *
 * @return W_SUCCESS, W_ERROR_BAD_STATE, W_ERROR_BAD_TAG_FORMAT
 */

W_ERROR static_PType1ChipGetMemoryAndLockControlTLV(
   tContext * pContext,
   tPType1ChipUserConnection   * pType1ChipUserConnection
   )
{
   bool_t bIsReservedMemoryControlBlockFound = W_FALSE;
   bool_t bGoOn;
   const uint8_t * pBuffer;
   uint32_t nCurrentBlockDescr;
   uint16_t nIndex;
   uint8_t  nValue;
   W_ERROR  nError;
   uint32_t nPageAddr;
   uint32_t nByteOffset;
   uint32_t nSize;
   uint32_t nBytesPerPage;
   uint32_t nBytesLockedPerLockBit;
   uint32_t nByteAddr;
   uint32_t i,j;
   uint16_t nTlvSize;

   PDebugTrace("static_PType1ChipGetMemoryAndLockControlTLV");

   pBuffer = pType1ChipUserConnection->pLowMemCache;

   /* Walk through the low memomry searching for block descriptor TLV */

   nIndex = P_TYPE1_CHIP_NDEF_TAG_TLV_START_OFFSET;
   nCurrentBlockDescr = 0x10;        /* First block of the dynamic memory */

   nValue = pBuffer[nIndex];

   do
   {
      bGoOn = W_TRUE;

      switch (nValue)
      {
         /* null TLV */
         case P_TYPE1_CHIP_NDEF_NULL_TLV:

            /* skip the NULL TLV */
            /* PDebugTrace("static_PType1ChipGetMemoryAndLockControlTLV: NULL"); */
            nIndex ++;
            break;

         /* Lock Control */
         case P_TYPE1_CHIP_NDEF_LOCK_CONTROL_TLV_TYPE:

            PDebugTrace("static_PType1ChipGetMemoryAndLockControlTLV: Lock Control");

            if (pBuffer[nIndex + 1] != P_TYPE1_CHIP_NDEF_LOCK_CONTROL_TLV_LENGTH)
            {
               PDebugError("static_PType1ChipGetMemoryAndLockControlTLV: wrong length");
               nError = W_ERROR_BAD_TAG_FORMAT;
               goto error;
            }

            nPageAddr   = (pBuffer[nIndex + 2] >> 4) & 0x0F;
            nByteOffset = pBuffer[nIndex + 2] & 0x0F;

            nSize       = pBuffer[nIndex + 3];
            if (nSize == 0) nSize = 256;

            nBytesPerPage = pBuffer[nIndex + 4] & 0x0F;
            if (nBytesPerPage == 0)
            {
               PDebugError("static_PType1ChipGetMemoryAndLockControlTLV: nBytesPerPage = 0 is RFU");
               nError = W_ERROR_BAD_TAG_FORMAT;
               goto error;
            }

            nBytesLockedPerLockBit = (pBuffer[nIndex + 4] >> 4) & 0xFF;
            nBytesLockedPerLockBit = 1 << nBytesLockedPerLockBit;

            /* we only support lock granularity multiple of 8 bytes for now */
            /** @todo */

            if ((nBytesLockedPerLockBit & 7) !=0)
            {
               PDebugError("static_PType1ChipGetMemoryAndLockControlTLV : unsupported lock granularity\n");
               nError = W_ERROR_BAD_TAG_FORMAT;
               goto error;
            }

            nByteAddr = nPageAddr * (1<<nBytesPerPage) + nByteOffset;

            pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nOffset = (uint16_t) nByteAddr;
            pType1ChipUserConnection->asMemLock[P_TYPE1_LOCK_ID_HIGH_MEM].nLength = (uint16_t) (nSize / 8);

            for (i = 0; i<nSize; i++)
            {
               for (j=0; j<nBytesLockedPerLockBit / 8; j++)
               {
                  pType1ChipUserConnection->asBlockLock[nCurrentBlockDescr].nLockId  = P_TYPE1_LOCK_ID_HIGH_MEM;
                  pType1ChipUserConnection->asBlockLock[nCurrentBlockDescr].nLockBit = (uint16_t) i;

                  nCurrentBlockDescr++;
               }
            }

            /* the current TLV has been processed, go to the next one */
            nIndex += 5;
            break;

         case P_TYPE1_CHIP_NDEF_RESERVED_MEMORY_TLV_TYPE :

            PDebugTrace("static_PType1ChipGetMemoryAndLockControlTLV: Reserved memory");

            if (pBuffer[nIndex + 1] != P_TYPE1_CHIP_NDEF_LOCK_CONTROL_TLV_LENGTH)
            {
               PDebugError("static_PType1ChipGetMemoryAndLockControlTLV: wrong length");
               nError = W_ERROR_BAD_TAG_FORMAT;
               goto error;
            }

            bIsReservedMemoryControlBlockFound = W_TRUE;

            /* the current TLV has been processed, go to the next one */
            nIndex += 5;

            break;


         case P_TYPE1_CHIP_NDEF_MESSAGE_TLV_TYPE :
         case P_TYPE1_CHIP_NDEF_PROPRIETARY_TLV_TYPE :
         case P_TYPE1_CHIP_NDEF_TERMINATOR_TLV_TYPE :

            /* These TLV must appear after the Lock control and reserved memory descriptors,
               so if we found it, the processing is terminated */
            bGoOn = W_FALSE;
            break;

         default :

            /* Other TLVs, skip them */
            if (pBuffer[nIndex+1] != 0xFF)
            {
               nTlvSize = pBuffer[nIndex+1];
            } else
            {
               nTlvSize = (pBuffer[nIndex+1] << 8) + pBuffer[nIndex+2];
            }

            nIndex += 2 + nTlvSize;
            break;
      }

      nValue = pBuffer[nIndex];

   } while ((bGoOn != W_FALSE) && (nIndex < (0x0D*8)));

   if (bIsReservedMemoryControlBlockFound == W_FALSE)
   {
      PDebugError("static_PType1ChipGetMemoryAndLockControlTLV: Control Block must be present in a TAG !");
      nError = W_ERROR_BAD_TAG_FORMAT;
      goto error;
   }

   return (W_SUCCESS);

error :

   return nError;
}


/* See Header file */
W_ERROR PType1ChipUserCheckType1(
            tContext* pContext,
            W_HANDLE hUserConnection,
            uint32_t* pnMaximumSpaceSize,
            uint8_t *pnSectorSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable )
{
   tPType1ChipUserConnection* pType1ChipUserConnection;
   W_ERROR nError;

   PDebugTrace("PType1ChipUserCheckType1");

   nError = PReaderUserGetConnectionObject(pContext, hUserConnection,
      P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection);

   if ( nError == W_SUCCESS )
   {
      /* Check the card type */
      if((pType1ChipUserConnection->aHeaderRom[0] & 0xF0) != 0x10)
      {
         PDebugLog("PType1ChipUserCheckType1: Not a NFC Type 1");
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }

      *pnSectorSize = P_TYPE1_CHIP_BLOCK_SIZE;
      *pnMaximumSpaceSize = pType1ChipUserConnection->nLowMemSize + pType1ChipUserConnection->nHighMemSize;

      if (PType1ChipIsWritable(pContext, hUserConnection) == W_SUCCESS)
      {
         *pbIsLocked = W_FALSE;
         *pbIsLockable = W_TRUE;
      }
      else
      {
         *pbIsLocked = W_TRUE;
         *pbIsLockable = W_FALSE;
      }

      /* we only support format of TAG using static memory model
         or Topaz 512 */

      if ((pType1ChipUserConnection->aHeaderRom[0] == 0x11) ||
          (pType1ChipUserConnection->aHeaderRom[0] == 0x12))
      {
         * pbIsFormattable = (* pbIsLocked == W_FALSE) ? W_TRUE : W_FALSE;
      }
      else
      {
         * pbIsFormattable = W_FALSE;
      }

      return W_SUCCESS;
   }
   else
   {
      PDebugError("PType1ChipUserCheckType1: could not get pType1ChipUserConnection buffer");
      return nError;
   }
}

static uint32_t static_PType1ChipGetIdentifierLength(
         tContext* pContext,
         void* pObject)
{
   return P_TYPE1_CHIP_UID_LENGTH;
}

/* Get identifier */
static void static_PType1ChipGetIdentifier(
         tContext* pContext,
         void* pObject,
         uint8_t* pIdentifierBuffer)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*)pObject;

   CMemoryCopy(pIdentifierBuffer, pType1ChipUserConnection->aUID, P_TYPE1_CHIP_UID_LENGTH);
}

/* See Client API Specifications */
W_ERROR PType1ChipGetConnectionInfo(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tWType1ChipConnectionInfo *pConnectionInfo )
{
   tPType1ChipUserConnection* pType1ChipUserConnection;
   W_ERROR                    nError;

   PDebugTrace("PType1ChipGetConnectionInfo");

   nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("PType1ChipGetConnectionInfo: could not get pType1ChipUserConnection buffer");
      goto error_cleanup;
   }

   if ( pConnectionInfo == null )
   {
      PDebugError("PType1ChipGetConnectionInfo: pConnectionInfo == null");
      nError = W_ERROR_BAD_PARAMETER;
      goto error_cleanup;
   }

   /*
    * all is fine
    */

   /* UID */
   CMemoryCopy(pConnectionInfo->UID, pType1ChipUserConnection->aUID, P_TYPE1_CHIP_UID_LENGTH );
   /* Sector size */
   pConnectionInfo->nBlockSize = P_TYPE1_CHIP_BLOCK_SIZE;
   /* Sector number */
   pConnectionInfo->nBlockNumber = (uint16_t)(pType1ChipUserConnection->nLowMemSize + pType1ChipUserConnection->nHighMemSize) / P_TYPE1_CHIP_BLOCK_SIZE;

   CMemoryCopy(
      pConnectionInfo->aHeaderRom,
      pType1ChipUserConnection->aHeaderRom,
      2 );


   return (W_SUCCESS);

error_cleanup :

   if (pConnectionInfo != null)
   {
      /* Fill in the structure with zeros with zero */
      CMemoryFill(pConnectionInfo, 0, sizeof(*pConnectionInfo));
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR PType1ChipIsWritable(
            tContext* pContext,
            W_HANDLE hUserConnection )
{
   tPType1ChipUserConnection* pType1ChipUserConnection;
   W_ERROR                    nError;
   uint16_t                   i;
   uint16_t                   nLockId;
   uint16_t                   nLockBit;
   uint8_t                    nCurrentLockValue;

   PDebugTrace("PType1ChipIsWritable");

   nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PType1ChipIsWritable: could not get pType1ChipUserConnection buffer");
      goto error_cleanup;
   }

   /*
    * all is fine
    */

   for (i=0; i<(pType1ChipUserConnection->nLowMemSize+pType1ChipUserConnection->nHighMemSize)/P_TYPE1_CHIP_BLOCK_SIZE; i++)
   {
      nLockId  = pType1ChipUserConnection->asBlockLock[i].nLockId;
      nLockBit = pType1ChipUserConnection->asBlockLock[i].nLockBit;

      if (nLockId < P_TYPE1_CHIP_NB_MEM_BLOCKS)
      {
         /* Check if the block is not already locked */

         nCurrentLockValue = pType1ChipUserConnection->asMemLock[nLockId].aBytes[nLockBit/8];

         if ((nCurrentLockValue & (1 << (nLockBit & 7))) == 0)
         {
            return (W_SUCCESS);
         }
      }
   }

   return(W_ERROR_ITEM_LOCKED);

error_cleanup:

   return nError;
}

/* Compute offsets and lengths of low and high memories */
static void static_PType1ChipComputeReadWriteParameters(
            uint32_t nLowMemSize,
            uint32_t nHighMemSize,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockBlocks,
            uint16_t* nLowMemOffset,
            uint16_t* nLowMemLength,
            uint16_t* nHighMemOffset,
            uint16_t* nHighMemLength)
{
   *nLowMemOffset  = 0;
   *nLowMemLength  = 0;
   *nHighMemOffset = 0;
   *nHighMemLength = 0;

   if ((bLockBlocks != W_FALSE) && (nOffset == 0) && (nLength == 0))
   {
      /* specific case meaning entire tag lock */
      *nLowMemLength = (uint16_t) nLowMemSize;
      *nHighMemLength = (uint16_t) nHighMemSize;
   }
   else
   {
      if (nOffset < nLowMemSize)
      {
         *nLowMemOffset = (uint16_t) nOffset;

         if ((nOffset + nLength) <= nLowMemSize)
         {
            *nLowMemLength = (uint16_t) nLength;
         }
         else
         {
            *nLowMemLength = (uint16_t) (nLowMemSize - nOffset);

            *nHighMemOffset = 0;
            *nHighMemLength = (uint16_t) (nLength - *nLowMemLength);
         }
      }
      else
      {
         *nHighMemOffset  = (uint16_t) (nOffset - nLowMemSize);
         *nHighMemLength = (uint16_t) nLength;
      }
   }
}

/**
 * @brief Called when the high memory has been read
 *
 * @param[in] pContext The context.
 *
 * @param[in] pCallbackParameter The callback parameter, e.g. the Type1 connection context
 *
 * @param[in] nError The result of the operation
 */

static void static_PType1ChipReadAutomatonHighMemCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection *) pCallbackParameter;

   PDebugTrace("static_PType1ChipReadAutomatonHighMemCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PType1ChipReadAutomatonHighMemCompleted %s", PUtilTraceError(nError));
   }

   static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, nError);
}

/**
 * @brief Called when the low memory has been read
 *
 * @param[in] pContext The context.
 *
 * @param[in] pCallbackParameter The callback parameter, e.g. the Type1 connection context
 *
 * @param[in] nError The result of the operation
 */

static void static_PType1ChipReadAutomatonLowMemCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection *) pCallbackParameter;

   PDebugTrace("static_PType1ChipReadAutomatonLowMemCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PType1ChipReadAutomatonLowMemCompleted %s", PUtilTraceError(nError));

      static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, nError);
      return;
   }

   /* the low memory has been written read */

   if (pType1ChipUserConnection->sReadWriteOperation.nHighMemLength != 0)
   {
      /* read the upper blocks if any */
      PSmartCacheRead(pContext, &pType1ChipUserConnection->sSmartCacheHighMem,
            pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset, pType1ChipUserConnection->sReadWriteOperation.nHighMemLength,
            pType1ChipUserConnection->sReadWriteOperation.pBuffer + pType1ChipUserConnection->sReadWriteOperation.nLowMemLength,
            (tPBasicGenericCallbackFunction *) static_PType1ChipReadAutomatonHighMemCompleted, pType1ChipUserConnection);
   }
   else
   {
      /* operation is now complete */
      static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, W_SUCCESS);
   }
}

static void static_PType1ChipReadInternalEx(
            tContext* pContext,
            tPType1ChipUserConnection* pType1ChipUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength)
{
   tDFCCallbackContext        sCallbackContext;
   W_ERROR                    nError;

   PDebugTrace("static_PType1ChipReadInternalEx");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check connection object */
   if (pType1ChipUserConnection == null)
   {
      PDebugError("static_PType1ChipReadInternalEx: null handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   if ((pBuffer == null) || (nLength == 0))
   {
      PDebugError("static_PType1ChipReadInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the length */
   if ((nOffset + nLength) > (pType1ChipUserConnection->nLowMemSize + pType1ChipUserConnection->nHighMemSize))
   {
      PDebugError("static_PType1ChipReadInternalEx: the data to read is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Read */
   pType1ChipUserConnection->sReadWriteOperation.pBuffer = pBuffer;
   pType1ChipUserConnection->sReadWriteOperation.bLockBlocks = W_FALSE;
   static_PType1ChipComputeReadWriteParameters(
            pType1ChipUserConnection->nLowMemSize,
            pType1ChipUserConnection->nHighMemSize,
            nOffset,
            nLength,
            W_FALSE,
            &pType1ChipUserConnection->sReadWriteOperation.nLowMemOffset,
            &pType1ChipUserConnection->sReadWriteOperation.nLowMemLength,
            &pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset,
            &pType1ChipUserConnection->sReadWriteOperation.nHighMemLength);

   /* Store the callback context */
   pType1ChipUserConnection->sCallbackContextInternal = sCallbackContext;

   /* Increment the reference count to keep the connection object alive during the operation.
      The reference count is decreased in static_PType1ChipInternalOperationCompleted() when the operation is completed */
   PHandleIncrementReferenceCount(pType1ChipUserConnection);

   if (pType1ChipUserConnection->sReadWriteOperation.nLowMemLength != 0)
   {
      PSmartCacheRead(pContext, &pType1ChipUserConnection->sSmartCache,
         pType1ChipUserConnection->sReadWriteOperation.nLowMemOffset, pType1ChipUserConnection->sReadWriteOperation.nLowMemLength,  pBuffer,
         (tPBasicGenericCallbackFunction *) static_PType1ChipReadAutomatonLowMemCompleted, pType1ChipUserConnection);
   }
   else
   {
      PSmartCacheRead(pContext, &pType1ChipUserConnection->sSmartCacheHighMem,
         pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset, pType1ChipUserConnection->sReadWriteOperation.nHighMemLength,  pBuffer,
         (tPBasicGenericCallbackFunction *) static_PType1ChipReadAutomatonHighMemCompleted, pType1ChipUserConnection);
   }

   return;

return_error:
   PDebugError("static_PType1ChipReadInternalEx: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See header */
void PType1ChipReadInternal(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = null;

   /* Check if the connection handle is valid */
   if (PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection) != W_SUCCESS)
   {
      pType1ChipUserConnection = null;
   }

   static_PType1ChipReadInternalEx(
         pContext,
         pType1ChipUserConnection,
         pCallback, pCallbackParameter,
         pBuffer,
         nOffset,
         nLength);
}

/* See Client API Specifications */
void PType1ChipRead(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            W_HANDLE *phOperation )
{
   tPType1ChipUserConnection* pType1ChipUserConnection = null;
   tDFCCallbackContext        sCallbackContext;
   W_ERROR                    nError;
   W_HANDLE                   hOperation = W_NULL_HANDLE;

   PDebugTrace("PType1ChipRead [%d - %d]", nOffset, nOffset + nLength - 1);

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PType1ChipRead: could not get pType1ChipUserConnection");
      goto return_error;
   }

   if ((pBuffer == null) || (nLength == 0))
   {
      PDebugError("PType1ChipRead: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the length */
   if ((nOffset + nLength) > (pType1ChipUserConnection->nLowMemSize + pType1ChipUserConnection->nHighMemSize))
   {
      PDebugError("PType1ChipRead: the data to read is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, static_PType1ReadOrWriteCancel, pType1ChipUserConnection)) == W_NULL_HANDLE)
      {
         PDebugError("PType1ChipRead: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PType1ChipRead: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hUserConnection, static_PType1ChipExecuteQueuedExchange, pType1ChipUserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PType1ChipUserCommandCompleted() when the operation is completed */
      PHandleIncrementReferenceCount(pType1ChipUserConnection);

      /* Store the operation handle */
      CDebugAssert(pType1ChipUserConnection->hOperation == W_NULL_HANDLE);
      pType1ChipUserConnection->hOperation = hOperation;

      /* Store the callback context */
      pType1ChipUserConnection->sCallbackContext = sCallbackContext;

      /* Read */
      static_PType1ChipReadInternalEx(
            pContext,
            pType1ChipUserConnection,
            static_PType1ChipUserCommandCompleted, pType1ChipUserConnection,
            pBuffer,
            nOffset,
            nLength);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PType1ChipUserCommandCompleted() when the operation is completed */
      PHandleIncrementReferenceCount(pType1ChipUserConnection);

      /* Save the operation handle */
      CDebugAssert(pType1ChipUserConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pType1ChipUserConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pType1ChipUserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pType1ChipUserConnection->sQueuedOperation.nType = P_TYPE1_QUEUED_READ;

      /* Save data */
      pType1ChipUserConnection->sQueuedOperation.pBuffer = pBuffer;
      pType1ChipUserConnection->sQueuedOperation.nOffset = nOffset;
      pType1ChipUserConnection->sQueuedOperation.nLength = nLength;
      pType1ChipUserConnection->sQueuedOperation.bLockBlocks = W_FALSE;

      return;

   default:
      /* Return this error */
      if(hOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hOperation);
      }

      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("PType1ChipRead: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/*
 * @brief Cancels the current read operation
 *
 * @param[in] pContext The context
 *
 * @param[in] pCancelParameter The cancel parameter, e.g pType1ChipUserConnection
 *
 * @param[in] bIsClosing
 */

void PType1ChipReadCancel(tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*) pCancelParameter;

   PDebugTrace("PType1ChipReadCancel");

   if (pType1ChipUserConnection->hDriverOperation != W_NULL_HANDLE)
   {
      PBasicCancelOperation(pContext, pType1ChipUserConnection->hDriverOperation);
      PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
      pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;
   }
}

static void static_PType1ChipWriteInternalEx(
            tContext* pContext,
            tPType1ChipUserConnection* pType1ChipUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockBlocks)
{
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t nIndex;
   uint16_t nLockId;
   uint16_t nLockBit;
   uint8_t nCurrentLockValue;

   PDebugTrace("static_PType1ChipWriteInternalEx");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check connection object */
   if (pType1ChipUserConnection == null)
   {
      PDebugError("static_PType1ChipWriteInternalEx: null handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockBlocks == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("static_PType1ChipWriteInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockBlocks == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("static_PType1ChipWriteInternalEx: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the length */
   if ((nOffset + nLength) > (pType1ChipUserConnection->nLowMemSize + pType1ChipUserConnection->nHighMemSize))
   {
      PDebugError("static_PType1ChipWriteInternalEx: the data to write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* check if all the memory to write is writable */
   if (nLength != 0)
   {
      nError = W_SUCCESS;

      for (nIndex = (nOffset / P_TYPE1_CHIP_BLOCK_SIZE); nIndex <= (nOffset + nLength - 1) / P_TYPE1_CHIP_BLOCK_SIZE; nIndex++)
      {
         nLockId  = pType1ChipUserConnection->asBlockLock[nIndex].nLockId;
         nLockBit  = pType1ChipUserConnection->asBlockLock[nIndex].nLockBit;

         if (nLockId < P_TYPE1_CHIP_NB_MEM_BLOCKS)
         {
            /* Check if the block is not already locked */
            nCurrentLockValue = pType1ChipUserConnection->asMemLock[nLockId].aBytes[nLockBit/8];

            if (nCurrentLockValue & (1 << (nLockBit & 7)))
            {
               /* the block is locked ! */
               nError = W_ERROR_ITEM_LOCKED;
               break;
            }
         }
         else
         {
            /* do not leave access to non lockable (e.g reserved and lock bits) */
            /* if (bLockBlocks != W_FALSE)
            { */
            PDebugError("static_PType1ChipWriteInternalEx: item not lockable");
            nError = W_ERROR_ITEM_LOCKED;
            /* } */
         }
      }

      if (nError == W_ERROR_ITEM_LOCKED)
      {
         PDebugError("static_PType1ChipWriteInternalEx: item locked");
         goto return_error;
      }
   }

   /* Write */
   pType1ChipUserConnection->sReadWriteOperation.pBuffer = (uint8_t *) pBuffer;
   pType1ChipUserConnection->sReadWriteOperation.bLockBlocks = bLockBlocks;
   static_PType1ChipComputeReadWriteParameters(
            pType1ChipUserConnection->nLowMemSize,
            pType1ChipUserConnection->nHighMemSize,
            nOffset,
            nLength,
            bLockBlocks,
            &pType1ChipUserConnection->sReadWriteOperation.nLowMemOffset,
            &pType1ChipUserConnection->sReadWriteOperation.nLowMemLength,
            &pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset,
            &pType1ChipUserConnection->sReadWriteOperation.nHighMemLength);

   /* Store the callback context */
   pType1ChipUserConnection->sCallbackContextInternal = sCallbackContext;

   /* Increment the reference count to keep the connection object alive during the operation.
      The reference count is decreased in static_PType1ChipInternalOperationCompleted() when the operation is completed */
   PHandleIncrementReferenceCount(pType1ChipUserConnection);

   if (pBuffer != null)
   {
      if (pType1ChipUserConnection->sReadWriteOperation.nLowMemLength != 0)
      {
         PSmartCacheWrite(pContext, &pType1ChipUserConnection->sSmartCache,
            pType1ChipUserConnection->sReadWriteOperation.nLowMemOffset, pType1ChipUserConnection->sReadWriteOperation.nLowMemLength,  pBuffer,
            (tPBasicGenericCallbackFunction *) static_PType1ChipWriteLowMemCompleted, pType1ChipUserConnection);
      }
      else
      {
         PSmartCacheWrite(pContext, &pType1ChipUserConnection->sSmartCacheHighMem,
            pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset, pType1ChipUserConnection->sReadWriteOperation.nHighMemLength,  pBuffer,
            (tPBasicGenericCallbackFunction *) static_PType1ChipWriteHighMemCompleted, pType1ChipUserConnection);
      }
   }
   else
   {
      /* lock only operation */
      static_PType1ChipUserLockBlock(pContext, pType1ChipUserConnection);
   }

   return;

return_error:
   PDebugError("static_PType1ChipWriteInternalEx: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See header */
void PType1ChipWriteInternal(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockBlocks)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = null;

   /* Check if the connection handle is valid */
   if (PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection) != W_SUCCESS)
   {
      pType1ChipUserConnection = null;
   }

   static_PType1ChipWriteInternalEx(
         pContext,
         pType1ChipUserConnection,
         pCallback, pCallbackParameter,
         pBuffer,
         nOffset,
         nLength,
         bLockBlocks);
}

/* See Client API Specifications */
void PType1ChipWrite(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void *pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockBlocks,
            W_HANDLE *phOperation )
{
   tPType1ChipUserConnection* pType1ChipUserConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t nIndex;
   uint16_t nLockId;
   uint16_t nLockBit;
   uint8_t nCurrentLockValue;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDebugTrace("PType1ChipWrite [%d %d]", nOffset, nOffset + nLength - 1);

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection);
   if ( nError != W_SUCCESS )
   {
      PDebugError("PType1ChipWrite: could not get pType1ChipUserConnection");
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockBlocks == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("PType1ChipWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockBlocks == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("PType1ChipWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the length */
   if ((nOffset + nLength) > (pType1ChipUserConnection->nLowMemSize + pType1ChipUserConnection->nHighMemSize))
   {
      PDebugError("PType1ChipWrite: the data to write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* check if all the memory to write is writable */
   if (nLength != 0)
   {
      nError = W_SUCCESS;

      for (nIndex = (nOffset / P_TYPE1_CHIP_BLOCK_SIZE); nIndex <= (nOffset + nLength - 1) / P_TYPE1_CHIP_BLOCK_SIZE; nIndex++)
      {
         nLockId  = pType1ChipUserConnection->asBlockLock[nIndex].nLockId;
         nLockBit  = pType1ChipUserConnection->asBlockLock[nIndex].nLockBit;

         if (nLockId < P_TYPE1_CHIP_NB_MEM_BLOCKS)
         {
            /* Check if the block is not already locked */
            nCurrentLockValue = pType1ChipUserConnection->asMemLock[nLockId].aBytes[nLockBit/8];

            if (nCurrentLockValue & (1 << (nLockBit & 7)))
            {
               /* the block is locked ! */
               nError = W_ERROR_ITEM_LOCKED;
               break;
            }
         }
         else
         {
            /* do not leave access to non lockable (e.g reserved and lock bits) */
            /* if (bLockBlocks != W_FALSE)
            { */
            PDebugError("PType1ChipWrite: item not lockable");
            nError = W_ERROR_ITEM_LOCKED;
            /* } */
         }
      }

      if (nError == W_ERROR_ITEM_LOCKED)
      {
         PDebugError("PType1ChipWrite: item locked");
         goto return_error;
      }
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, static_PType1ReadOrWriteCancel, pType1ChipUserConnection)) == W_NULL_HANDLE)
      {
         PDebugError("PType1ChipWrite: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PType1ChipWrite: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hUserConnection, static_PType1ChipExecuteQueuedExchange, pType1ChipUserConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PType1ChipUserCommandCompleted() when the operation is completed */
      PHandleIncrementReferenceCount(pType1ChipUserConnection);

      /* Store the operation handle */
      CDebugAssert(pType1ChipUserConnection->hOperation == W_NULL_HANDLE);
      pType1ChipUserConnection->hOperation = hOperation;

      /* Store the callback context */
      pType1ChipUserConnection->sCallbackContext = sCallbackContext;

      /* Write */
      static_PType1ChipWriteInternalEx(
            pContext,
            pType1ChipUserConnection,
            static_PType1ChipUserCommandCompleted, pType1ChipUserConnection,
            pBuffer,
            nOffset,
            nLength,
            bLockBlocks);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PType1ChipUserCommandCompleted() when the operation is completed */
      PHandleIncrementReferenceCount(pType1ChipUserConnection);

      /* Save the operation handle */
      CDebugAssert(pType1ChipUserConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pType1ChipUserConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pType1ChipUserConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pType1ChipUserConnection->sQueuedOperation.nType = P_TYPE1_QUEUED_WRITE;

      /* Save data */
      pType1ChipUserConnection->sQueuedOperation.pBuffer = (uint8_t*) pBuffer;
      pType1ChipUserConnection->sQueuedOperation.nOffset = nOffset;
      pType1ChipUserConnection->sQueuedOperation.nLength = nLength;
      pType1ChipUserConnection->sQueuedOperation.bLockBlocks = W_FALSE;

      return;

   default:
      /* Return this error */
      if(hOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hOperation);
      }

      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("PType1ChipWrite: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/*
 * @brief Cancels the current read / write operation
 *
 * @param[in] pContext The context
 *
 * @param[in] pCancelParameter The cancel parameter, e.g pType1ChipUserConnection
 *
 * @param[in] bIsClosing
 */

static void static_PType1ReadOrWriteCancel(tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = pCancelParameter;

   PDebugTrace("PType1ChipReadOrWriteCancel");

   if (pType1ChipUserConnection->hDriverOperation != W_NULL_HANDLE)
   {
      PBasicCancelOperation(pContext, pType1ChipUserConnection->hDriverOperation);
      PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);
      pType1ChipUserConnection->hDriverOperation = W_NULL_HANDLE;
   }
}

/**
 * @brief Called when the low memory has been written
 *
 * @param[in] pContext The context.
 *
 * @param[in] pCallbackParameter The callback parameter, e.g. the Type1 connection context
 *
 * @param[in] nError The result of the operation
 */

static void static_PType1ChipWriteLowMemCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection *) pCallbackParameter;

   PDebugTrace("static_PType1ChipWriteLowMemCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PType1ChipWriteLowMemCompleted %s", PUtilTraceError(nError));

      static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, nError);
      return;
   }

   /* the low memory has been written successfully */

   if (pType1ChipUserConnection->sReadWriteOperation.nHighMemLength != 0)
   {
      /* write the upper blocks if any */
      PSmartCacheWrite(pContext, &pType1ChipUserConnection->sSmartCacheHighMem,
            pType1ChipUserConnection->sReadWriteOperation.nHighMemOffset, pType1ChipUserConnection->sReadWriteOperation.nHighMemLength,
            pType1ChipUserConnection->sReadWriteOperation.pBuffer + pType1ChipUserConnection->sReadWriteOperation.nLowMemLength,
            (tPBasicGenericCallbackFunction *) static_PType1ChipWriteHighMemCompleted, pType1ChipUserConnection);

   }
   else
   {
      if (pType1ChipUserConnection->sReadWriteOperation.bLockBlocks != W_FALSE)
      {
         /* or lock the blocks if requested */
         static_PType1ChipUserLockBlock(pContext, pType1ChipUserConnection);
      }
      else
      {
         /* operation is now complete */
         static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, W_SUCCESS);
      }
   }
}

/**
 * @brief Called when the high memory has been written
 *
 * @param[in] pContext The context.
 *
 * @param[in] pCallbackParameter The callback parameter, e.g. the Type1 connection context
 *
 * @param[in] nError The result of the operation
 */

static void static_PType1ChipWriteHighMemCompleted(
   tContext * pContext,
   void     * pCallbackParameter,
   W_ERROR    nError)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection *) pCallbackParameter;

   PDebugTrace("static_PType1ChipWriteHighMemCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PType1ChipWriteHighMemCompleted %s", PUtilTraceError(nError));

      static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, nError);
      return;
   }

   /* the high memory has been written successfully */

   if (pType1ChipUserConnection->sReadWriteOperation.bLockBlocks != W_FALSE)
   {
      /* lock the blocks if requested */
      static_PType1ChipUserLockBlock(pContext, pType1ChipUserConnection);
   }
   else
   {
      /* operation is now complete */
      static_PType1ChipInternalOperationCompleted(pContext, pType1ChipUserConnection, W_SUCCESS);
   }
}


/* See Client API Specifications */
W_ERROR WType1ChipReadSync(
            W_HANDLE hConnection,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength )
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WType1ChipReadSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WType1ChipRead(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pBuffer, nOffset, nLength,
            null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR WType1ChipWriteSync(
            W_HANDLE hConnection,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockBlocks )
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WType1ChipWriteSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WType1ChipWrite(
         hConnection,
         PBasicGenericSyncCompletion,
         &param,
         pBuffer, nOffset, nLength,
         bLockBlocks,
         null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See header */

W_ERROR PType1ChipUserInvalidateCache(
            tContext* pContext,
            W_HANDLE  hUserConnection,
            uint32_t nOffset,
            uint32_t nLength)
{
   tPType1ChipUserConnection* pType1ChipUserConnection;
   W_ERROR nError ;

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection);

   if ( nError != W_SUCCESS )
   {
      PDebugError("PType1ChipUserInvalidateCache: could not get pType1ChipUserConnection : %s", PUtilTraceError(nError));
      return nError;
   }

   PSmartCacheInvalidateCache(pContext, &pType1ChipUserConnection->sSmartCache, 0, pType1ChipUserConnection->nLowMemSize);

   if (pType1ChipUserConnection->nHighMemSize)
   {
      PSmartCacheInvalidateCache(pContext, &pType1ChipUserConnection->sSmartCacheHighMem, 0, pType1ChipUserConnection->nHighMemSize);
   }

   return W_SUCCESS;
}


/* See header file */
void PType1ChipUserCreateSecondaryConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty )
{
   tPType1ChipUserConnection* pType1ChipUserConnection;
   W_ERROR               nError;
   tDFCCallbackContext   sCallbackContext;

   PDebugTrace("PType1ChipUserCreateSecondaryConnection : nProperty %d", nProperty);

   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection);

   if ((nError != W_SUCCESS) || (pType1ChipUserConnection == null))
   {
      PDebugError("PType1ChipUserCreateSecondaryConnection : bad connection handle");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto send_result;
   }

   switch (nProperty)
   {
      case W_PROP_JEWEL :

         if (pType1ChipUserConnection->aHeaderRom[0] != P_TYPE1_CHIP_IRT5001)
         {
            PDebugLog("PType1ChipUserCreateSecondaryConnection : not a JEWEL chip !");
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
            goto send_result;
         }

         break;

      case W_PROP_TOPAZ :

         if (pType1ChipUserConnection->aHeaderRom[0] != P_TYPE1_TAG_STATIC_HR0)
         {
            PDebugLog("PType1ChipUserCreateSecondaryConnection : not a TOPAZ chip !");
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
            goto send_result;
         }
         break;

      case W_PROP_TOPAZ_512:

         if (pType1ChipUserConnection->aHeaderRom[0] != P_TYPE1_TAG_TOPAZ_512_HR0)
         {
            PDebugLog("PType1ChipUserCreateSecondaryConnection : not a TOPAZ 512 chip !");
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
            goto send_result;
         }
         break;

      default:

         PDebugError("Unexpected property value %d", nProperty);
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         goto send_result;
   }


   pType1ChipUserConnection->nCardSecondaryConnection = nProperty;
   nError = W_SUCCESS;

send_result:

   /* Send the result */

   PDFCPostContext2(
      & sCallbackContext,
      nError );

}

/** See tPReaderUserRemoveSecondaryConnection */
void PType1ChipUserRemoveSecondaryConnection(
            tContext* pContext,
            W_HANDLE hConnection )
{
   tPType1ChipUserConnection* pType1ChipUserConnection;
   W_ERROR               nError;

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_TYPE1_CHIP_USER_CONNECTION, (void**)&pType1ChipUserConnection);

   if ((nError != W_SUCCESS) || (pType1ChipUserConnection == null))
   {
      PDebugError("PType1ChipUserRemoveSecondaryConnection : bad connection handle");
      return;
   }

   pType1ChipUserConnection->nCardSecondaryConnection = 0;
}

/* Polling command's callback */
static void static_PType1ChipPollCompleted(
      tContext * pContext,
      void * pCallbackParamer,
      uint32_t nLength,
      W_ERROR nResult)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*) pCallbackParamer;

   PHandleClose(pContext, pType1ChipUserConnection->hDriverOperation);

   PDebugTrace("static_PType1ChipPollCompleted");

   /* Send the error */
   PDFCPostContext2(&pType1ChipUserConnection->sCallbackContext, nResult);

   /* Release the reference after completion. May destroy the object */
   PHandleDecrementReferenceCount(pContext, pType1ChipUserConnection);
}

/* Send polling command */
static void static_PType1ChipPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*) pObject;
   uint8_t nOffset = 0;

   PDebugTrace("static_PType1ChipPoll");

   CDebugAssert(pType1ChipUserConnection != null);

   /* Increment the ref count to avoid prematurely freeing during the operation
      The ref count will be decremented in the static_PPicoPollCompleted callback  */
   PHandleIncrementReferenceCount(pType1ChipUserConnection);

   /* store the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*) pCallback,
      pCallbackParameter,
      &pType1ChipUserConnection->sCallbackContext);

   /* Send the command */
   pType1ChipUserConnection->aReaderToCardBuffer[nOffset++] = 0x01;
   pType1ChipUserConnection->aReaderToCardBuffer[nOffset++] = 0x00;
   pType1ChipUserConnection->aReaderToCardBuffer[nOffset++] = 0x00;
   CMemoryCopy(
         &pType1ChipUserConnection->aReaderToCardBuffer[nOffset],
         pType1ChipUserConnection->aUID,
         P_TYPE1_CHIP_SMALL_UID_LENGTH);

   nOffset += P_TYPE1_CHIP_SMALL_UID_LENGTH;

   pType1ChipUserConnection->hDriverOperation = PType1ChipDriverExchangeData(
      pContext,
      pType1ChipUserConnection->hDriverConnection,
      static_PType1ChipPollCompleted,
      pType1ChipUserConnection,
      pType1ChipUserConnection->aReaderToCardBuffer,
      nOffset,
      pType1ChipUserConnection->aCardToReaderBuffer,
      sizeof(pType1ChipUserConnection->aCardToReaderBuffer));
}

/* Execute the queued operation (after polling) */
static void static_PType1ChipExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tPType1ChipUserConnection* pType1ChipUserConnection = (tPType1ChipUserConnection*) pObject;

   PDebugTrace("static_PType1ChipExecuteQueuedExchange");

   /* Restore operation handle */
   pType1ChipUserConnection->hOperation = pType1ChipUserConnection->sQueuedOperation.hOperation;
   /* Restore callback context */
   pType1ChipUserConnection->sCallbackContext = pType1ChipUserConnection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (pType1ChipUserConnection->hOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pType1ChipUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PType1ChipExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_PType1ChipUserCommandCompleted(pContext, pType1ChipUserConnection, nResult);
   }
   else
   {
      switch (pType1ChipUserConnection->sQueuedOperation.nType)
      {
      case P_TYPE1_QUEUED_READ:
         /* Read */
         static_PType1ChipReadInternalEx(
               pContext,
               pType1ChipUserConnection,
               static_PType1ChipUserCommandCompleted, pType1ChipUserConnection,
               pType1ChipUserConnection->sQueuedOperation.pBuffer,
               pType1ChipUserConnection->sQueuedOperation.nOffset,
               pType1ChipUserConnection->sQueuedOperation.nLength);

         break;

      case P_TYPE1_QUEUED_WRITE:
         /* Write */
         static_PType1ChipWriteInternalEx(
               pContext,
               pType1ChipUserConnection,
               static_PType1ChipUserCommandCompleted, pType1ChipUserConnection,
               pType1ChipUserConnection->sQueuedOperation.pBuffer,
               pType1ChipUserConnection->sQueuedOperation.nOffset,
               pType1ChipUserConnection->sQueuedOperation.nLength,
               pType1ChipUserConnection->sQueuedOperation.bLockBlocks);

         break;

      default:
         /* Return an error */
         PDebugError("static_PType1ChipExecuteQueuedExchange: unknown type of operation!");
         static_PType1ChipUserCommandCompleted(pContext, pType1ChipUserConnection, W_ERROR_BAD_STATE);
      }
   }

   /* Reset data */
   CMemoryFill(&pType1ChipUserConnection->sQueuedOperation, 0, sizeof(pType1ChipUserConnection->sQueuedOperation));
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* default timeout used for xchange data type1 chip is 618ms*/
#define P_TYPE1_CHIP_DEFAULT_TIMEOUT           0x0B
#define P_TYPE1_CHIP_ENABLE_TIMEOUT            0x10

/* the maximum frame size for type1 chip */
#define P_TYPE1_CHIP_BUFFER_MAX_SIZE            256

/* Declare a type1 chip driver exchange data structure */
typedef struct __tType1ChipDriverConnection
{
   /* Connection object registry */
   tHandleObjectHeader        sObjectHeader;

   /* Connection information */
   uint8_t                   nTimeout;
   /* response buffer */
   uint8_t*                   pCardToReaderBufferType1Chip;
   /* maximum length of response buffer*/
   uint32_t                   nCardToReaderBufferMaxLengthType1Chip;

   /* Buffer for the payload of a NFC HAL command (Type1 Chip frame)*/
   uint8_t                    aReaderToCardBufferNAL[P_TYPE1_CHIP_BUFFER_MAX_SIZE];

   /* Callback context */
   tDFCDriverCCReference      pDriverCC;

   /* Service Context */
   uint8_t                    nServiceIdentifier;

   /* Service Operation */
   tNALServiceOperation       sServiceOperation;

   /* Operation handle used for cancellation */
   W_HANDLE                   hOperation;

} tType1ChipDriverConnection;

/* Destroy connection callback */
static uint32_t static_PType1ChipDriverDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Connection registry type1 chip  type */
tHandleType g_sType1ChipDriverConnection = { static_PType1ChipDriverDestroyConnection,
                                             null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE1_CHIP_DRIVER_CONNECTION (&g_sType1ChipDriverConnection)

/**
 * @brief   Destroyes a type 1 chip connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PType1ChipDriverDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tType1ChipDriverConnection* pType1ChipDriverConnection = (tType1ChipDriverConnection*)pObject;

   PDebugTrace("static_PType1ChipDriverDestroyConnection");

   PDFCDriverFlushCall(pType1ChipDriverConnection->pDriverCC);

   /* Free the type1 chip  connection structure */
   CMemoryFree( pType1ChipDriverConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @see  PNALServiceExecuteCommand
 **/
static void static_PType1ChipDriverExecuteCommandCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError,
            uint32_t nReceptionCounter)
{
   tType1ChipDriverConnection* pType1ChipDriverConnection = (tType1ChipDriverConnection*)pCallbackParameter;
   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PDebugError(
         "static_PType1ChipDriverExecuteCommandCompleted: nError %s",
         PUtilTraceError(nError) );
      if ( nError == W_ERROR_CANCEL )
      {
         goto return_function;
      }
      nLength = 0;
   }
   else
   {
      PDebugTrace("static_PType1ChipDriverExecuteCommandCompleted: Response");

      PReaderDriverSetAntiReplayReference(pContext);

      /* Check if the buffer is too short */
      if ( nLength > pType1ChipDriverConnection->nCardToReaderBufferMaxLengthType1Chip )
      {
         nLength = 0;
         nError = W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   /* Set the current operation as terminated */

   if (pType1ChipDriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, pType1ChipDriverConnection->hOperation);
      PHandleClose(pContext, pType1ChipDriverConnection->hOperation);
      pType1ChipDriverConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the answer */
   PDFCDriverPostCC3(
      pType1ChipDriverConnection->pDriverCC,
      nLength,
      nError );

return_function:

   /* Release the reference after completion
      May destroy pType1ChipDriverConnection */
   PHandleDecrementReferenceCount(pContext, pType1ChipDriverConnection);
}

/** @see tPReaderDriverCreateConnection() */
static W_ERROR static_PType1ChipDriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection )
{
   tType1ChipDriverConnection* pType1ChipDriverConnection;
   W_HANDLE hDriverConnection;
   W_ERROR nError;

   /* Check the parameters */
   if ( phDriverConnection == null )
   {
      PDebugError("PType1ChipCreateConnection: W_ERROR_BAD_PARAMETER");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Create the Type1 Chip buffer */
   pType1ChipDriverConnection = (tType1ChipDriverConnection*)CMemoryAlloc( sizeof(tType1ChipDriverConnection) );
   if ( pType1ChipDriverConnection == null )
   {
      PDebugError("PType1ChipCreateConnection: pType1ChipDriverConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pType1ChipDriverConnection, 0, sizeof(tType1ChipDriverConnection));

   /* Register the Type1 Chip connection structure */
   if ( ( nError = PHandleRegister(
                        pContext,
                        pType1ChipDriverConnection,
                        P_HANDLE_TYPE1_CHIP_DRIVER_CONNECTION,
                        &hDriverConnection) ) != W_SUCCESS )
   {
      PDebugError("PType1ChipCreateConnection: error on PHandleRegister %s",
         PUtilTraceError(nError) );
      CMemoryFree(pType1ChipDriverConnection);
      return nError;
   }

   /* Store the connection information */
   pType1ChipDriverConnection->nServiceIdentifier = nServiceIdentifier;
   pType1ChipDriverConnection->nTimeout = P_TYPE1_CHIP_DEFAULT_TIMEOUT;
   *phDriverConnection = hDriverConnection;

   return W_SUCCESS;
}

/** @see tPReaderDriverParseDetectionMessage() */
static W_ERROR static_PType1ChipDriverParseDetectionMessage(
               tContext* pContext,
               const uint8_t* pBuffer,
               uint32_t nLength,
               tPReaderDriverCardInfo* pCardInfo )
{
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("static_PType1ChipDriverParseDetectionMessage()");

   if(pCardInfo->nProtocolBF != W_NFCC_PROTOCOL_READER_TYPE_1_CHIP)
   {
      PDebugError("static_PType1ChipDriverParseDetectionMessage: protocol error");
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_function;
   }
   if((pBuffer == null)
   ||(nLength != P_TYPE1_CHIP_DETECTION_MESSAGE_SIZE))
   {
      PDebugTrace("static_PType1ChipDriverParseDetectionMessage(): bad parameters");
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_function;
   }

   /* Get UID size in the ATQA */
   if ( (pBuffer[7] & 0xC0) == 0 )
   {
      pCardInfo->nUIDLength = P_TYPE1_CHIP_SMALL_UID_LENGTH + 2;
   }
   else
   {
      PDebugError("static_PType1ChipDriverParseDetectionMessage: wrong UID length");
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_function;
   }

   /* Copy the "UID" i.e. the 4 first bytes of the 7-bytes UID + the 2 HR bytes */
   CMemoryCopy(
           pCardInfo->aUID,
           pBuffer,
           pCardInfo->nUIDLength );

return_function:

   if(nError == W_SUCCESS)
   {
      PDebugTrace("static_PType1ChipDriverParseDetectionMessage: 4-bytes UID + 2 HR bytes = ");
      PDebugTraceBuffer(pCardInfo->aUID, pCardInfo->nUIDLength);

   }
   else
   {
      PDebugTrace("static_PType1ChipDriverParseDetectionMessage: error %s", PUtilTraceError(nError));
   }

   return nError;
}

/* The protocol information structure */
tPRegistryDriverReaderProtocolConstant g_sType1ChipReaderProtocolConstant = {
   W_NFCC_PROTOCOL_READER_TYPE_1_CHIP,
   NAL_SERVICE_READER_TYPE_1,
   static_PType1ChipDriverCreateConnection,
   null,
   static_PType1ChipDriverParseDetectionMessage,
   null };



/*
 * @brief Cancel the current data exchange operation
 *
 * @param[in] pContext The context
 *
 * @param[in] pCancelParametet The cancel parameter, e.g pType1ChipDriverConnection
 *
 * @param[in] bIsClosing
 *
 */

void PType1ChipDriverExchangeDataCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tType1ChipDriverConnection* pType1ChipDriverConnection = pCancelParameter;

   PDebugTrace("PType1ChipDriverExchangeDataCancel");

   if ( (pType1ChipDriverConnection->hOperation != W_NULL_HANDLE) &&
        (PBasicGetOperationState(pContext, pType1ChipDriverConnection->hOperation) == P_OPERATION_STATE_STARTED) )
   {
      /* request the cancel of the NFC HAL operation */
      PNALServiceCancelOperation(pContext, & pType1ChipDriverConnection->sServiceOperation);
   }
   else
   {
      PDebugTrace("PType1ChipDriverExchangeDataCancel : operation is no longer in the state STARTED");
   }
}


/* See header file */
W_HANDLE PType1ChipDriverExchangeData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength)
{
   tType1ChipDriverConnection* pType1ChipDriverConnection;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pDriverCC );

   nError = PReaderDriverGetConnectionObject(
      pContext, hDriverConnection, P_HANDLE_TYPE1_CHIP_DRIVER_CONNECTION,
      (void**)&pType1ChipDriverConnection);

   if ( nError == W_SUCCESS )
   {
      /* Store the callback context */
      pType1ChipDriverConnection->pDriverCC = pDriverCC;

      /* Check the parameters */
      if (  ( pReaderToCardBuffer == null )
         || ( nReaderToCardBufferLength == 0 )
         || ( nReaderToCardBufferLength >= P_TYPE1_CHIP_BUFFER_MAX_SIZE )
         || (  ( pCardToReaderBuffer == null )
            && ( nCardToReaderBufferMaxLength != 0 ) )
         || (  ( pCardToReaderBuffer != null )
            && ( nCardToReaderBufferMaxLength == 0 ) ) )
      {
         PDebugError("PType1ChipDriverExchangeData: W_ERROR_BAD_PARAMETER");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      /* create an operation */

      pType1ChipDriverConnection->hOperation = PBasicCreateOperation(pContext, PType1ChipDriverExchangeDataCancel, pType1ChipDriverConnection);

      if (pType1ChipDriverConnection->hOperation == W_NULL_HANDLE)
      {
         PDebugError("PType1ChipDriverExchangeData : PBasicCreateOperation failed");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      nError = PHandleDuplicate(pContext, pType1ChipDriverConnection->hOperation, &hOperation);

      if (nError != W_SUCCESS)
      {
         PDebugError("PType1ChipDriverExchangeData : PHandleDuplicate failed %s", PUtilTraceError(nError));
         goto error;
      }


      /* Store the connection information */
      pType1ChipDriverConnection->nCardToReaderBufferMaxLengthType1Chip = nCardToReaderBufferMaxLength;
      /* Store the callback buffer */
      pType1ChipDriverConnection->pCardToReaderBufferType1Chip = pCardToReaderBuffer;

      /* Prepare the command */
      pType1ChipDriverConnection->aReaderToCardBufferNAL[0] =  (NAL_TIMEOUT_READER_XCHG_DATA_ENABLE | pType1ChipDriverConnection->nTimeout);
      CMemoryCopy(
         &pType1ChipDriverConnection->aReaderToCardBufferNAL[1],
         pReaderToCardBuffer,
         nReaderToCardBufferLength );

      /* Increment the reference count to keep the connection object alive
         during the operation.
         The reference count is decreased in static_PType1ChipDriverExecuteCommandCompleted
         when the NFC HAL operation is completed */
      PHandleIncrementReferenceCount(pType1ChipDriverConnection);

      /* Send the command */
      PNALServiceExecuteCommand(
         pContext,
         pType1ChipDriverConnection->nServiceIdentifier,
         &pType1ChipDriverConnection->sServiceOperation,
         NAL_CMD_READER_XCHG_DATA,
         pType1ChipDriverConnection->aReaderToCardBufferNAL,
         nReaderToCardBufferLength + 1,
         pCardToReaderBuffer,
         nCardToReaderBufferMaxLength,
         static_PType1ChipDriverExecuteCommandCompleted,
         pType1ChipDriverConnection );
   }
   else
   {
      PDebugError("PType1ChipDriverExchangeData: could not get pType1ChipDriverConnection buffer");
      /* Send the error */
      PDFCDriverPostCC3(
         pDriverCC,
         0,
         nError );
   }

   return (hOperation);

error:

   if (pType1ChipDriverConnection->hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, pType1ChipDriverConnection->hOperation);
      PHandleClose(pContext, pType1ChipDriverConnection->hOperation);
      pType1ChipDriverConnection->hOperation = W_NULL_HANDLE;
   }

   if (hOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hOperation);
   }

   /* Send the error */
   PDFCDriverPostCC3(
      pType1ChipDriverConnection->pDriverCC,
      0,
      nError );

   return (W_NULL_HANDLE);
}


#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
