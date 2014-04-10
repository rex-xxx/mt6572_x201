/*
 * Copyright (c) 2011-2012 Inside Secure, All Rights Reserved.
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
   Contains the implementation of the Kovio RFID functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( KOVIO_RFID )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_KOVIO_RFID_MAX_SIZE_BUFFER         32

/* Queued operation type */
#define P_KOVIO_RFID_QUEUED_NONE             0
#define P_KOVIO_RFID_QUEUED_READ             1
#define P_KOVIO_RFID_QUEUED_WRITE            2


extern tSmartCacheSectorSize g_sSectorSize4;


/* Declare a Kovio RFID exchange data structure */
typedef struct __tKovioRFIDConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;
   /* Connection handle */
   W_HANDLE                   hConnection;

   /* Connection information */
   uint8_t                    nUIDLength;
   uint8_t                    UID[7];

   /* Lock bytes */
   uint8_t                    aLockBytes[P_KOVIO_RFID_LOCK_BYTE_NUMBER];
   bool_t                     bLockBytesRetrieved;

   /* temporary data */
   uint8_t                    aCommandBuffer[P_KOVIO_RFID_MAX_SIZE_BUFFER ];
   uint8_t                    aResponseBuffer[P_KOVIO_RFID_MAX_SIZE_BUFFER];
   uint32_t                   nOffsetToRead;

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
      bool_t               bLockSectors;
      uint32_t             nPassword;
      uint8_t              nPendingConfigByte;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hOperation;
   } sQueuedOperation;

} tKovioRFIDConnection;

