/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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
   Contains the implementation of the My-d functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( MY_D )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_MY_D_MAX_SIZE_BUFFER               32

/* The My-d move is organised in 38 blocks of 4 bytes */
#define P_MY_D_BLOCK_SIZE                    P_NDEF2GEN_BLOCK_SIZE
#define P_MY_D_MOVE_BLOCK_NUMBER             38

/* The My-d NFC is organised in pages of 8 bytes */
#define P_MY_D_NFC_PAGE_SIZE                 8

/* My-d block info */
#define P_MY_D_MOVE_CONFIG_BYTE_OFFSET       1
#define P_MY_D_MOVE_PROTECTED_FIRST_BLOCK    0x10
#define P_MY_D_MOVE_LOCK_BLOCK               0x24
#define P_MY_D_MOVE_LOCK_LENGTH              4

#define P_MY_D_FIRST_DATA_BLOCK              (P_NDEF2GEN_STATIC_LOCK_BLOCK + 1)
#define P_MY_D_MOVE_LAST_DATA_BLOCK          (P_MY_D_MOVE_LOCK_BLOCK - 1)
#define P_MY_D_NFC_LAST_DATA_BLOCK           (pMyDConnection->nSectorNumber - 1)

#define P_MY_D_MOVE_MAX_RETRY_COUNTER        7
#define P_MY_D_MOVE_STATUS_BYTE_MASK         (W_MY_D_MOVE_16BITS_COUNTER_ENABLED | W_MY_D_MOVE_READ_WRITE_PASSWORD_ENABLED | W_MY_D_MOVE_WRITE_PASSWORD_ENABLED | W_MY_D_MOVE_CONFIG_LOCKED)
#define P_MY_D_MOVE_PASSWORD_LENGTH          4

/* Smart cache read/write block number and align */
#define P_MY_D_NFC_READ_BLOCK_NUMBER         1
#define P_MY_D_NFC_READ_BLOCK_ALIGN          1
#define P_MY_D_NFC_WRITE_BLOCK_NUMBER        1
#define P_MY_D_NFC_WRITE_BLOCK_ALIGN         1

#define P_MY_D_NFC_READ_2BLOCK_NUMBER         2
#define P_MY_D_NFC_READ_2BLOCK_ALIGN          2
#define P_MY_D_NFC_WRITE_2BLOCK_NUMBER        2
#define P_MY_D_NFC_WRITE_2BLOCK_ALIGN         2

#define P_MY_D_NFC_CMD_WRITE                 0x30
#define P_MY_D_NFC_CMD_WRITE_BYTE            0xE0
#define P_MY_D_NFC_CMD_READ                  0x10

#define P_MY_D_NFC_CMD_WRITE2                0x31
#define P_MY_D_NFC_CMD_READ2                 0x11

#define P_MY_D_NFC_OFFSET_ACCESS_CONDITION   0x09

#define P_MY_D_NFC_VALUE_ACCESS_CONDITION_LOCKED   0x66

/* Queued operation type */
#define P_MYD_QUEUED_NONE                    0
#define P_MYD_QUEUED_READ                    1
#define P_MYD_QUEUED_WRITE                   2
#define P_MYD_QUEUED_AUTHENTICATE            3
#define P_MYD_QUEUED_FREEZE_LOCK_CONF        4
#define P_MYD_QUEUED_SET_CONFIGURATION       5

/*cache Connection defines*/
#define P_MY_D_IDENTIFIER_LEVEL                    ZERO_IDENTIFIER_LEVEL

extern tSmartCacheSectorSize g_sSectorSize8;
extern tSmartCacheSectorSize g_sSectorSize4;


/* Declare a My-d exchange data structure */
typedef struct __tMyDConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;
   /* Connection handle */
   W_HANDLE                   hConnection;

   /* Connection information */
   uint8_t                    nUIDLength;
   uint8_t                    UID[7];

   /* Type of the card (Move, NFC) */
   uint8_t                    nType;

   /* Lock bytes */
   uint8_t                    aLockBytes[6];
   bool_t                       bLockBytesRetrieved;

   union
   {
      struct
      {
         /* Config byte of My-d move */
         uint8_t              nMyDMoveConfigByte;
         uint8_t              nPendingConfigByte;
         /* W_TRUE if authentication has been done */
         bool_t                 bAuthenticated;
      } sMove;
      struct
      {
         tSmartCache*         pSmartCacheDynamic;
         tDFCCallbackContext  sCacheCallbackContext;
         uint32_t             nOffsetToInvalidate;
         uint32_t             nLengthToInvalidate;
      }sNFC;
   } uMyD;

   /* temporary data */
   uint8_t                    aCommandBuffer[P_MY_D_MAX_SIZE_BUFFER ];
   uint8_t                    aResponseBuffer[P_MY_D_MAX_SIZE_BUFFER];

   uint32_t                   nOffsetToLock;
   uint32_t                   nLengthToLock;

   uint32_t                   nCurrentOperationState;

   /* Smart cache data */
   tSmartCacheSectorSize*     pSectorSize;
   uint32_t                   nSectorNumber;

   /* Callback context */
   tDFCCallbackContext        sCallbackContext;
   /* Operation handle */
   W_HANDLE                   hOperation;

   /* Hold data of the queued operation which will be executed after the polling completion */
   struct __tQueuedOperation
   {
      /* Type of operation: Read, Write... */
      uint32_t             nType;
      /* Data */
      uint8_t*             pBuffer;
      uint32_t             nOffset;
      uint32_t             nLength;
      bool_t                 bLockSectors;
      uint32_t             nPassword;
      uint8_t              nPendingConfigByte;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hOperation;
   } sQueuedOperation;

} tMyDConnection;

static void static_PMyDNFCReadDynamic(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter);


/* See tSmartCacheWriteSector */
static void static_PMyDNFCWriteDynamic(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               const uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter);


/*SmartCacheDescriptor for myD NFC */
static tSmartCacheDescriptor g_sDescriptorMyDNFCDynamic = {
   P_MY_D_IDENTIFIER_LEVEL, &g_sSectorSize8,
   {
      { P_MY_D_NFC_READ_BLOCK_ALIGN, P_MY_D_NFC_READ_BLOCK_NUMBER, static_PMyDNFCReadDynamic },
      { 0, 0, null }
   },
   {
      { P_MY_D_NFC_WRITE_BLOCK_ALIGN, P_MY_D_NFC_WRITE_BLOCK_NUMBER, static_PMyDNFCWriteDynamic },
      { 0, 0, null }
   },
};

static tSmartCacheDescriptor g_sDescriptorMyDNFCDynamic2 = {
   P_MY_D_IDENTIFIER_LEVEL, &g_sSectorSize8,
   {
      { P_MY_D_NFC_READ_BLOCK_ALIGN, P_MY_D_NFC_READ_BLOCK_NUMBER, static_PMyDNFCReadDynamic },
     { P_MY_D_NFC_READ_2BLOCK_ALIGN, P_MY_D_NFC_READ_2BLOCK_NUMBER, static_PMyDNFCReadDynamic },
   },
   {
      { P_MY_D_NFC_WRITE_BLOCK_ALIGN, P_MY_D_NFC_WRITE_BLOCK_NUMBER, static_PMyDNFCWriteDynamic },
     { P_MY_D_NFC_READ_2BLOCK_ALIGN, P_MY_D_NFC_READ_2BLOCK_NUMBER, static_PMyDNFCWriteDynamic },
   },
};