/**
 * @brief   Destroyes a Kovio RFID connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PKovioRFIDDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tKovioRFIDConnection* pKovioRFIDConnection = (tKovioRFIDConnection*)pObject;

   PDebugTrace("static_PKovioRFIDDestroyConnection");

   PDFCFlushCall(&pKovioRFIDConnection->sCallbackContext);

   /* Free the Kovio RFID connection structure */
   CMemoryFree( pKovioRFIDConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the Kovio RFID connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 **/

static uint32_t static_PKovioRFIDGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/* See PReaderExchangeData */
static void static_PKovioRFIDExchangeData(
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

   /* Mifare UL and Kovio RFID Cards - Do not ask for CRC Check for "WRITE", WRITE2B", "COMPATIBILITY WRITE" commands */

   if ((nReaderToCardBufferLength >= 1) && (pReaderToCardBuffer != null)  && (pReaderToCardBuffer[0] == 0xA2))
   {
      PDebugTrace("static_PKovioRFIDExchangeData: No CRC for writting command");
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
 * @brief   Gets the Kovio RFID connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PKovioRFIDGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   PDebugTrace("static_PKovioRFIDGetProperties");

   pPropertyArray[0] = W_PROP_KOVIO_RFID;

   return W_TRUE;
}

/**
 * @brief   Checkes the Kovio RFID connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PKovioRFIDCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   PDebugTrace("static_PKovioRFIDCheckProperties: nPropertyValue=%s (0x%02X)",
               PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue);

   return (W_PROP_KOVIO_RFID == nPropertyValue) ? W_TRUE : W_FALSE;
}


/* Handle registry KovioRFID connection type */
tHandleType g_sKovioRFIDConnection = { static_PKovioRFIDDestroyConnection,
                                       null,
                                       static_PKovioRFIDGetPropertyNumber,
                                       static_PKovioRFIDGetProperties,
                                       static_PKovioRFIDCheckProperties,
                                       null, null,
                                       static_PKovioRFIDExchangeData,
                                       null };


#define P_HANDLE_TYPE_KOVIO_RFID_CONNECTION (&g_sKovioRFIDConnection)

#define GetBit(value, bit)    (((value) >> (bit)) & 0x01)
#define SetBit(value, bit)    ((value) = (value) | (1 << (bit)))

/**
  * Retreive the lock status of a block by parsing the lock bytes of the card
  *
  * return W_TRUE if the block is locked
  */
static bool_t static_PKovioRFIDIsSectorLocked(
      tContext * pContext,
      tKovioRFIDConnection * pKovioRFIDConnection,
      uint32_t nSector)
{
   if (nSector <= 1)
   {
      /* Sectors [0 - 1] are locked */
      return W_TRUE;
   }

   if (nSector == 2)
   {
      return W_FALSE;
   }

   if (nSector == 3)
   {
      /* block 3 locks is located in aLockBytes[0] bit 0 */
      return GetBit(pKovioRFIDConnection->aLockBytes[0], 0);
   }

   if ((nSector >= 4) && (nSector <= 9))
   {
      /* block 4-9 locks are located in aLockBytes[0] bit 1 */
      return GetBit(pKovioRFIDConnection->aLockBytes[0], 1);
   }

   if ((nSector >= 10) && (nSector <= 15))
   {
      /* block 10-15 locks are located in aLockBytes[0] bit 2 */
      return GetBit(pKovioRFIDConnection->aLockBytes[0], 2);
   }

   if ( ((nSector >= 16) && (nSector <= 47)) ||
         nSector == 62)
   {
      /* block 16-47 and 62 locks are located in aLockBytes[7] bit 6 */
      return GetBit(pKovioRFIDConnection->aLockBytes[7], 6);
   }

   if ((nSector >= 48) && (nSector <= 63))
   {
      /* block 16-47 and 62 locks are located in aLockBytes[7] bit 7 */
      return GetBit(pKovioRFIDConnection->aLockBytes[7], 7);
   }

   /* should not occur */
   CDebugAssert(0);
   return W_TRUE;
}

static void static_PKovioRFIDLockSector(
      tContext * pContext,
      tKovioRFIDConnection * pKovioRFIDConnection,
      uint32_t         nSector)
{
   if (3 == nSector)
   {
      /* block 0 locks is located in aLockBytes[0] bit 0*/
      SetBit(pKovioRFIDConnection->aLockBytes[0], 0);
      return;
   }

   if ((nSector >= 4) && (nSector <= 9))
   {
      /* block 4-9 locks are located in aLockBytes[0] bit 1 */
      SetBit(pKovioRFIDConnection->aLockBytes[0], 1);
      return;
   }

   if ((nSector >= 10) && (nSector <= 15))
   {
      /* block 10-15 locks are located in aLockBytes[0] bit 2 */
      SetBit(pKovioRFIDConnection->aLockBytes[0], 2);
      return;
   }

   if ( ((nSector >= 16) && (nSector <= 47)) ||
         nSector == 62)
   {
      /* block 16-47 and 62 locks are located in aLockBytes[7] bit 6 */
      SetBit(pKovioRFIDConnection->aLockBytes[7], 6);
      return;
   }

   if ((nSector >= 48) && (nSector <= 63))
   {
      /* block 16-47 and 62 locks are located in aLockBytes[7] bit 7 */
      SetBit(pKovioRFIDConnection->aLockBytes[7], 7);
      return;
   }

   /* should not occur */
   CDebugAssert(0);
}

/**
  * Retreive the lockable status of a sector by parsing the lock bytes of the card
  *
  * return W_TRUE if the sector is lockable
  */
static bool_t static_PKovioRFIDIsSectorLockable(
      tContext * pContext,
      tKovioRFIDConnection * pKovioRFIDConnection,
      uint32_t nSector)
{
   return (nSector < 3) ? W_FALSE
                       : W_TRUE;
}


/**
 * @brief   Sends the result.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pKovioRFIDConnection  The KovioRFID connection.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PKovioRFIDSendResult(
            tContext* pContext,
            tKovioRFIDConnection * pKovioRFIDConnection,
            W_ERROR nError )
{
   PDebugTrace("static_PKovioRFIDSendResult");

   if (pKovioRFIDConnection->hOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pKovioRFIDConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PKovioRFIDSendResult: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pKovioRFIDConnection->hOperation);
      PHandleClose(pContext, pKovioRFIDConnection->hOperation);
      pKovioRFIDConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the error */
   PDFCPostContext2(&pKovioRFIDConnection->sCallbackContext, nError);

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, pKovioRFIDConnection->hConnection);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pKovioRFIDConnection);
}