/**
 * @brief   Destroyes a My-d connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PMyDDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pObject;

   PDebugTrace("static_PMyDDestroyConnection");

   PDFCFlushCall(&pMyDConnection->sCallbackContext);

   if(pMyDConnection->nType == W_PROP_MY_D_NFC
      && pMyDConnection->uMyD.sNFC.pSmartCacheDynamic != null)
   {

      PSmartCacheDestroyCache(pContext, pMyDConnection->uMyD.sNFC.pSmartCacheDynamic);
      /* Free the smartCache used by the connection */
      CMemoryFree(pMyDConnection->uMyD.sNFC.pSmartCacheDynamic);
   }

   /* Free the My-d connection structure */
   CMemoryFree( pMyDConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the My-d connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 **/

static uint32_t static_PMyDGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/* See PReaderExchangeData */
static void static_PMyDExchangeData(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation )
{
   bool_t bCheckResponseCRC = W_TRUE;
   bool_t bCheckAckOrNack = W_FALSE;

   /* Mifare UL and My-d Cards - Do not ask for CRC Check for "WRITE", WRITE2B", "COMPATIBILITY WRITE" commands */

   if ((nReaderToCardBufferLength >= 1) && (pReaderToCardBuffer != null) &&
       ((pReaderToCardBuffer[0] == 0xA2) || (pReaderToCardBuffer[0] == 0xA1) || (pReaderToCardBuffer[0] == 0xA0)))
   {
      PDebugTrace("static_PMyDExchangeData: No CRC for Mifare UL and MyD WRITE operation");
      bCheckResponseCRC = W_FALSE;
      bCheckAckOrNack = W_TRUE;
   }

   P14Part3UserExchangeDataEx(
            pContext,
            pObject,
            pCallback, pCallbackParameter,
            pReaderToCardBuffer, nReaderToCardBufferLength,
            pCardToReaderBuffer, nCardToReaderBufferMaxLength,
            phOperation,
            bCheckResponseCRC, bCheckAckOrNack);
}

/**
 * @brief   Gets the My-d connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PMyDGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pObject;

   PDebugTrace("static_PMyDGetProperties");

   pPropertyArray[0] = pMyDConnection->nType;

   return W_TRUE;
}

/**
 * @brief   Checkes the My-d connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PMyDCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pObject;

   PDebugTrace("static_PMyDCheckProperties: nPropertyValue=%s (0x%02X)",
               PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue);

   return (pMyDConnection->nType == nPropertyValue) ? W_TRUE : W_FALSE;
}

/* Send polling command */
static void static_PMyDNFCPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

/* Handle registry My-d connection type */
tHandleType g_sMyDMoveConnection = { static_PMyDDestroyConnection,
                                 null,
                                 static_PMyDGetPropertyNumber,
                                 static_PMyDGetProperties,
                                 static_PMyDCheckProperties,
                                 null, null,
                                 static_PMyDExchangeData,
                                 null };

/* Handle registry My-d connection type */
tHandleType g_sMyDNFCConnection = { static_PMyDDestroyConnection,
                                 null,
                                 static_PMyDGetPropertyNumber,
                                 static_PMyDGetProperties,
                                 static_PMyDCheckProperties,
                                 null, null,
                                 static_PMyDExchangeData,
                                 static_PMyDNFCPoll };

#define P_HANDLE_TYPE_MY_D_MOVE_CONNECTION (&g_sMyDMoveConnection)
#define P_HANDLE_TYPE_MY_D_NFC_CONNECTION  (&g_sMyDNFCConnection)


#define GetBit(value, bit)    (((value) >> (bit)) & 0x01)
#define SetBit(value, bit)    ((value) = (value) | (1 << (bit)))

/**
  * Retreive the lock status of a sector by parsing the lock bytes of the card
  *
  * return W_TRUE if the sector is locked
  */
static bool_t static_PMyDIsSectorLocked(
      tContext * pContext,
      tMyDConnection * pMyDConnection,
      uint32_t         nSector)
{
   if (nSector <= 1)
   {
      /* Sectors [0 - 1] are locked */
      return W_TRUE;
   }

   if (nSector == 2)
   {
      /* Check the block locking lockbits */
      return (pMyDConnection->aLockBytes[0] & 0x07) == 0x07;
   }

   if ((3 <= nSector) && (nSector <= 7))
   {
      /* sectors 3-7 locks are located in aLockBytes[0] */
      return GetBit(pMyDConnection->aLockBytes[0], nSector);
   }

   if ((8 <= nSector) && (nSector <= 15))
   {
      /* sectors 8-15 locks are located in aLockBytes[1] */
      return GetBit(pMyDConnection->aLockBytes[1], nSector - 8);
   }

   if (W_PROP_MY_D_MOVE == pMyDConnection->nType)
   {
      if ((16 <= nSector)  && (nSector <= 23))
      {
         return GetBit(pMyDConnection->aLockBytes[2], nSector - 16);
      }

      if ((24 <= nSector)  && (nSector <= 31))
      {
         return GetBit(pMyDConnection->aLockBytes[3], nSector - 24);
      }

      if ((32 <= nSector)  && (nSector <= 35))
      {
         return GetBit(pMyDConnection->aLockBytes[4], nSector - 32);
      }

      if (nSector == 36)
      {
         /* Check the block locking lockbits */
         return ( (pMyDConnection->aLockBytes[5] & 0x0F) == 0x0F );
      }

      if (nSector == 37)
      {
         /* Manufacturer block is locked */
         return W_TRUE;
      }
   }
   else if (W_PROP_MY_D_NFC == pMyDConnection->nType)
   {
      /* No lock bytes for these sectors in My-d NFC */
      if ((16 <= nSector)  && (nSector < pMyDConnection->nSectorNumber))
      {
         return W_FALSE;
      }
   }

   /* should not occur */
   CDebugAssert(0);
   return W_TRUE;
}


static void static_PMyDLockSector(
      tContext * pContext,
      tMyDConnection * pMyDConnection,
      uint32_t         nSector)
{
   if ((3 <= nSector) && (nSector <= 7))
   {
      /* sectors 3-7 locks are located in aLockBytes[0] */
      SetBit(pMyDConnection->aLockBytes[0], nSector);
      return;
   }

   if ((8 <= nSector) && (nSector <= 15))
   {
      /* sectors 8-15 locks are located in aLockBytes[1] */
      SetBit(pMyDConnection->aLockBytes[1], nSector - 8);
      return;
   }

   if (W_PROP_MY_D_MOVE == pMyDConnection->nType)
   {
      if ((16 <= nSector)  && (nSector <= 23))
      {
         SetBit(pMyDConnection->aLockBytes[2], nSector - 16);
         return;
      }

      if ((24 <= nSector)  && (nSector <= 31))
      {
         SetBit(pMyDConnection->aLockBytes[3], nSector - 24);
         return;
      }

      if ((32 <= nSector)  && (nSector <= 35))
      {
         SetBit(pMyDConnection->aLockBytes[4], nSector - 32);
         return;
      }
   }
   else if (W_PROP_MY_D_NFC == pMyDConnection->nType)
   {
      /* No lock bytes for these sectors in My-d NFC */
      if ((16 <= nSector)  && (nSector < pMyDConnection->nSectorNumber))
      {
         return;
      }
   }

   /* should not occur */
   CDebugAssert(0);
}

/**
  * Retreive the lockable status of a sector by parsing the lock bytes of the card
  *
  * return W_TRUE if the sector is lockable
  */
static bool_t static_PMyDIsSectorLockable(
      tContext * pContext,
      tMyDConnection * pMyDConnection,
      uint32_t         nSector)
{
   if (pMyDConnection->nType == W_PROP_MY_D_MOVE)
   {
      if (nSector <= 2)
      {
         /* Lock bytes shall be locked with the dedicated API */
         return W_FALSE;
      }

      if ((3 <= nSector) && (nSector <= 15))
      {
         return GetBit(pMyDConnection->aLockBytes[0], (nSector + 2) / 6) == 0;
      }

      if ((16 <= nSector) && (nSector <= 35))
      {
         return GetBit(pMyDConnection->aLockBytes[5], (nSector - 16) / 5) == 0;
      }

      if (nSector == 36)
      {
         /* Lock bytes shall be locked with the dedicated API */
         return W_FALSE;
      }

      if (nSector == 37)
      {
         /* Manufacturer block is not lockable */
         return W_FALSE;
      }
   }
   else
   {
      /* My-d NFC is not lockable */
      return W_FALSE;
   }

   /* should not occur */
   CDebugAssert(0);
   return W_FALSE;
}

/**
 * @brief   Sends the result.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pMyDConnection  The My-d connection.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PMyDSendResult(
            tContext* pContext,
            tMyDConnection* pMyDConnection,
            W_ERROR nError )
{
   PDebugTrace("static_PMyDSendResult");

   if (pMyDConnection->hOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pMyDConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PMyDSendResult: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pMyDConnection->hOperation);
      PHandleClose(pContext, pMyDConnection->hOperation);
      pMyDConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the error */
   PDFCPostContext2(&pMyDConnection->sCallbackContext, nError);

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, pMyDConnection->hConnection);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pMyDConnection);
}

/**
 *  Lock Bytes 0-1 have been written.
 */
static void static_PMyDWriteLockBytesCompleted(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*) pCallbackParameter;

   PDebugTrace("static_PMyDWriteLockBytesCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PMyDWriteLockBytesCompleted: returning %s", PUtilTraceError(nError));
   }

   static_PMyDSendResult(pContext, pMyDConnection, nError);
}

/**
 *  Lock Bytes 2-3 have been written.
 */
static void static_PMyDWriteLockBytes2Completed(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*) pCallbackParameter;

   PDebugTrace("static_PMyDWriteLockBytes2Completed");

   if (nError == W_SUCCESS)
   {
      PNDEF2GenWrite(pContext,
                     pMyDConnection->hConnection,
                     static_PMyDWriteLockBytesCompleted,
                     pMyDConnection,
                     &pMyDConnection->aLockBytes[0],
                     P_NDEF2GEN_STATIC_LOCK_BYTE_ADDRESS,
                     P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH);
   }
   else
   {
      PDebugError("static_PMyDWriteLockBytes2Completed: returning %s", PUtilTraceError(nError));

      static_PMyDSendResult(pContext, pMyDConnection, nError);
   }
}


/**
 *  Writes the lock bytes into the card
 */
static void static_PMyDWriteLockBytes(
            tContext* pContext,
            tMyDConnection* pMyDConnection)
{
   PDebugTrace("static_PMyDWriteLockBytes");

   if (pMyDConnection->nType == W_PROP_MY_D_MOVE)
   {
      PNDEF2GenWrite(pContext,
                     pMyDConnection->hConnection,
                     static_PMyDWriteLockBytes2Completed,
                     pMyDConnection,
                     &pMyDConnection->aLockBytes[2],
                     P_MY_D_MOVE_LOCK_BLOCK *P_NDEF2GEN_BLOCK_SIZE,
                     P_MY_D_MOVE_LOCK_LENGTH);
   }
   else
   {
      /* fake the write of the Lock Bytes 2-3 */
      static_PMyDWriteLockBytes2Completed(pContext, pMyDConnection, W_SUCCESS);
   }
}

/**
 * Bytes have been read
 */
static void static_PMyDReadCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pCallbackParameter;

   PDebugTrace("static_PMyDReadCompleted");

   static_PMyDSendResult(pContext, pMyDConnection, nError );
}