static void static_PKovioRFIDLockSectorAutomaton(
            tContext* pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tKovioRFIDConnection* pKovioRFIDConnection = (tKovioRFIDConnection*)pCallbackParameter;
   uint32_t nBlockStart = 0, nBlockEnd = 0, nIndex = 0;
   PDebugTrace("static_PKovioRFIDLockSectorAutomaton");

   if( nError != W_SUCCESS)
   {
      PDebugError("static_PKovioRFIDLockSectorAutomaton error %d", nError);
      static_PKovioRFIDSendResult(pContext, pKovioRFIDConnection, nError);
      return;
   }

   switch(pKovioRFIDConnection->nCurrentOperationState)
   {
   case 0:
         nBlockStart = pKovioRFIDConnection->pSectorSize->pDivide(pKovioRFIDConnection->nOffsetToLock);
         nBlockEnd   = pKovioRFIDConnection->pSectorSize->pDivide(pKovioRFIDConnection->nLengthToLock + pKovioRFIDConnection->nOffsetToLock - 1);

         for( nIndex = nBlockStart; nIndex <= nBlockEnd; nIndex ++ )
         {
            static_PKovioRFIDLockSector(pContext, pKovioRFIDConnection, nIndex);
         }

         pKovioRFIDConnection->nLengthToLock = 0;

         PNDEF2GenWrite(pContext,
                     pKovioRFIDConnection->hConnection,
                     static_PKovioRFIDLockSectorAutomaton,
                     pKovioRFIDConnection,
                     &pKovioRFIDConnection->aLockBytes[0],
                     (P_NDEF2GEN_STATIC_LOCK_BLOCK *P_NDEF2GEN_BLOCK_SIZE) + P_NDEF2GEN_STATIC_LOCK_BYTE_OFFSET,
                     P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH);
         break;

      case 1:
         /* Lock Byte 0-1 written & write lock Byte 2-7 */
         PNDEF2GenWrite(pContext,
                     pKovioRFIDConnection->hConnection,
                     static_PKovioRFIDLockSectorAutomaton,
                     pKovioRFIDConnection,
                     &pKovioRFIDConnection->aLockBytes[2],
                     P_KOVIO_RFID_LOCK_BLOCK * P_NDEF2GEN_BLOCK_SIZE,
                     P_KOVIO_RFID_LOCK_LENGTH);
         break;

      case 2:
         /* Lock byte 2-7 written & post callback */
         static_PKovioRFIDSendResult(pContext, pKovioRFIDConnection, W_SUCCESS);
         break;

      default:
         PDebugError("static_PKovioRFIDLockSectorAutomaton error %d", W_ERROR_BAD_STATE);
         static_PKovioRFIDSendResult(pContext, pKovioRFIDConnection, W_ERROR_BAD_STATE);
         return;
   }

   pKovioRFIDConnection->nCurrentOperationState ++;
}


/**
 * Bytes have been read
 */
static void static_PKovioRFIDReadCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tKovioRFIDConnection* pKovioRFIDConnection = (tKovioRFIDConnection*)pCallbackParameter;

   PDebugTrace("static_PKovioRFIDReadCompleted");

   static_PKovioRFIDSendResult(pContext, pKovioRFIDConnection, nError );
}

/**
 * Bytes have been written
 */
static void static_PKovioRFIDWriteCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tKovioRFIDConnection* pKovioRFIDConnection = (tKovioRFIDConnection*)pCallbackParameter;

   PDebugTrace("static_PKovioRFIDWriteCompleted");

   if(nError == W_SUCCESS && pKovioRFIDConnection->nLengthToLock != 0)
   {
      pKovioRFIDConnection->nCurrentOperationState = 0;
      static_PKovioRFIDLockSectorAutomaton(pContext, pKovioRFIDConnection, W_SUCCESS);
      return;
   }

   /* Send the result */
   static_PKovioRFIDSendResult( pContext, pKovioRFIDConnection, nError );
}


/* Automaton used during connection creation */
static void static_PKovioRFIDCreateConnectionAutomaton(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tKovioRFIDConnection* pKovioRFIDConnection = (tKovioRFIDConnection*)pCallbackParameter;

   PDebugTrace("static_PKovioRFIDCreateConnectionAutomaton");

   if (W_SUCCESS != nError)
   {
      goto end;
   }

   switch (pKovioRFIDConnection->nCurrentOperationState)
   {
      case 0:
         /* Add the Kovio RFID connection structure */
         if ( ( nError = PHandleAddHeir(
                           pContext,
                           pKovioRFIDConnection->hConnection,
                           pKovioRFIDConnection,
                           P_HANDLE_TYPE_KOVIO_RFID_CONNECTION) ) != W_SUCCESS )
         {
            PDebugError("static_PKovioRFIDCreateConnectionAutomaton: error returned by PHandleAddHeir()");
            /* Free pKovioRFIDConnection here since PHandleAddHeir has not been called yet
               thus pKovioRFIDConnection cannot free it */
            CMemoryFree(pKovioRFIDConnection);
            goto end;
         }

         /* Create the dynamic smart cache */
         if ( (nError = PNDEF2GenCreateSmartCacheDynamic(
                           pContext,
                           pKovioRFIDConnection->hConnection,
                           pKovioRFIDConnection->nSectorNumber)) != W_SUCCESS)
         {
            PDebugError("static_PKovioRFIDCreateConnectionAutomaton : PNDEF2GenCreateSmartCacheDynamic returned %s", PUtilTraceError(nError));
            goto end;
         }

         /* Read config byte and lock bytes 0-1 */
         PNDEF2GenRead(pContext,
                       pKovioRFIDConnection->hConnection,
                       static_PKovioRFIDCreateConnectionAutomaton,
                       pKovioRFIDConnection,
                       &pKovioRFIDConnection->aLockBytes[0],
                       (P_NDEF2GEN_STATIC_LOCK_BLOCK * P_NDEF2GEN_BLOCK_SIZE) + P_NDEF2GEN_STATIC_LOCK_BYTE_OFFSET, /* Page containing lock byte 0 and 1 */
                       P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH);
         break;

      case 1:
         /* Retrieve lock bytes 0-1 */

         /*Now read lock bytes 2-7 */
         PNDEF2GenRead(pContext,
                        pKovioRFIDConnection->hConnection,
                        static_PKovioRFIDCreateConnectionAutomaton,
                        pKovioRFIDConnection,
                        &pKovioRFIDConnection->aLockBytes[P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH],
                        P_KOVIO_RFID_LOCK_BLOCK * P_NDEF2GEN_BLOCK_SIZE,
                        P_KOVIO_RFID_LOCK_LENGTH);
         break;

      case 2:
         /* All lock byte are retrieved */
         pKovioRFIDConnection->bLockBytesRetrieved = W_TRUE;

         /* All bytes have been read */
         nError = W_SUCCESS;
         goto end;
   }

   pKovioRFIDConnection->nCurrentOperationState++;
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

   PDFCPostContext2(&pKovioRFIDConnection->sCallbackContext, nError);
}