static void static_PMyDNFCLockSectorAutomaton(
            tContext* pContext,
            void * pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pCallbackParameter;
   PDebugTrace("static_PMyDWriteCompleted");

   if(nLength == 1)
   {
      /* if we have locked all sectors, we return send the callback and exit this function */
      if(pMyDConnection->nLengthToLock == 0)
      {
         static_PMyDSendResult( pContext, pMyDConnection, W_SUCCESS );
      }
      else
      {
         uint32_t nIndex = 0;

         CDebugAssert(pMyDConnection->nLengthToLock > 0);

         /* Command byte */
         pMyDConnection->aCommandBuffer[nIndex++] = P_MY_D_NFC_CMD_WRITE_BYTE;

         /* Page Adr => lower byte first*/
         pMyDConnection->aCommandBuffer[nIndex++] = (uint8_t)(pMyDConnection->nOffsetToLock & 0x000000FF);
         pMyDConnection->aCommandBuffer[nIndex++] = (uint8_t)((pMyDConnection->nOffsetToLock & 0x0000FF00) >> 8);

         /* Byte written offset => Access condition */
         pMyDConnection->aCommandBuffer[nIndex++] = P_MY_D_NFC_OFFSET_ACCESS_CONDITION;
         pMyDConnection->aCommandBuffer[nIndex++] = P_MY_D_NFC_VALUE_ACCESS_CONDITION_LOCKED;

         pMyDConnection->nLengthToLock --;
         pMyDConnection->nOffsetToLock ++;

         P14P3UserExchangeData(
                  pContext,
                  pMyDConnection->hConnection,
                  static_PMyDNFCLockSectorAutomaton,
                  pMyDConnection,
                  pMyDConnection->aCommandBuffer,
                  nIndex,
                  pMyDConnection->aResponseBuffer,
                  sizeof(pMyDConnection->aResponseBuffer),
                  null,
                  W_FALSE,
                  W_FALSE);
      }
      return;
   }

    /* Send the result */
   static_PMyDSendResult( pContext, pMyDConnection, nError );
}

/**
 * Bytes have been written
 */
static void static_PMyDNFCWriteCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pCallbackParameter;

   PDebugTrace("static_PMyDWriteCompleted");

   if(nError == W_SUCCESS)
   {
      /*check if it supports the NDEF Type 2*/
      if(PHandleCheckProperty(pContext, pMyDConnection->hConnection, W_PROP_NFC_TAG_TYPE_2_GENERIC) == W_SUCCESS
         /* AND*/
         &&
         /* if some datas have been written */
         pMyDConnection->uMyD.sNFC.nLengthToInvalidate > 0
         &&
         /*| ndef first offset = Total Size * sector size / 2 = Total Size * 8/2 = Total Size * 4 |
             if first ndef offset is lower than then address written. Nothing sould be done
         */
         (g_sSectorSize4.pMultiply(pMyDConnection->nSectorNumber) < (pMyDConnection->uMyD.sNFC.nLengthToInvalidate + pMyDConnection->uMyD.sNFC.nOffsetToInvalidate))
         )
      {
         /* Calcul the Ndef Length and offset to invalidate */
         /* First Case offset is lower or equals than ndef offset starting  */
         if(g_sSectorSize4.pMultiply(pMyDConnection->nSectorNumber) >= pMyDConnection->uMyD.sNFC.nOffsetToInvalidate)
         {
            pMyDConnection->uMyD.sNFC.nLengthToInvalidate -= g_sSectorSize4.pMultiply(pMyDConnection->nSectorNumber) - pMyDConnection->uMyD.sNFC.nOffsetToInvalidate;
            pMyDConnection->uMyD.sNFC.nOffsetToInvalidate = 0;
         }

         /* Second case */
         /* We need to reduce to the ndef offset the starting offset */
         else
         {
            pMyDConnection->uMyD.sNFC.nOffsetToInvalidate -= g_sSectorSize4.pMultiply(pMyDConnection->nSectorNumber);
            /* The length does not change */
         }

         PNDEF2GenInvalidateCache(
                     pContext,
                     pMyDConnection->hConnection,
                     pMyDConnection->uMyD.sNFC.nOffsetToInvalidate,
                     pMyDConnection->uMyD.sNFC.nLengthToInvalidate);

      }


      /* Check if we need to lock the bytes */
      if ( pMyDConnection->nLengthToLock != 0 )
      {
         /* We give 1 as third argument to perform the first step*/
         static_PMyDNFCLockSectorAutomaton(
                     pContext,
                     pMyDConnection,
                     1,
                     W_SUCCESS);
         return;
      }
   }

   /* Send the result */
   static_PMyDSendResult( pContext, pMyDConnection, nError );
}

/**
 * Bytes have been written
 */
static void static_PMyDMoveWriteCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pCallbackParameter;

   PDebugTrace("static_PMyDWriteCompleted");

   if(nError == W_SUCCESS)
   {
      /* Check if we need to lock the bytes */
      if ( pMyDConnection->nLengthToLock != 0 )
      {
         uint32_t nBlockStart, nBlockEnd, nIndex;

         nBlockStart = pMyDConnection->pSectorSize->pDivide(pMyDConnection->nOffsetToLock);
         nBlockEnd   = pMyDConnection->pSectorSize->pDivide(pMyDConnection->nLengthToLock + pMyDConnection->nOffsetToLock - 1);

         for( nIndex = nBlockStart; nIndex <= nBlockEnd; nIndex ++ )
         {
            static_PMyDLockSector(pContext, pMyDConnection, nIndex);
         }

         pMyDConnection->nLengthToLock = 0;

         /* Write lock bytes */
         static_PMyDWriteLockBytes(pContext, pMyDConnection);

         return;
      }
   }

   /* Send the result */
   static_PMyDSendResult( pContext, pMyDConnection, nError );
}

/**
 * The Configuration byte has been written
 */
static void static_PMyDMoveWriteConfigurationCompleted(
      tContext * pContext,
      void     * pCallbackParameter,
      W_ERROR    nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*) pCallbackParameter;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PMyDMoveWriteConfigurationCompleted : nError %d", nError);
      static_PMyDSendResult(pContext, pMyDConnection, nError);
      return;
   }

   /* If config is locked, status byte and retry counter are ignored */
   if ( (pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_CONFIG_LOCKED) != 0 )
   {
      /* Save the new config byte */
      pMyDConnection->uMyD.sMove.nMyDMoveConfigByte = pMyDConnection->uMyD.sMove.nPendingConfigByte;
   }

   static_PMyDSendResult(pContext, pMyDConnection, W_SUCCESS);
}

/**
 * The Password have been written
 */
static void static_PMyDMoveSetPasswordCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pCallbackParameter;

   PDebugTrace("static_PMyDMoveSetPasswordCompleted");

   /* Clear password in buffers */
   CMemoryFill(&pMyDConnection->aCommandBuffer[1], 0, P_MY_D_MOVE_PASSWORD_LENGTH);
   CMemoryFill(&pMyDConnection->aResponseBuffer[0], 0, P_MY_D_MOVE_PASSWORD_LENGTH);

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PMyDMoveSetPasswordCompleted : nError %d", nError);
      static_PMyDSendResult(pContext, pMyDConnection, nError);
      return;
   }

   /* If config is locked, status byte and retry counter are ignored */
   if ( (pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_CONFIG_LOCKED) != 0 )
   {
      static_PMyDMoveWriteConfigurationCompleted(pContext, pMyDConnection, W_SUCCESS);
   }
   else
   {
      /* Else, write the configuration byte */
      PNDEF2GenWrite(pContext,
                     pMyDConnection->hConnection,
                     static_PMyDMoveWriteConfigurationCompleted,
                     pMyDConnection,
                     &pMyDConnection->uMyD.sMove.nPendingConfigByte,
           P_NDEF2GEN_STATIC_LOCK_BLOCK * P_NDEF2GEN_BLOCK_SIZE + P_MY_D_MOVE_CONFIG_BYTE_OFFSET,
                     1);
   }
}

/**
 * Authentication has been done
 */
static void static_PMyDMoveAuthenticateCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pCallbackParameter;

   PDebugTrace("static_PMyDMoveSetPasswordCompleted");

   /* Clear password in buffer */
   CMemoryFill(&pMyDConnection->aCommandBuffer[1], 0, P_MY_D_MOVE_PASSWORD_LENGTH);

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PMyDMoveSetPasswordCompleted : nError %d", nError);
      pMyDConnection->uMyD.sMove.bAuthenticated = W_FALSE;
      static_PMyDSendResult(pContext, pMyDConnection, nError);
      return;
   }

   /* Authenticated */
   pMyDConnection->uMyD.sMove.bAuthenticated = W_TRUE;

   static_PMyDSendResult(pContext, pMyDConnection, W_SUCCESS);
}