/* See Header file */
void PKovioRFIDCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty )
{
   tKovioRFIDConnection* pKovioRFIDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PKovioRFIDCreateConnection");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the NDEF buffer */
   pKovioRFIDConnection = (tKovioRFIDConnection*)CMemoryAlloc( sizeof(tKovioRFIDConnection) );
   if ( pKovioRFIDConnection == null )
   {
      PDebugError("PKovioRFIDCreateConnection: pKovioRFIDConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }

   CMemoryFill(pKovioRFIDConnection, 0, sizeof(tKovioRFIDConnection));

   /* Get the 14443-3 information level */
   if ( ( nError = P14P3UserCheckKovioRFID(
                     pContext,
                     hConnection,
                     pKovioRFIDConnection->UID,
                     &pKovioRFIDConnection->nUIDLength) ) != W_SUCCESS )
   {
      PDebugLog("PKovioRFIDCreateConnection: not a Kovio RFID card");
      goto return_error;
   }

   /* Store the connection information */
   pKovioRFIDConnection->hConnection = hConnection;

   /* Store the callback context */
   pKovioRFIDConnection->sCallbackContext = sCallbackContext;

   /* Begining of automaton */
   pKovioRFIDConnection->nCurrentOperationState = 0;

   pKovioRFIDConnection->pSectorSize   = &g_sSectorSize4;
   pKovioRFIDConnection->nSectorNumber = P_KOVIO_RFID_BLOCK_NUMBER;
   static_PKovioRFIDCreateConnectionAutomaton(pContext, pKovioRFIDConnection, W_SUCCESS);

   return;

return_error:

   if(pKovioRFIDConnection != null)
   {
      CMemoryFree(pKovioRFIDConnection);
   }

   PDFCPostContext2(&sCallbackContext, nError);
}

/** See tPReaderUserRemoveSecondaryConnection */
void PKovioRFIDRemoveConnection(
            tContext* pContext,
            W_HANDLE hUserConnection )
{
   tKovioRFIDConnection* pKovioRFIDConnection = (tKovioRFIDConnection*)PHandleRemoveLastHeir(
            pContext, hUserConnection,
            P_HANDLE_TYPE_KOVIO_RFID_CONNECTION);

   PDebugTrace("PKovioRFIDRemoveConnection");

   /* Remove the connection object */
   if(pKovioRFIDConnection != null)
   {
      CMemoryFree(pKovioRFIDConnection);
   }
}

/* See Header file */
W_ERROR PKovioRFIDCheckType2(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumSpaceSize,
            uint8_t* pnSectorSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable )
{
   tKovioRFIDConnection* pKovioRFIDConnection;
   uint32_t nIndex;
   W_ERROR nError;
   bool_t    bIsLocked = W_FALSE;
   bool_t    bIsLockable = W_TRUE;

   PDebugTrace("PKovioRFIDCheckType2");

   /* Reset the maximum tag size */
   if (pnMaximumSpaceSize != null) *pnMaximumSpaceSize = 0;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_KOVIO_RFID_CONNECTION, (void**)&pKovioRFIDConnection);
   if ( nError == W_SUCCESS )
   {
      if (pnMaximumSpaceSize != null)
      {
         *pnMaximumSpaceSize = pKovioRFIDConnection->pSectorSize->pMultiply(P_KOVIO_RFID_LAST_DATA_BLOCK - P_KOVIO_RFID_FIRST_DATA_BLOCK + 1);
      }

      /* Go through the lock byte */
      for (nIndex=4; nIndex <= P_KOVIO_RFID_LAST_DATA_BLOCK; nIndex++)
      {
         bIsLocked |= static_PKovioRFIDIsSectorLocked(pContext, pKovioRFIDConnection, nIndex);
         bIsLockable &= static_PKovioRFIDIsSectorLockable(pContext, pKovioRFIDConnection, nIndex);
      }

      bIsLocked = W_FALSE;
      if (pbIsLocked != null) *pbIsLocked = bIsLocked;

      if (pbIsLockable != null) *pbIsLockable = bIsLockable;
      if (pnSectorSize != null) *pnSectorSize = (uint8_t)pKovioRFIDConnection->pSectorSize->nValue;
      if (pbIsFormattable != null) *pbIsFormattable = ! bIsLocked;


      return W_SUCCESS;
   }

   return nError;
}

/* See header file */
W_ERROR PKovioRFIDNDEF2Lock(tContext * pContext,
                      W_HANDLE hConnection)
{
   tKovioRFIDConnection* pKovioRFIDConnection = null;
   W_ERROR nError;

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_KOVIO_RFID_CONNECTION, (void**)&pKovioRFIDConnection);
   if (nError != W_SUCCESS)
   {
      PDebugError("PKovioRFIDNDEF2Lock : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      return nError;
   }

   /* Update lock bytes */
   CMemoryFill(&pKovioRFIDConnection->aLockBytes[0], 0xFF, 1);
   CMemoryFill(&pKovioRFIDConnection->aLockBytes[7], 0xFF, 1);

   return W_SUCCESS;
}

typedef struct
{
   uint8_t  UID[7];
   uint16_t nSectorSize;
   uint16_t nSectorNumber;
} tWKovioRFIDConnectionInfo;

/* See Client API Specifications */
W_ERROR PKovioRFIDGetConnectionInfo(
            tContext* pContext,
            W_HANDLE hConnection,
            tWKovioRFIDConnectionInfo *pConnectionInfo )
{
   tKovioRFIDConnection* pKovioRFIDConnection;
   W_ERROR nError;

   PDebugTrace("PKovioRFIDGetConnectionInfo");

   /* Check the parameters */
   if ( pConnectionInfo == null )
   {
      PDebugError("PKovioRFIDGetConnectionInfo: pConnectionInfo == null");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_KOVIO_RFID_CONNECTION, (void**)&pKovioRFIDConnection);
   if ( nError == W_SUCCESS )
   {
      /* UID */
      CMemoryCopy(
         pConnectionInfo->UID,
         pKovioRFIDConnection->UID,
         pKovioRFIDConnection->nUIDLength );
      /* Sector size */
      pConnectionInfo->nSectorSize = (uint16_t) pKovioRFIDConnection->pSectorSize->nValue;
      /* Sector number */
      pConnectionInfo->nSectorNumber = (uint16_t)pKovioRFIDConnection->nSectorNumber;
      return W_SUCCESS;
   }
   else
   {
      PDebugError("PKovioRFIDGetConnectionInfo: could not get pKovioRFIDConnection buffer");

      /* Fill in the structure with zeros */
      CMemoryFill(pConnectionInfo, 0, sizeof(tWMifareConnectionInfo));

      return nError;
   }
}



static void static_PKovioExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tKovioRFIDConnection* pKovioRFIDConnection = (tKovioRFIDConnection*) pObject;

   PDebugTrace("static_PKovioExecuteQueuedExchange");

   /* Restore operation handle */
   pKovioRFIDConnection->hOperation = pKovioRFIDConnection->sQueuedOperation.hOperation;
   /* Restore callback context */
   pKovioRFIDConnection->sCallbackContext = pKovioRFIDConnection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (pKovioRFIDConnection->hOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pKovioRFIDConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PKovioExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_PKovioRFIDSendResult(pContext, pKovioRFIDConnection, nResult);
   }
   else
   {
      switch (pKovioRFIDConnection->sQueuedOperation.nType)
      {
         case P_KOVIO_RFID_QUEUED_READ:
            PNDEF2GenRead(pContext,
                          pKovioRFIDConnection->hConnection,
                          static_PKovioRFIDReadCompleted,
                          pKovioRFIDConnection,
                          pKovioRFIDConnection->sQueuedOperation.pBuffer,
                          pKovioRFIDConnection->sQueuedOperation.nOffset,
                          pKovioRFIDConnection->sQueuedOperation.nLength);

         break;

         case P_KOVIO_RFID_QUEUED_WRITE:
            /* Store the exchange information */
            if (pKovioRFIDConnection->sQueuedOperation.bLockSectors != W_FALSE)
            {
               pKovioRFIDConnection->nOffsetToLock = pKovioRFIDConnection->sQueuedOperation.nOffset;
               pKovioRFIDConnection->nLengthToLock = pKovioRFIDConnection->sQueuedOperation.nLength;

               if (pKovioRFIDConnection->sQueuedOperation.pBuffer == null)
               {
                  /* You must write nothing but just locking */
                  static_PKovioRFIDWriteCompleted(pContext, pKovioRFIDConnection, W_SUCCESS);
                  break;
               }
            }
            else
            {
               pKovioRFIDConnection->nOffsetToLock = 0;
               pKovioRFIDConnection->nLengthToLock = 0;
            }

            PNDEF2GenWrite(pContext,
                           pKovioRFIDConnection->hConnection,
                           static_PKovioRFIDWriteCompleted,
                           pKovioRFIDConnection,
                           pKovioRFIDConnection->sQueuedOperation.pBuffer,
                           pKovioRFIDConnection->sQueuedOperation.nOffset,
                           pKovioRFIDConnection->sQueuedOperation.nLength);


         break;

      default:
         /* Return an error */
         PDebugError("static_PKovioExecuteQueuedExchange: unknown type of operation!");
         static_PKovioRFIDSendResult(pContext, pKovioRFIDConnection, W_ERROR_BAD_STATE);
      }
   }

   /* Reset data */
   CMemoryFill(&pKovioRFIDConnection->sQueuedOperation, 0, sizeof(pKovioRFIDConnection->sQueuedOperation));
}