/* Automaton used during connection creation */
static void static_PMyDCreateConnectionAutomaton(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pCallbackParameter;
   bool_t bFreeStructure          = W_FALSE;

   PDebugTrace("static_PMyDCreateConnectionAutomaton");


   if (W_SUCCESS != nError)
   {
      /* Send error */
      PDebugError("static_PMyDCreateConnectionAutomaton: returning %s", PUtilTraceError(nError));
      goto end;
   }

   switch (pMyDConnection->nCurrentOperationState)
   {
      case 0:
         /* Add the My-d connection structure */
         if ( ( nError = PHandleAddHeir(
                           pContext,
                           pMyDConnection->hConnection,
                           pMyDConnection,
                           pMyDConnection->nType == W_PROP_MY_D_NFC ? P_HANDLE_TYPE_MY_D_NFC_CONNECTION : P_HANDLE_TYPE_MY_D_MOVE_CONNECTION ) ) != W_SUCCESS )
         {
            PDebugError("static_PMyDCreateConnectionAutomaton: error returned by PHandleAddHeir()");
            /* Free pMyDConnection here since PHandleAddHeir has not been called yet
               thus PMyDRemoveConnection cannot free it */
            bFreeStructure = W_TRUE;
            goto end;
         }

         if (pMyDConnection->nSectorNumber == 77)
         {

            pMyDConnection->uMyD.sNFC.pSmartCacheDynamic = (tSmartCache *) CMemoryAlloc(sizeof(tSmartCache));

            if(pMyDConnection->uMyD.sNFC.pSmartCacheDynamic == null)
            {
               PDebugError("PNDEF2GenCreateSmartCacheDynamic: not enough memory for smartcache");
               goto end;
            }

            /* Create the smart cache for the dynamic area */
            if((nError = PSmartCacheCreateCache(
                        pContext,
                        pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
                        pMyDConnection->nSectorNumber,
                        &g_sDescriptorMyDNFCDynamic,
                        pMyDConnection)) != W_SUCCESS)
            {
               PDebugError("PNDEF2GenCreateSmartCacheDynamic: error creating the smart cache");
            }

            goto end;
         }

         /* Create the dynamic smart cache */
         if ( (nError = PNDEF2GenCreateSmartCacheDynamic(
                           pContext,
                           pMyDConnection->hConnection,
                           pMyDConnection->nSectorNumber)) != W_SUCCESS)
         {
            PDebugError("static_PMyDCreateConnectionAutomaton : PNDEF2GenCreateSmartCacheDynamic returned %s", PUtilTraceError(nError));
            goto end;
         }

         /* Read config byte and lock bytes 0-1 */
         PNDEF2GenRead(pContext,
                       pMyDConnection->hConnection,
                       static_PMyDCreateConnectionAutomaton,
                       pMyDConnection,
                       pMyDConnection->aResponseBuffer,
                       P_NDEF2GEN_STATIC_LOCK_BLOCK * P_NDEF2GEN_BLOCK_SIZE,
                       P_NDEF2GEN_BLOCK_SIZE);
         break;

      case 1:
         /* Retrieve lock bytes 0-1 */
         CMemoryCopy(&pMyDConnection->aLockBytes[0],
                     &pMyDConnection->aResponseBuffer[P_NDEF2GEN_STATIC_LOCK_BYTE_OFFSET],
                     P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH);

         if (W_PROP_MY_D_MOVE == pMyDConnection->nType)
         {
            /* Retrieve config byte */
            pMyDConnection->uMyD.sMove.nMyDMoveConfigByte = pMyDConnection->aResponseBuffer[P_MY_D_MOVE_CONFIG_BYTE_OFFSET];

            /* Read lock bytes 2-5 */
            PNDEF2GenRead(pContext,
                          pMyDConnection->hConnection,
                          static_PMyDCreateConnectionAutomaton,
                          pMyDConnection,
                          &pMyDConnection->aLockBytes[P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH],
                          P_MY_D_MOVE_LOCK_BLOCK * P_NDEF2GEN_BLOCK_SIZE,
                          P_MY_D_MOVE_LOCK_LENGTH);
            break;
         }
         else
         {
            if(W_PROP_MY_D_NFC == pMyDConnection->nType)
            {
               pMyDConnection->uMyD.sNFC.pSmartCacheDynamic = (tSmartCache *) CMemoryAlloc(sizeof(tSmartCache));

               if(pMyDConnection->uMyD.sNFC.pSmartCacheDynamic == null)
               {
                  PDebugError("PNDEF2GenCreateSmartCacheDynamic: not enough memory for smartcache");
                  goto end;
               }

               /* Create the smart cache for the dynamic area */
               if((nError = PSmartCacheCreateCache(
                           pContext,
                           pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
                           pMyDConnection->nSectorNumber,
                           &g_sDescriptorMyDNFCDynamic2,
                           pMyDConnection)) != W_SUCCESS)
               {
                  PDebugError("PNDEF2GenCreateSmartCacheDynamic: error creating the smart cache");

                  goto end;
               }
            }
            pMyDConnection->nCurrentOperationState++;
            /* No break. For My-d NFC, go directly to the next step */
         }

      case 2:
         /* All bytes have been read */
         pMyDConnection->bLockBytesRetrieved = W_TRUE;
         nError = W_SUCCESS;
         goto end;
   }

   pMyDConnection->nCurrentOperationState++;
   return;

end:
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

   PDFCPostContext2(&pMyDConnection->sCallbackContext, nError);

   if(bFreeStructure == W_TRUE)
   {
      CMemoryFree(pMyDConnection);
   }
}

/**
 * The My-d NFC manufacturer bytes have been read
 */
static void static_PMyDNFCReadManufacturerCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*)pCallbackParameter;

   PDebugTrace("static_PMyDNFCReadManufacturerCompleted");

   if (W_SUCCESS == nError)
   {
      /* Retrieve the My-d NFC type and size from the manufacturer CI byte */
      switch (pMyDConnection->aResponseBuffer[0] & 0xC7)
      {
      case 0xC2:
         /* SLE 66R04P */
         pMyDConnection->nSectorNumber = 77;   /* 77 blocks = 616 bytes */
         break;

      case 0xC4:
         /* SLE 66R16P */
         pMyDConnection->nSectorNumber = 256;   /* 256 blocks = 1024 bytes */
         break;

      case 0xC5:
         /* SLE 66R32P */
         pMyDConnection->nSectorNumber = 512;   /* 512 blocks = 2048 bytes */
         break;

      default:
         PDebugLog("static_PMyDNFCReadManufacturerCompleted: unsupported My-d NFC card");
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         break;
      }
   }

   if(nError == W_SUCCESS)
   {
      /* Continue creation */
      static_PMyDCreateConnectionAutomaton(pContext, pCallbackParameter, W_SUCCESS);
   }else
   {
      /* Stop Creation */
      if( (nError == W_ERROR_RF_PROTOCOL_NOT_SUPPORTED)  ||
          (nError == W_ERROR_RF_COMMUNICATION)           ||
          (nError == W_ERROR_TIMEOUT))
      {
         nError = W_ERROR_RF_COMMUNICATION;
      }
      else
      {
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
      }

      PDFCPostContext2(&pMyDConnection->sCallbackContext, nError);

      CMemoryFree(pMyDConnection);
      pMyDConnection = null;
   }
}



/* See Header file */
void PMyDCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty )
{
   static const uint8_t pRead[] = { 0x10, 0x02, 0x00 };
   tMyDConnection* pMyDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PMyDCreateConnection");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the NDEF buffer */
   pMyDConnection = (tMyDConnection*)CMemoryAlloc( sizeof(tMyDConnection) );
   if ( pMyDConnection == null )
   {
      PDebugError("PMyDCreateConnection: pMyDConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pMyDConnection, 0, sizeof(tMyDConnection));

   /* Get the 14443-3 information level */
   if ( ( nError = P14P3UserCheckMyD(
                     pContext,
                     hConnection,
                     pMyDConnection->UID,
                     &pMyDConnection->nUIDLength,
                     &pMyDConnection->nType ) ) != W_SUCCESS )
   {
      PDebugLog("PMyDCreateConnection: not a My-d card");
      goto return_error;
   }

   PDebugTrace("PMyDCreateConnection: detection of the type %s",
      PUtilTraceConnectionProperty(pMyDConnection->nType));

   /* Store the connection information */
   pMyDConnection->hConnection = hConnection;

   /* Store the callback context */
   pMyDConnection->sCallbackContext = sCallbackContext;

   /* Begining of automaton */
   pMyDConnection->nCurrentOperationState = 0;

   switch ( pMyDConnection->nType )
   {
   case W_PROP_MY_D_MOVE:
      /* My-d move size is known */
      pMyDConnection->pSectorSize = &g_sSectorSize4;
      pMyDConnection->nSectorNumber = P_MY_D_MOVE_BLOCK_NUMBER;
      static_PMyDCreateConnectionAutomaton(pContext, pMyDConnection, W_SUCCESS);
      break;

   case W_PROP_MY_D_NFC:
      /* Read My-d NFC size in Manufacturer CI byte */
      pMyDConnection->pSectorSize = &g_sSectorSize8;
      P14P3UserExchangeData(
         pContext,
         pMyDConnection->hConnection,
         static_PMyDNFCReadManufacturerCompleted, pMyDConnection,
         pRead, sizeof(pRead),
         pMyDConnection->aResponseBuffer, P_MY_D_NFC_PAGE_SIZE,
         W_NULL_HANDLE,
         W_TRUE,
         W_FALSE);
      break;

   default:
      PDebugError("PMyDCreateConnection: unknown type");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   return;

return_error:

   if(pMyDConnection != null)
   {
      CMemoryFree(pMyDConnection);
   }

   PDFCPostContext2(&sCallbackContext, nError);
}

static tHandleType* static_PMyDGetHandleType(
            tContext* pContext,
            W_HANDLE hConnection)
{
   if (PHandleCheckProperty(pContext, hConnection, W_PROP_MY_D_NFC) == W_SUCCESS)
   {
      return P_HANDLE_TYPE_MY_D_NFC_CONNECTION;
   }

   return P_HANDLE_TYPE_MY_D_MOVE_CONNECTION;
}

/** See tPReaderUserRemoveSecondaryConnection */
void PMyDRemoveConnection(
            tContext* pContext,
            W_HANDLE hUserConnection )
{
   tHandleType* pHandleType = static_PMyDGetHandleType(pContext, hUserConnection);

   tMyDConnection* pMyDConnection = (tMyDConnection*)PHandleRemoveLastHeir(
            pContext, hUserConnection,
            pHandleType);

   PDebugTrace("PMyDRemoveConnection");

   /* Remove the connection object */
   if(pMyDConnection != null)
   {
      CMemoryFree(pMyDConnection);
   }
}

/* See Header file */
W_ERROR PMyDCheckType2(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumSpaceSize,
            uint8_t* pnSectorSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable )
{
   tMyDConnection* pMyDConnection;
   uint32_t nIndex;
   W_ERROR nError;
   bool_t    bIsLocked = W_FALSE;
   bool_t    bIsLockable = W_TRUE;
   uint32_t nLastBlockIndex;

   tHandleType* pHandleType = static_PMyDGetHandleType(pContext, hConnection);

   PDebugTrace("PMyDCheckType2");

   /* Reset the maximum tag size */
   if (pnMaximumSpaceSize != null) *pnMaximumSpaceSize = 0;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, pHandleType, (void**)&pMyDConnection);
   if ( nError == W_SUCCESS )
   {
      /* Maximum size */
      switch ( pMyDConnection->nType )
      {
      case W_PROP_MY_D_MOVE:
         nLastBlockIndex = P_MY_D_MOVE_LAST_DATA_BLOCK;
         break;
      case W_PROP_MY_D_NFC:
         nLastBlockIndex = P_MY_D_NFC_LAST_DATA_BLOCK;
         break;
      default:
         /* other My-d cards are not supported */
         PDebugError("PMyDCheckType2: Unknown type");
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }

      if (pnMaximumSpaceSize != null)
      {
         *pnMaximumSpaceSize = pMyDConnection->pSectorSize->pMultiply(nLastBlockIndex - P_MY_D_FIRST_DATA_BLOCK + 1);
      }

      /* Go through the lock byte */
      for (nIndex=4; nIndex <= nLastBlockIndex; nIndex++)
      {
         bIsLocked |= static_PMyDIsSectorLocked(pContext, pMyDConnection, nIndex);
         bIsLockable &= static_PMyDIsSectorLockable(pContext, pMyDConnection, nIndex);
      }

      if (pbIsLocked != null) *pbIsLocked = bIsLocked;
      if (pbIsLockable != null) *pbIsLockable = bIsLockable;
      if (pnSectorSize != null) *pnSectorSize = (uint8_t)pMyDConnection->pSectorSize->nValue;
      if (pbIsFormattable != null) *pbIsFormattable = ! bIsLocked;

      return W_SUCCESS;
   }

   return nError;
}

/* See header file */
W_ERROR PMyDNDEF2Lock(tContext * pContext,
                      W_HANDLE hConnection)
{
   tMyDConnection* pMyDConnection = null;
   W_ERROR nError;
   tHandleType* pHandleType = static_PMyDGetHandleType(pContext, hConnection);

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, pHandleType, (void**)&pMyDConnection);
   if (nError != W_SUCCESS)
   {
      PDebugError("PMyDNDEF2Lock : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      return nError;
   }

   /* Update lock bytes */
   CMemoryFill(&pMyDConnection->aLockBytes[0], 0xFF, 2);
   if (W_PROP_MY_D_MOVE == pMyDConnection->nType)
   {
      CMemoryFill(&pMyDConnection->aLockBytes[2], 0xFF, 2);
      CMemoryFill(&pMyDConnection->aLockBytes[4], 0x0F, 2);
   }

   return W_SUCCESS;
}

/* See Client API Specifications */
W_ERROR PMyDGetConnectionInfo(
            tContext* pContext,
            W_HANDLE hConnection,
            tWMyDConnectionInfo *pConnectionInfo )
{
   tMyDConnection* pMyDConnection;
   W_ERROR nError;
   tHandleType* pHandleType = static_PMyDGetHandleType(pContext, hConnection);

   PDebugTrace("PMyDGetConnectionInfo");

   /* Check the parameters */
   if ( pConnectionInfo == null )
   {
      PDebugError("PMyDGetConnectionInfo: pConnectionInfo == null");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PReaderUserGetConnectionObject(pContext, hConnection, pHandleType, (void**)&pMyDConnection);
   if ( nError == W_SUCCESS )
   {
      /* UID */
      CMemoryCopy(
         pConnectionInfo->UID,
         pMyDConnection->UID,
         pMyDConnection->nUIDLength );
      /* Sector size */
      pConnectionInfo->nSectorSize = (uint16_t) pMyDConnection->pSectorSize->nValue;
      /* Sector number */
      pConnectionInfo->nSectorNumber = (uint16_t)pMyDConnection->nSectorNumber;
      return W_SUCCESS;
   }
   else
   {
      PDebugError("PMyDGetConnectionInfo: could not get pMyDConnection buffer");

      /* Fill in the structure with zeros */
      CMemoryFill(pConnectionInfo, 0, sizeof(tWMifareConnectionInfo));

      return nError;
   }
}

/* See tPBasicGenericDataCallback */
static void static_PMyDNFCSmartCacheActionCompleted(tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nResult)
{
   /* Just make the wrapping for SmartCache Read/Write function */
   tMyDConnection * pMyDConnection = (tMyDConnection*) pCallbackParameter;

   PDFCPostContext2(&(pMyDConnection->uMyD.sNFC.sCacheCallbackContext), nResult);
}

W_ERROR PMyDNFCInvalidateSmartCacheNDEF(
                  tContext * pContext,
                  W_HANDLE hConnection,
                  uint32_t nOffset,
                  uint32_t nLength)
{
   /* Just make the wrapping for SmartCache Read/Write function */
   tMyDConnection * pMyDConnection = null;
   W_ERROR nError;

   PDebugTrace("pMyDNFCInvalidateSmartCache");

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MY_D_NFC_CONNECTION, (void**)&pMyDConnection);
   if (pMyDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("pMyDNFCInvalidateSmartCache: Bad handle");
      return nError;
   }

   nOffset += g_sSectorSize4.pMultiply(pMyDConnection->nSectorNumber);

   CDebugAssert((nOffset + nLength) <= g_sSectorSize8.pMultiply(pMyDConnection->nSectorNumber));

   PSmartCacheInvalidateCache(
         pContext,
         pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
         nOffset,
         nLength);

   return W_SUCCESS;
}

static void static_PMyDNFCReadDynamic(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{
   tMyDConnection* pMyDConnection = (tMyDConnection *) pConnection;

   uint8_t nIndex = 0;

   PDebugTrace("static_PMyDNFCReadDynamic");
   CDebugAssert((nSectorNumber == 1) || (nSectorNumber == 2));

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &(pMyDConnection->uMyD.sNFC.sCacheCallbackContext) );

   if  ( ((nSectorNumber != 1) && (nSectorNumber != 2))
        || ( nSectorOffset + nSectorNumber > pMyDConnection->nSectorNumber) )
   {
      PDebugTrace("Error on static_PMyDNFCReadDynamic : W_ERROR_BAD_PARAMETERS => \n\
                     nSectorNumber = %d \n\
                     nSectorOffset = %d \n\
                     pMyDConnection->nSectorNumber = %d",nSectorNumber, nSectorOffset, pMyDConnection->nSectorNumber );

      PDFCPostContext2(&(pMyDConnection->uMyD.sNFC.sCacheCallbackContext),W_ERROR_BAD_PARAMETER);
      return;
   }

   if (nSectorNumber == 1)
   {
      pMyDConnection->aCommandBuffer[nIndex++] = P_MY_D_NFC_CMD_READ;
   }
   else
   {
      pMyDConnection->aCommandBuffer[nIndex++] = P_MY_D_NFC_CMD_READ2;
   }

   pMyDConnection->aCommandBuffer[nIndex++] = (uint8_t) (nSectorOffset & 0x000000FF);
   pMyDConnection->aCommandBuffer[nIndex++] = (uint8_t) ((nSectorOffset & 0x0000FF00) >> 8);

   P14P3UserExchangeData(
      pContext,
      pMyDConnection->hConnection,
      static_PMyDNFCSmartCacheActionCompleted,
      pMyDConnection,
      pMyDConnection->aCommandBuffer,
      nIndex,
      pBuffer,
      nSectorNumber * g_sSectorSize8.nValue,
      null,
      W_TRUE,
      W_FALSE);
}

/* See tSmartCacheWriteSector */
static void static_PMyDNFCWriteDynamic(
               tContext* pContext,
               void* pConnection,
               uint32_t nSectorOffset,
               uint32_t nSectorNumber,
               const uint8_t* pBuffer,
               tPBasicGenericCallbackFunction* pCallback,
               void* pCallbackParameter)
{
   tMyDConnection* pMyDConnection = (tMyDConnection *) pConnection;

   uint8_t nIndex = 0;

   PDebugTrace("static_PMyDNFCWriteDynamic");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &(pMyDConnection->uMyD.sNFC.sCacheCallbackContext) );

   if( ( (nSectorNumber != 1) && (nSectorNumber != 2))
        || ( nSectorOffset + nSectorNumber > pMyDConnection->nSectorNumber) )
   {
      PDebugTrace("Error on static_PMyDNFCWriteDynamic : W_ERROR_BAD_PARAMETERS => \n\
                     nSectorNumber = %d \n\
                     nSectorOffset = %d \n\
                     pMyDConnection->nSectorNumber = %d",nSectorNumber, nSectorOffset, pMyDConnection->nSectorNumber );

      PDFCPostContext2(&(pMyDConnection->uMyD.sNFC.sCacheCallbackContext), W_ERROR_BAD_PARAMETER);
      return;
   }

   if (nSectorNumber ==1 )
   {
      pMyDConnection->aCommandBuffer[nIndex++] = P_MY_D_NFC_CMD_WRITE;
   }
   else
   {
      pMyDConnection->aCommandBuffer[nIndex++] = P_MY_D_NFC_CMD_WRITE2;
   }
   pMyDConnection->aCommandBuffer[nIndex++] = (uint8_t) (nSectorOffset & 0x000000FF);
   pMyDConnection->aCommandBuffer[nIndex++] = (uint8_t) ((nSectorOffset & 0x0000FF00) >> 8);

   CMemoryCopy(&pMyDConnection->aCommandBuffer[nIndex], pBuffer, 8 * nSectorNumber);
   nIndex =  (uint8_t) (nIndex + 8 * nSectorNumber);


   P14P3UserExchangeData(
      pContext,
      pMyDConnection->hConnection,
      static_PMyDNFCSmartCacheActionCompleted,
      pMyDConnection,
      pMyDConnection->aCommandBuffer,
      nIndex,
      pMyDConnection->aResponseBuffer,
      sizeof(pMyDConnection->aResponseBuffer),
      null,
      W_FALSE,
      W_FALSE);
}


static void static_PMyDExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*) pObject;

   PDebugTrace("static_PMyDExecuteQueuedExchange");

   /* Restore operation handle */
   pMyDConnection->hOperation = pMyDConnection->sQueuedOperation.hOperation;
   /* Restore callback context */
   pMyDConnection->sCallbackContext = pMyDConnection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (pMyDConnection->hOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pMyDConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PMyDExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_PMyDSendResult(pContext, pMyDConnection, nResult);
   }
   else
   {
      switch (pMyDConnection->sQueuedOperation.nType)
      {
      case P_MYD_QUEUED_READ:
         /* Read */
         switch(pMyDConnection->nType)
         {
         case W_PROP_MY_D_MOVE:
            PNDEF2GenRead(pContext,
                          pMyDConnection->hConnection,
                          static_PMyDReadCompleted,
                          pMyDConnection,
                          pMyDConnection->sQueuedOperation.pBuffer,
                          pMyDConnection->sQueuedOperation.nOffset,
                          pMyDConnection->sQueuedOperation.nLength);
            break;

         case W_PROP_MY_D_NFC:
            PSmartCacheRead(
                     pContext,
                     pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
                     pMyDConnection->sQueuedOperation.nOffset,
                     pMyDConnection->sQueuedOperation.nLength,
                     pMyDConnection->sQueuedOperation.pBuffer,
                     static_PMyDReadCompleted,
                     pMyDConnection);
            break;
         }

         break;

      case P_MYD_QUEUED_WRITE:
         /* Write */
         switch(pMyDConnection->nType)
         {
         case W_PROP_MY_D_MOVE:
            /* Store the exchange information */
            if (pMyDConnection->sQueuedOperation.bLockSectors != W_FALSE)
            {
               pMyDConnection->nOffsetToLock = pMyDConnection->sQueuedOperation.nOffset;
               pMyDConnection->nLengthToLock = pMyDConnection->sQueuedOperation.nLength;

               if (pMyDConnection->sQueuedOperation.pBuffer == null)
               {
                  /* You must write nothing but just locking */
                  static_PMyDMoveWriteCompleted(pContext, pMyDConnection, W_SUCCESS);
                  break;
               }
            }
            else
            {
               pMyDConnection->nOffsetToLock = 0;
               pMyDConnection->nLengthToLock = 0;
            }

            PNDEF2GenWrite(pContext,
                           pMyDConnection->hConnection,
                           static_PMyDMoveWriteCompleted,
                           pMyDConnection,
                           pMyDConnection->sQueuedOperation.pBuffer,
                           pMyDConnection->sQueuedOperation.nOffset,
                           pMyDConnection->sQueuedOperation.nLength);

            break;

         case W_PROP_MY_D_NFC:
            /* Store the exchange information */
            if (pMyDConnection->sQueuedOperation.bLockSectors != W_FALSE)
            {
               if (pMyDConnection->sQueuedOperation.nLength == 0 && pMyDConnection->sQueuedOperation.nOffset == 0)
               {
                  pMyDConnection->nOffsetToLock = 0;
                  pMyDConnection->nLengthToLock = pMyDConnection->nSectorNumber;
               }
               else
               {
                  /* search the first offset to lock*/
                  pMyDConnection->nOffsetToLock = g_sSectorSize8.pDivide(pMyDConnection->sQueuedOperation.nOffset);
                  pMyDConnection->nLengthToLock = g_sSectorSize8.pDivide(pMyDConnection->sQueuedOperation.nOffset + g_sSectorSize8.nValue + pMyDConnection->sQueuedOperation.nLength - 1) - pMyDConnection->nOffsetToLock;
               }

               if(pMyDConnection->sQueuedOperation.pBuffer == null)
               {
                  /* You must write nothing but just locking */
                  static_PMyDNFCLockSectorAutomaton(pContext, pMyDConnection, 1, W_SUCCESS);
                  break;
               }
            }
            else
            {
               pMyDConnection->nOffsetToLock = 0;
               pMyDConnection->nLengthToLock = 0;
            }

            pMyDConnection->uMyD.sNFC.nOffsetToInvalidate = pMyDConnection->sQueuedOperation.nOffset;
            pMyDConnection->uMyD.sNFC.nLengthToInvalidate = pMyDConnection->sQueuedOperation.nLength;

            PSmartCacheWrite(
                     pContext,
                     pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
                     pMyDConnection->sQueuedOperation.nOffset,
                     pMyDConnection->sQueuedOperation.nLength,
                     pMyDConnection->sQueuedOperation.pBuffer,
                     static_PMyDNFCWriteCompleted,
                     pMyDConnection);

            break;
         }

         break;

      case P_MYD_QUEUED_AUTHENTICATE:
         /* Send ACCESS command */
         pMyDConnection->aCommandBuffer[0] = 0xB2;
         pMyDConnection->aCommandBuffer[1] = (uint8_t)(pMyDConnection->sQueuedOperation.nPassword);
         pMyDConnection->aCommandBuffer[2] = (uint8_t)(pMyDConnection->sQueuedOperation.nPassword >> 8);
         pMyDConnection->aCommandBuffer[3] = (uint8_t)(pMyDConnection->sQueuedOperation.nPassword >> 16);
         pMyDConnection->aCommandBuffer[4] = (uint8_t)(pMyDConnection->sQueuedOperation.nPassword >> 24);

         P14P3UserExchangeData(
            pContext,
            pMyDConnection->hConnection,
            static_PMyDMoveAuthenticateCompleted, pMyDConnection,
            pMyDConnection->aCommandBuffer, 1 + P_MY_D_MOVE_PASSWORD_LENGTH,
            null, 0, /* No data in response */
            W_NULL_HANDLE,
            W_FALSE,   /* No CRC in response */
            W_TRUE);

         break;

      case P_MYD_QUEUED_FREEZE_LOCK_CONF:
         /* Freeze data lock configuration */
         /* LOCK byte # 0 : Set the bits 1 and 2 that lock the other lock bits */
         pMyDConnection->aLockBytes[0] |= 0x06;
         /* LOCK byte # 5 : Set the bits 0-3 that lock the other lock bits */
         pMyDConnection->aLockBytes[5] |= 0x0F;

         /* Write lock bytes */
         static_PMyDWriteLockBytes(pContext, pMyDConnection);

         break;

      case P_MYD_QUEUED_SET_CONFIGURATION:
         /* Set Configuration */
         /* If config is locked, status byte and retry counter are ignored */
         if ( (pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_CONFIG_LOCKED) == 0 )
         {
            /* New value of config byte to write after password */
            pMyDConnection->uMyD.sMove.nPendingConfigByte = pMyDConnection->sQueuedOperation.nPendingConfigByte;
         }

         /* Set password */
         pMyDConnection->aCommandBuffer[0] = 0xB1;
         pMyDConnection->aCommandBuffer[1] = (uint8_t)(pMyDConnection->sQueuedOperation.nPassword);
         pMyDConnection->aCommandBuffer[2] = (uint8_t)(pMyDConnection->sQueuedOperation.nPassword >> 8);
         pMyDConnection->aCommandBuffer[3] = (uint8_t)(pMyDConnection->sQueuedOperation.nPassword >> 16);
         pMyDConnection->aCommandBuffer[4] = (uint8_t)(pMyDConnection->sQueuedOperation.nPassword >> 24);

         P14P3UserExchangeData(
            pContext,
            pMyDConnection->hConnection,
            static_PMyDMoveSetPasswordCompleted, pMyDConnection,
            pMyDConnection->aCommandBuffer, 1 + P_MY_D_MOVE_PASSWORD_LENGTH,
            pMyDConnection->aResponseBuffer, P_MY_D_MOVE_PASSWORD_LENGTH,
            W_NULL_HANDLE,
            W_TRUE,
            W_FALSE);

         break;

      default:
         /* Return an error */
         PDebugError("static_PMyDExecuteQueuedExchange: unknown type of operation!");
         static_PMyDSendResult(pContext, pMyDConnection, W_ERROR_BAD_STATE);
      }
   }

   /* Reset data */
   CMemoryFill(&pMyDConnection->sQueuedOperation, 0, sizeof(pMyDConnection->sQueuedOperation));
}