/* See Client API Specifications */
void PKovioRFIDRead(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            W_HANDLE *phOperation )
{
   tKovioRFIDConnection* pKovioRFIDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nError;

   PDebugTrace("PKovioRFIDRead");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_KOVIO_RFID_CONNECTION, (void**)&pKovioRFIDConnection);
   if (pKovioRFIDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PKovioRFIDRead: Bad handle");
      goto return_error;
   }

   /* Check parameters */
   if ( (null == pBuffer) || (0 == nLength) )
   {
      PDebugError("PKovioRFIDRead: null parameter");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( (nOffset + nLength) > pKovioRFIDConnection->pSectorSize->pMultiply(pKovioRFIDConnection->nSectorNumber) )
   {
      PDebugError("PKovioRFIDRead: the data to read/write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PKovioRFIDRead: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PKovioRFIDRead: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PKovioExecuteQueuedExchange, pKovioRFIDConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PKovioRFIDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pKovioRFIDConnection);

      /* Store the operation handle */
      CDebugAssert(pKovioRFIDConnection->hOperation == W_NULL_HANDLE);
      pKovioRFIDConnection->hOperation = hOperation;

      /* Store the callback context */
      pKovioRFIDConnection->sCallbackContext = sCallbackContext;

      PNDEF2GenRead(pContext,
                     hConnection,
                     static_PKovioRFIDReadCompleted,
                     pKovioRFIDConnection,
                     pBuffer,
                     nOffset,
                     nLength);


      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PKovioRFIDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pKovioRFIDConnection);

      /* Save the operation handle */
      CDebugAssert(pKovioRFIDConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pKovioRFIDConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pKovioRFIDConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pKovioRFIDConnection->sQueuedOperation.nType = P_KOVIO_RFID_QUEUED_READ;

      /* Save data */
      pKovioRFIDConnection->sQueuedOperation.pBuffer = pBuffer;
      pKovioRFIDConnection->sQueuedOperation.nOffset = nOffset;
      pKovioRFIDConnection->sQueuedOperation.nLength = nLength;
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
   PDebugError("PKovioRFIDRead: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
void PKovioRFIDWrite(
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
   tKovioRFIDConnection* pKovioRFIDConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nError;
   uint32_t nIndex;

   PDebugTrace("PKovioRFIDWrite");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_KOVIO_RFID_CONNECTION, (void**)&pKovioRFIDConnection);
   if (pKovioRFIDConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PKovioRFIDWrite: Bad handle");
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockSectors == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("PKovioRFIDWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockSectors == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("PKovioRFIDWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( (nOffset + nLength) > pKovioRFIDConnection->pSectorSize->pMultiply(pKovioRFIDConnection->nSectorNumber) )
   {
      PDebugError("PKovioRFIDWrite: the data to read/write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( (nLength > 0) && (nOffset < pKovioRFIDConnection->pSectorSize->pMultiply(P_KOVIO_RFID_FIRST_DATA_BLOCK)) )
   {
      PDebugError("PKovioRFIDWrite: writing of Serial Number or Lock bytes is not allowed. Use the specialized API for locking !");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check if the Kovio card is locked/lockable */
   for (nIndex = pKovioRFIDConnection->pSectorSize->pDivide(nOffset); nIndex <= pKovioRFIDConnection->pSectorSize->pDivide(nOffset + nLength - 1); nIndex++)
   {
      if ((pBuffer != null) && (static_PKovioRFIDIsSectorLocked(pContext, pKovioRFIDConnection, nIndex) != W_FALSE))
      {
         PDebugError("PMPKovioRFIDWriteyDWrite: item locked");
         nError = W_ERROR_ITEM_LOCKED;
         goto return_error;
      }

      if ((bLockSectors != W_FALSE) && (static_PKovioRFIDIsSectorLockable(pContext, pKovioRFIDConnection, nIndex) == W_FALSE))
      {
         PDebugError("PKovioRFIDWrite: item not lockable");
         nError = W_ERROR_ITEM_LOCKED;
         goto return_error;
      }
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PKovioRFIDWrite: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PKovioRFIDWrite: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PKovioExecuteQueuedExchange, pKovioRFIDConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PKovioRFIDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pKovioRFIDConnection);

      /* Store the operation handle */
      CDebugAssert(pKovioRFIDConnection->hOperation == W_NULL_HANDLE);
      pKovioRFIDConnection->hOperation = hOperation;

      /* Store the callback context */
      pKovioRFIDConnection->sCallbackContext = sCallbackContext;

      /* Store the exchange information */
      if (bLockSectors != W_FALSE)
      {
         pKovioRFIDConnection->nOffsetToLock = nOffset;
         pKovioRFIDConnection->nLengthToLock = nLength;

         if (pBuffer == null)
         {
            /* You must write nothing but just locking */
            static_PKovioRFIDWriteCompleted(pContext, pKovioRFIDConnection, W_SUCCESS);
            break;
         }
      }
      else
      {
         pKovioRFIDConnection->nOffsetToLock = 0;
         pKovioRFIDConnection->nLengthToLock = 0;
      }

      PNDEF2GenWrite(pContext,
                     hConnection,
                     static_PKovioRFIDWriteCompleted,
                     pKovioRFIDConnection,
                     pBuffer,
                     nOffset,
                     nLength);
      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PKovioRFIDSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pKovioRFIDConnection);

      /* Save the operation handle */
      CDebugAssert(pKovioRFIDConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pKovioRFIDConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pKovioRFIDConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pKovioRFIDConnection->sQueuedOperation.nType = P_KOVIO_RFID_QUEUED_WRITE;

      /* Save data */
      pKovioRFIDConnection->sQueuedOperation.pBuffer = (uint8_t*)pBuffer;
      pKovioRFIDConnection->sQueuedOperation.nOffset = nOffset;
      pKovioRFIDConnection->sQueuedOperation.nLength = nLength;
      pKovioRFIDConnection->sQueuedOperation.bLockSectors = bLockSectors;

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
   PDebugError("PKovioRFIDWrite: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