/* See Client API Specifications */
void PMyDRead(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            W_HANDLE *phOperation )
{
   tMyDConnection* pMyDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nError;
   tHandleType* pHandleType = static_PMyDGetHandleType(pContext, hConnection);

   PDebugTrace("PMyDRead");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, pHandleType, (void**)&pMyDConnection);
   if (pMyDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PMyDRead: Bad handle");
      goto return_error;
   }

   /* Check type of connection */
   if (pMyDConnection->nType != W_PROP_MY_D_MOVE && pMyDConnection->nType != W_PROP_MY_D_NFC)
   {
      PDebugError("PMyDRead: unknown type");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   /* Check parameters */
   if ( (null == pBuffer) || (0 == nLength) )
   {
      PDebugError("PMyDRead: null parameter");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( (nOffset + nLength) > pMyDConnection->pSectorSize->pMultiply(pMyDConnection->nSectorNumber) )
   {
      PDebugError("PMyDRead: the data to read/write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

    /* For My-d move, check if authentication is required and done or not */
   if ( (pMyDConnection->nType == W_PROP_MY_D_MOVE) &&
        ((nOffset + nLength) > pMyDConnection->pSectorSize->pMultiply(P_MY_D_MOVE_PROTECTED_FIRST_BLOCK)) &&
        ((pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_READ_WRITE_PASSWORD_ENABLED) != 0) &&
        (W_FALSE == pMyDConnection->uMyD.sMove.bAuthenticated) )
   {
      PDebugError("PMyDRead: authentication is required");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PMyDRead: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PMyDRead: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMyDExecuteQueuedExchange, pMyDConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Store the operation handle */
      CDebugAssert(pMyDConnection->hOperation == W_NULL_HANDLE);
      pMyDConnection->hOperation = hOperation;

      /* Store the callback context */
      pMyDConnection->sCallbackContext = sCallbackContext;

      switch(pMyDConnection->nType)
      {
      case W_PROP_MY_D_MOVE:
         PNDEF2GenRead(pContext,
                       hConnection,
                       static_PMyDReadCompleted,
                       pMyDConnection,
                       pBuffer,
                       nOffset,
                       nLength);
         break;

      case W_PROP_MY_D_NFC:
         PSmartCacheRead(
                  pContext,
                  pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
                  nOffset,
                  nLength,
                  pBuffer,
                  static_PMyDReadCompleted,
                  pMyDConnection);
         break;
      }

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Save the operation handle */
      CDebugAssert(pMyDConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pMyDConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pMyDConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMyDConnection->sQueuedOperation.nType = P_MYD_QUEUED_READ;

      /* Save data */
      pMyDConnection->sQueuedOperation.pBuffer = pBuffer;
      pMyDConnection->sQueuedOperation.nOffset = nOffset;
      pMyDConnection->sQueuedOperation.nLength = nLength;

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
   PDebugError("PMyDRead: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
void PMyDWrite(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction *pCallback,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockSectors,
            W_HANDLE *phOperation )
{
   tMyDConnection* pMyDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nError;
   tHandleType* pHandleType = static_PMyDGetHandleType(pContext, hConnection);
   uint32_t nIndex;

   PDebugTrace("PMyDWrite");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, pHandleType, (void**)&pMyDConnection);
   if (pMyDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PMyDWrite: Bad handle");
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockSectors == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("PMyDWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockSectors == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("PMyDWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( (nOffset + nLength) > pMyDConnection->pSectorSize->pMultiply(pMyDConnection->nSectorNumber) )
   {
      PDebugError("PMyDWrite: the data to read/write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( (nLength > 0) && (nOffset < pMyDConnection->pSectorSize->pMultiply(P_MY_D_FIRST_DATA_BLOCK)) )
   {
      PDebugError("PMyDWrite: writing of Serial Number or Lock bytes is not allowed. Use the specialized API for locking !");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if (pMyDConnection->nType == W_PROP_MY_D_MOVE)
   {
      /* Special My-D Move checks */
      if ( (pMyDConnection->pSectorSize->pDivide(nOffset) == P_MY_D_MOVE_LOCK_BLOCK) ||
           (pMyDConnection->pSectorSize->pDivide(nOffset + nLength) == P_MY_D_MOVE_LOCK_BLOCK) )
      {
         PDebugError("PMyDWrite: writing of lock bytes is not allowed. Use the specialized API for locking !");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }

      if ((pBuffer == null) && (nOffset == 0) && (nLength == 0))
      {
         nOffset =  pMyDConnection->pSectorSize->pMultiply(P_MY_D_FIRST_DATA_BLOCK);
         nLength =  pMyDConnection->pSectorSize->pMultiply(P_MY_D_MOVE_LAST_DATA_BLOCK - P_MY_D_FIRST_DATA_BLOCK + 1);
      }

      /* For My-d move, check if authentication is required and done or not */
      if ( ((nOffset + nLength) > pMyDConnection->pSectorSize->pMultiply(P_MY_D_MOVE_PROTECTED_FIRST_BLOCK)) &&
           ((pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_WRITE_PASSWORD_ENABLED) != 0) &&
           (W_FALSE == pMyDConnection->uMyD.sMove.bAuthenticated) )
      {
         PDebugError("PMyDWrite: authentication is required");
         nError = W_ERROR_BAD_STATE;
         goto return_error;
      }

      /* Check if the My-d card is locked/lockable */
      for (nIndex = pMyDConnection->pSectorSize->pDivide(nOffset); nIndex <= pMyDConnection->pSectorSize->pDivide(nOffset + nLength - 1); nIndex++)
      {
         if ((pBuffer != null) && (static_PMyDIsSectorLocked(pContext, pMyDConnection, nIndex) != W_FALSE))
         {
            PDebugError("PMyDWrite: item locked");
            nError = W_ERROR_ITEM_LOCKED;
            goto return_error;
         }

         if ((bLockSectors != W_FALSE) && (static_PMyDIsSectorLockable(pContext, pMyDConnection, nIndex) == W_FALSE))
         {
            PDebugError("PMyDWrite: item not lockable");
            nError = W_ERROR_ITEM_LOCKED;
            goto return_error;
         }
      }
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PMyDWrite: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PMyDWrite: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMyDExecuteQueuedExchange, pMyDConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Store the operation handle */
      CDebugAssert(pMyDConnection->hOperation == W_NULL_HANDLE);
      pMyDConnection->hOperation = hOperation;

      /* Store the callback context */
      pMyDConnection->sCallbackContext = sCallbackContext;

      switch(pMyDConnection->nType)
      {
      case W_PROP_MY_D_MOVE:
         /* Store the exchange information */
         if (bLockSectors != W_FALSE)
         {
            pMyDConnection->nOffsetToLock = nOffset;
            pMyDConnection->nLengthToLock = nLength;

            if (pBuffer == null)
            {
               /* You must write nothing but just locking */
               static_PMyDMoveWriteCompleted(pContext, pMyDConnection, W_SUCCESS);
               break;
            }
         }
         else
         {
            pMyDConnection->nOffsetToLock = 0;
            pMyDConnection->nLengthToLock = 0;
         }

         PNDEF2GenWrite(pContext,
                        hConnection,
                        static_PMyDMoveWriteCompleted,
                        pMyDConnection,
                        pBuffer,
                        nOffset,
                        nLength);

         break;

      case W_PROP_MY_D_NFC:
         /* Store the exchange information */
         if (bLockSectors != W_FALSE)
         {
            if (nLength == 0 && nOffset == 0)
            {
               pMyDConnection->nOffsetToLock = 0;
               pMyDConnection->nLengthToLock = pMyDConnection->nSectorNumber;
            }
            else
            {
               /* search the first offset to lock*/
               pMyDConnection->nOffsetToLock = g_sSectorSize8.pDivide(nOffset);
               pMyDConnection->nLengthToLock = g_sSectorSize8.pDivide(nOffset + g_sSectorSize8.nValue + nLength - 1) - pMyDConnection->nOffsetToLock;
            }

            if(pBuffer == null)
            {
               /* You must write nothing but just locking */
               static_PMyDNFCLockSectorAutomaton(pContext, pMyDConnection, 1, W_SUCCESS);
               break;
            }
         }
         else
         {
            pMyDConnection->nOffsetToLock = 0;
            pMyDConnection->nLengthToLock = 0;
         }

         pMyDConnection->uMyD.sNFC.nOffsetToInvalidate = nOffset;
         pMyDConnection->uMyD.sNFC.nLengthToInvalidate = nLength;

         PSmartCacheWrite(
                  pContext,
                  pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
                  nOffset,
                  nLength,
                  pBuffer,
                  static_PMyDNFCWriteCompleted,
                  pMyDConnection);

         break;
      }

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Save the operation handle */
      CDebugAssert(pMyDConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pMyDConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pMyDConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMyDConnection->sQueuedOperation.nType = P_MYD_QUEUED_WRITE;

      /* Save data */
      pMyDConnection->sQueuedOperation.pBuffer = (uint8_t*)pBuffer;
      pMyDConnection->sQueuedOperation.nOffset = nOffset;
      pMyDConnection->sQueuedOperation.nLength = nLength;
      pMyDConnection->sQueuedOperation.bLockSectors = bLockSectors;

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
   PDebugError("PMyDWrite: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* ------------------------------------------------------ */

/* See client API */
void PMyDMoveFreezeDataLockConfiguration(
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tMyDConnection* pMyDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PMyDMoveFreezeDataLockConfiguration");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MY_D_MOVE_CONNECTION, (void**)&pMyDConnection);
   if (pMyDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PMyDMoveFreezeDataLockConfiguration : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   if (pMyDConnection->nType != W_PROP_MY_D_MOVE)
   {
      /* Not a My-d move connection */
      PDebugError("PMyDMoveFreezeDataLockConfiguration : not a My-d move");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMyDExecuteQueuedExchange, pMyDConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Store the callback context */
      pMyDConnection->sCallbackContext = sCallbackContext;

      /* LOCK byte # 0 : Set the bits 1 and 2 that lock the other lock bits */
      pMyDConnection->aLockBytes[0] |= 0x06;
      /* LOCK byte # 5 : Set the bits 0-3 that lock the other lock bits */
      pMyDConnection->aLockBytes[5] |= 0x0F;

      /* Write lock bytes */
      static_PMyDWriteLockBytes(pContext, pMyDConnection);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Save callback context */
      pMyDConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMyDConnection->sQueuedOperation.nType = P_MYD_QUEUED_FREEZE_LOCK_CONF;

      return;

   default:
      goto return_error;
   }

return_error :

   /* Send the error */
   PDFCPostContext2(&sCallbackContext, nError);
}

/* ------------------------------------------------------ */

/* See client API */
void PMyDMoveGetConfiguration (
      tContext * pContext,
      W_HANDLE hConnection,
      tPMyDMoveGetConfigurationCompleted * pCallback,
      void * pCallbackParameter)
{
   tMyDConnection* pMyDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint8_t nStatusByte = 0;
   uint8_t nPasswordRetryCounter = 0;

   PDebugTrace("PMyDMoveGetConfiguration");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MY_D_MOVE_CONNECTION, (void**)&pMyDConnection);
   if (pMyDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PMyDMoveGetConfiguration : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   /* Only My-d move are supported */
   if (pMyDConnection->nType != W_PROP_MY_D_MOVE)
   {
      PDebugError("PMyDMoveGetConfiguration : not a My-d move");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Return parameters */
   nStatusByte = (pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & P_MY_D_MOVE_STATUS_BYTE_MASK);
   nPasswordRetryCounter = ((pMyDConnection->uMyD.sMove.nMyDMoveConfigByte >> 4) & 0x07);

   PDFCPostContext4(&sCallbackContext, W_SUCCESS, nStatusByte, nPasswordRetryCounter);

   return;

return_error:

   /* Send the error */
   PDFCPostContext4(&sCallbackContext, nError, 0, 0);
}

/* ------------------------------------------------------ */

/* See client API */
void PMyDMoveSetConfiguration (
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericCallbackFunction *pCallback,
      void *pCallbackParameter,
      uint8_t nStatusByte,
      uint8_t nPasswordRetryCounter,
      uint32_t nPassword,
      bool_t bLockConfiguration)
{
   tMyDConnection* pMyDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PMyDMoveSetConfiguration");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MY_D_MOVE_CONNECTION, (void**)&pMyDConnection);
   if (pMyDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PMyDMoveSetConfiguration : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   /* Only My-d move are supported */
   if (pMyDConnection->nType != W_PROP_MY_D_MOVE)
   {
      PDebugError("PMyDMoveSetConfiguration : not a My-d move connection");
      nError =  W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check if authentication is required and done or not */
   if ( (((pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_WRITE_PASSWORD_ENABLED) != 0) ||
         ((pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_READ_WRITE_PASSWORD_ENABLED) != 0)) &&
         (W_FALSE == pMyDConnection->uMyD.sMove.bAuthenticated) )
   {
      PDebugError("PMyDWrite: authentication is required to set password");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   /* check parameters */
   if (nPasswordRetryCounter > P_MY_D_MOVE_MAX_RETRY_COUNTER)
   {
      PDebugError("PMyDMoveSetConfiguration: wrong nPasswordRetryCounter");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* If config is locked, status byte and retry counter are ignored */
   if ( (pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_CONFIG_LOCKED) == 0 )
   {
      /* RFU and PCN bytes must be 0 */
      /* READ without WRITE password protection does not exist */
      /* Config bits are one time programmable and cannot be reset to 0 */
      if ( ((nStatusByte & P_MY_D_MOVE_STATUS_BYTE_MASK) != nStatusByte) ||
           (((nStatusByte | pMyDConnection->uMyD.sMove.nMyDMoveConfigByte) & P_MY_D_MOVE_STATUS_BYTE_MASK) != nStatusByte))
      {
         PDebugError("PMyDMoveSetConfiguration: wrong nStatusByte");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMyDExecuteQueuedExchange, pMyDConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Store the callback context */
      pMyDConnection->sCallbackContext = sCallbackContext;

      /* If config is locked, status byte and retry counter are ignored */
      if ( (pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_CONFIG_LOCKED) == 0 )
      {
         /* New value of config byte to write after password */
         pMyDConnection->uMyD.sMove.nPendingConfigByte = (nStatusByte | (nPasswordRetryCounter << 4));
      }

      /* Set My-d Move password */
      pMyDConnection->aCommandBuffer[0] = 0xB1;
      pMyDConnection->aCommandBuffer[1] = (uint8_t)(nPassword);
      pMyDConnection->aCommandBuffer[2] = (uint8_t)(nPassword >> 8);
      pMyDConnection->aCommandBuffer[3] = (uint8_t)(nPassword >> 16);
      pMyDConnection->aCommandBuffer[4] = (uint8_t)(nPassword >> 24);

      P14P3UserExchangeData(
         pContext,
         pMyDConnection->hConnection,
         static_PMyDMoveSetPasswordCompleted, pMyDConnection,
         pMyDConnection->aCommandBuffer, 1 + P_MY_D_MOVE_PASSWORD_LENGTH,
         pMyDConnection->aResponseBuffer, P_MY_D_MOVE_PASSWORD_LENGTH,
         W_NULL_HANDLE,
         W_TRUE,
         W_FALSE);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Save callback context */
      pMyDConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMyDConnection->sQueuedOperation.nType = P_MYD_QUEUED_SET_CONFIGURATION;

      /* Save data */
      pMyDConnection->sQueuedOperation.nPassword = nPassword;

      /* If config is locked, status byte and retry counter are ignored */
      if ( (pMyDConnection->uMyD.sMove.nMyDMoveConfigByte & W_MY_D_MOVE_CONFIG_LOCKED) == 0 )
      {
         /* New value of config byte to write after password */
         pMyDConnection->sQueuedOperation.nPendingConfigByte = (nStatusByte | (nPasswordRetryCounter << 4));
      }

      return;

   default:
      goto return_error;
   }

return_error:

   /* Send the error */
   PDFCPostContext2(&sCallbackContext, nError);
}

/* ------------------------------------------------------ */

/* See Client API */
void PMyDMoveAuthenticate (
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericCallbackFunction *pCallback,
      void *pCallbackParameter,
      uint32_t nPassword)
{
   tMyDConnection* pMyDConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR             nError;

   PDebugTrace("PMyDMoveAuthenticate");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MY_D_MOVE_CONNECTION, (void**)&pMyDConnection);
   if (pMyDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PMyDMoveAuthenticate : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   /* Only My-d move are supported */
   if (pMyDConnection->nType != W_PROP_MY_D_MOVE)
   {
      PDebugError("PMyDMoveAuthenticate : not a My-d move connection");
      nError =  W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMyDExecuteQueuedExchange, pMyDConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Store the callback context */
      pMyDConnection->sCallbackContext = sCallbackContext;

      /* Send ACCESS command */
      pMyDConnection->aCommandBuffer[0] = 0xB2;
      pMyDConnection->aCommandBuffer[1] = (uint8_t)(nPassword);
      pMyDConnection->aCommandBuffer[2] = (uint8_t)(nPassword >> 8);
      pMyDConnection->aCommandBuffer[3] = (uint8_t)(nPassword >> 16);
      pMyDConnection->aCommandBuffer[4] = (uint8_t)(nPassword >> 24);

      P14P3UserExchangeData(
         pContext,
         pMyDConnection->hConnection,
         static_PMyDMoveAuthenticateCompleted, pMyDConnection,
         pMyDConnection->aCommandBuffer, 1 + P_MY_D_MOVE_PASSWORD_LENGTH,
         null, 0, /* No data in response */
         W_NULL_HANDLE,
         W_FALSE,   /* No CRC in response */
         W_TRUE);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMyDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMyDConnection);

      /* Save callback context */
      pMyDConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMyDConnection->sQueuedOperation.nType = P_MYD_QUEUED_AUTHENTICATE;

      /* Save data */
      pMyDConnection->sQueuedOperation.nPassword = nPassword;

      return;

   default:
      goto return_error;
   }

return_error:

   PDFCPostContext2(&sCallbackContext, nError);
}

/* ------------------------------------------------------ */

/* See Client API Specifications */
W_ERROR WMyDReadSync(
                  W_HANDLE hConnection,
                  uint8_t* pBuffer,
                  uint32_t nOffset,
                  uint32_t nLength)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMyDReadSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMyDRead(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pBuffer, nOffset, nLength,
            null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR WMyDWriteSync(
                  W_HANDLE hConnection,
                  const uint8_t* pBuffer,
                  uint32_t nOffset,
                  uint32_t nLength,
                  bool_t bLockSectors)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMyDWriteSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMyDWrite(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pBuffer, nOffset, nLength, bLockSectors,
            null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR WMyDMoveFreezeDataLockConfigurationSync(
                  W_HANDLE hConnection)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMyDMoveFreezeDataLockConfigurationSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMyDMoveFreezeDataLockConfiguration(
            hConnection,
            PBasicGenericSyncCompletion,
            &param );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR WMyDMoveGetConfigurationSync(
                  W_HANDLE hConnection,
                  uint8_t * pnStatusByte,
                  uint8_t * pnPasswordRetryCounter)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMyDMoveGetConfigurationSync");

   if ((pnStatusByte == null) || (pnPasswordRetryCounter == null))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMyDMoveGetConfiguration(
            hConnection,
            PBasicGenericSyncCompletionUint8Uint8,
            &param );
   }

   return PBasicGenericSyncWaitForResultUint8Uint8(&param, pnStatusByte, pnPasswordRetryCounter);
}

/* See Client API Specifications */
W_ERROR WMyDMoveSetConfigurationSync(
                  W_HANDLE hConnection,
                  uint8_t nStatusByte,
                  uint8_t nPasswordRetryCounter,
                  uint32_t nPassword,
                  bool_t bLockConfiguration)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMyDMoveSetConfigurationSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMyDMoveSetConfiguration(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            nStatusByte, nPasswordRetryCounter, nPassword, bLockConfiguration );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR WMyDMoveAuthenticateSync(
                  W_HANDLE hConnection,
                  uint32_t nPassword)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMyDMoveAuthenticateSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMyDMoveAuthenticate(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            nPassword );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* Polling command's callback */
static void static_PMyDNFCPollCompleted(
      tContext * pContext,
      void * pCallbackParameter,
      W_ERROR nError)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*) pCallbackParameter;

   PDebugTrace("static_PMyDNFCPollCompleted");

   /* Send the error */
   PDFCPostContext2(&pMyDConnection->sCallbackContext, nError);

   /* Release the reference after completion. May destroy the object */
   PHandleDecrementReferenceCount(pContext, pMyDConnection);
}

/* Send polling command */
static void static_PMyDNFCPoll(
      tContext * pContext,
      void * pObject,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tMyDConnection* pMyDConnection = (tMyDConnection*) pObject;

   PDebugTrace("static_PMyDNFCPoll");

   CDebugAssert( (pMyDConnection != null) && (pMyDConnection->nType == W_PROP_MY_D_NFC) );

   /* store the callback context */
   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &pMyDConnection->sCallbackContext);

   /* Increment the reference count to keep the connection object alive during the operation.
      The reference count is decreased in static_PMyDSendResult() when the operation is completed */
   PHandleIncrementReferenceCount(pMyDConnection);

   PSmartCacheInvalidateCache(
      pContext,
      pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
      0,
      1);

   PSmartCacheRead(
            pContext,
            pMyDConnection->uMyD.sNFC.pSmartCacheDynamic,
            0,
            1,
            pMyDConnection->aResponseBuffer,
            static_PMyDNFCPollCompleted,
            pMyDConnection);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
