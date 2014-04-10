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
   Contains the implementation of the Mifare functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( MIFA )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* The Mifare UL is organised 16 blocks of 4 bytes */
#define P_MIFARE_BLOCK_NUMBER_UL                   16
#define P_MIFARE_BLOCK_SIZE_UL                     P_NDEF2GEN_BLOCK_SIZE

/* The Mifare UL_C is organised 48 blocks of 4 bytes */
#define P_MIFARE_BLOCK_NUMBER_UL_C                 48
#define P_MIFARE_BLOCK_SIZE_UL_C                   P_NDEF2GEN_BLOCK_SIZE
#define P_MIFARE_BLOCK_NUMBER_DATA_UL_C            44

/* The Mifare 1K is organised in 5 sectors with 4 blocks  of 16 bytes, */
#define P_MIFARE_BLOCK_NUMBER_MINI                 (5 * 4)
#define P_MIFARE_BLOCK_SIZE_MINI                   16

/* The Mifare 1K is organised in 16 sectors with 4 blocks, sector trailer on block 3, of 16 bytes, */
/* with manufacturer data on block 0 of sector 0 */
#define P_MIFARE_BLOCK_NUMBER_1K                  (16 * 4)
#define P_MIFARE_BLOCK_SIZE_1K                     16

/* The Mifare 4K is organised in 32 sectors with 4 blocks, sector trailer on block 3, of 16 bytes */
/* and 8 sectors with 16 blocks of 16 bytes, */
/* with manufacturer data on block 0 of sector 0 */
#define P_MIFARE_BLOCK_NUMBER_4K                  (32 * 4 + 8 * 16)
#define P_MIFARE_BLOCK_SIZE_4K                     16

/* The Mifare Plus 2K is organised in 32 sectors with 4 blocks if 16 bytes */
#define P_MIFARE_BLOCK_NUMBER_PLUS_2K             (32 * 4)
#define P_MIFARE_BLOCK_SIZE_PLUS_2K                16

/* The Mifare Plus 4K is organised in 32 sectors with 4 blocks of 16 bytes
   followed with 8 sectors of 16 blocks of 16 bytes....
   as an approximation, report 64 sectors of 4 blocks of 16 bytes */

#define P_MIFARE_BLOCK_NUMBER_PLUS_4K             (32 * 4 + 8 * 16)
#define P_MIFARE_BLOCK_SIZE_PLUS_4K                16

/* Mifare block info */
#define P_MIFARE_ULC_LOCK_BLOCK              0x28
#define P_MIFARE_ULC_LOCK_LENGTH             2

#define P_MIFARE_FIRST_DATA_BLOCK            (P_NDEF2GEN_STATIC_LOCK_BLOCK + 1)
#define P_MIFARE_UL_LAST_DATA_BLOCK          (P_MIFARE_BLOCK_NUMBER_UL - 1)
#define P_MIFARE_ULC_LAST_DATA_BLOCK         (P_MIFARE_ULC_LOCK_BLOCK - 1)

#define P_MIFARE_ULC_AUTH0_BLOCK             0x2A
#define P_MIFARE_ULC_AUTH1_BLOCK             0x2B
#define P_MIFARE_ULC_KEY_BLOCK               0x2C

/* Queued operation type */
#define P_MIFARE_QUEUED_NONE                    0
#define P_MIFARE_QUEUED_READ                    1
#define P_MIFARE_QUEUED_WRITE                   2
#define P_MIFARE_QUEUED_AUTHENTICATE            3
#define P_MIFARE_QUEUED_SET_ACCESS_RIGHTS       4
#define P_MIFARE_QUEUED_RETRIEVE_ACCESS_RIGHTS  5
#define P_MIFARE_QUEUED_FREEZE_LOCK_CONF        6

/*cache Connection defines*/
#define P_MIFARE_IDENTIFIER_LEVEL            ZERO_IDENTIFIER_LEVEL

#ifndef P_MIFARE_UL_C_DEFAULT_KEY
#define P_MIFARE_UL_C_DEFAULT_KEY            { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F }
#endif  /* P_MIFARE_UL_C_DEFAULT_KEY */

static const uint8_t g_MifareULCDefaultKey[] = P_MIFARE_UL_C_DEFAULT_KEY;
static const uint8_t g_MifareULCAuthenticateCommand[] = { 0x1A, 0x00 };

static const uint8_t g_MifareDesfireGetVersion[]                = { 0x90, 0x60, 0x00, 0x00, 0x00 };
static const uint8_t g_MifareDesfireGetNextVersion[]            = { 0x90, 0xAF, 0x00, 0x00, 0x00 };
static const uint8_t g_MifareDesfireGetVersionAnswerMask[]      = { 0xFF, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0xFF  };
static const uint8_t g_MifareDesfireGetVersionAnswerMasked[]    = { 0x04, 0x01, 0x01, 0x00, 0x00, 0x00, 0x05, 0x91, 0xAF  };

/* Declare a Mifare exchange data structure */
typedef struct __tMifareConnection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;
   /* Connection handle */
   W_HANDLE                   hConnection;

   /* Connection information */
   uint8_t                    nUIDLength;
   uint8_t                    UID[7];

   uint8_t                    aLockBytes[4];
   uint8_t                    nAuthenticateMode;
   uint8_t                    nAuthenticateThreshold;
   bool_t                     bULCAccessRightsRetrieved;

   /* Use for bypassing the accessRightsRetrieved */
   bool_t                     bBypassULCAccessRightsRetrieved;

   /* a temporary buffer used for authentication set processing */
   uint8_t                    aTempBuffer[32];
   uint8_t                    aKey[16];
   uint8_t                    aIvec[8];
   uint8_t                    aRandom[8];

   uint8_t                    nRequestedAuthenticateMode;
   uint8_t                    nRequestedAuthenticateThreshold;
   bool_t                       bLockAuthentication;
   uint32_t                   nCurrentOperationState;

   tSmartCacheSectorSize*     pSectorSize;
   uint32_t                   nSectorNumber;

   uint32_t                   nSectorSize;

   /* Type of the card (UL, 1K, 4K, Desfire) */
   uint8_t                    nType;

   /* Command informtion */
   uint32_t                   nOffsetToLock;
   uint32_t                   nLengthToLock;

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
      const uint8_t*       pKey;
      uint8_t              nRequestedAuthenticateMode;
      uint8_t              nRequestedAuthenticateThreshold;
      bool_t                 bLockAuthentication;
      /* Callback context */
      tDFCCallbackContext  sCallbackContext;
      /* Operation handle */
      W_HANDLE             hOperation;
   } sQueuedOperation;


#ifdef P_INCLUDE_MIFARE_CLASSIC
   /*----- For Mifare Classic ----- */
   bool_t   bMifareClassicAuthenticated;
   bool_t   bUseKeyA;
   uint8_t  nMifareClassicAuthenticatedSectorNumber;
   uint8_t  nMifareClassicOperation;
#endif /* P_INCLUDE_MIFARE_CLASSIC */

} tMifareConnection;

/* Declare a Mifare 4A exchange data structure */
typedef struct __tMifareConnection4A
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;

   /* Type of the card (Desfire) */
   uint8_t                    nType;

   uint8_t                    nUIDLength;
   uint8_t                    UID[7];

   /* get Version command related stuff */
   uint8_t                    nState;
   W_HANDLE                   hConnection;
   tDFCCallbackContext        sCallbackContext;
   uint8_t                    aBuffer[32];

} tMifareConnection4A;

/**
 * @brief   Destroyes a Mifare connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PMifareDestroyConnection4A(
            tContext* pContext,
            void* pObject )
{
   tMifareConnection4A* pMifareConnection4A = (tMifareConnection4A*)pObject;

   PDebugTrace("static_PMifareDestroyConnection4A");

   CMemoryFree( pMifareConnection4A );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the Mifare connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static uint32_t static_PMifareGetPropertyNumber4A(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/**
 * @brief   Gets the Mifare connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PMifareGetProperties4A(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tMifareConnection4A* pMifareConnection4A = (tMifareConnection4A*)pObject;

   PDebugTrace("static_PMifareGetProperties4A");
   pPropertyArray[0] = pMifareConnection4A->nType;
   return W_TRUE;
}

/**
 * @brief   Checkes the Mifare connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PMifareCheckProperties4A(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tMifareConnection4A* pMifareConnection4A = (tMifareConnection4A*)pObject;

   PDebugTrace(
      "static_PMifareCheckProperties4A: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   return ( nPropertyValue == pMifareConnection4A->nType )?W_TRUE:W_FALSE;
}

/* Handle registry Mifare connection type */
tHandleType g_sMifareConnection4A = { static_PMifareDestroyConnection4A,
                                    null,
                                    static_PMifareGetPropertyNumber4A,
                                    static_PMifareGetProperties4A,
                                    static_PMifareCheckProperties4A,
                                    null, null, null, null };

#define P_HANDLE_TYPE_MIFARE_CONNECTION_4_A (&g_sMifareConnection4A)

/**
 * @brief   Destroyes a Mifare connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PMifareDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tMifareConnection* pMifareConnection = (tMifareConnection*)pObject;

   PDebugTrace("static_PMifareDestroyConnection");

   PDFCFlushCall(&pMifareConnection->sCallbackContext);

   /* Free the Mifare connection structure */
   CMemoryFree( pMifareConnection );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the Mifare connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 **/

static uint32_t static_PMifareGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{

   tMifareConnection* pMifareConnection = (tMifareConnection*)pObject;

   if (pMifareConnection->nType == W_PROP_MIFARE_UL_C)
   {
      /* special case for Mifare UL-C, return two properties : W_PROP_MIFARE_UL and W_PROP_MIFARE_UL_C */
      return 2;
   }
   else
   {
      return 1;
   }
}

/* See PReaderExchangeData */
static void static_PMifareExchangeData(
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

   /* retrieve the context */
   tMifareConnection * pMifareConnection = (tMifareConnection *) pObject;

   if( ( pMifareConnection->nType == W_PROP_MIFARE_1K )
    || ( pMifareConnection->nType == W_PROP_MIFARE_4K ) )
   {
      PDebugError("Use Mifare Classic");
      P14Part3ExchangeRawMifare(pContext,
                                pMifareConnection->hConnection,
                                pCallback,
                                pCallbackParameter,
                                pReaderToCardBuffer,
                                nReaderToCardBufferLength,
                                pCardToReaderBuffer,
                                nCardToReaderBufferMaxLength,
                                phOperation);
      return;
   }

   /* Mifare UL and My-d Cards - Do not ask for CRC Check for "WRITE", WRITE2B", "COMPATIBILITY WRITE" commands */

   if ((nReaderToCardBufferLength >= 1) && (pReaderToCardBuffer != null) &&
       ((pReaderToCardBuffer[0] == 0xA2) || (pReaderToCardBuffer[0] == 0xA1) || (pReaderToCardBuffer[0] == 0xA0)))
   {
      PDebugTrace("static_PMifareExchangeData: No CRC for Mifare UL and MyD WRITE operation");
      bCheckResponseCRC = W_FALSE;
      bCheckAckOrNack = W_TRUE;
   }

   /**
    * For mifare ULC
    **/
   if(pMifareConnection->bBypassULCAccessRightsRetrieved == W_TRUE)
   {
      pMifareConnection->bBypassULCAccessRightsRetrieved = W_FALSE;
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
 * @brief   Gets the Mifare connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PMifareGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tMifareConnection* pMifareConnection = (tMifareConnection*)pObject;

   PDebugTrace("static_PMifareGetProperties");

   if (pMifareConnection->nType == W_PROP_MIFARE_UL_C)
   {
      /* special case for Mifare UL-C, return two properties : W_PROP_MIFARE_UL and W_PROP_MIFARE_UL_C */
      pPropertyArray[0] = W_PROP_MIFARE_UL_C;
      pPropertyArray[1] = W_PROP_MIFARE_UL;
   }
   else
   {
      pPropertyArray[0] = pMifareConnection->nType;
   }

   return W_TRUE;
}

/**
 * @brief   Checkes the Mifare connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PMifareCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tMifareConnection* pMifareConnection = (tMifareConnection*)pObject;

   PDebugTrace(
      "static_PMifareCheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if (pMifareConnection->nType == W_PROP_MIFARE_UL_C)
   {
      /* special case for Mifare UL-C, return two properties : W_PROP_MIFARE_UL and W_PROP_MIFARE_UL_C */
      if ((nPropertyValue == W_PROP_MIFARE_UL_C) || (nPropertyValue == W_PROP_MIFARE_UL))
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
      /* standard case */

      return ( nPropertyValue == pMifareConnection->nType )? W_TRUE : W_FALSE;
   }
}

/* Handle registry Mifare connection type */
tHandleType g_sMifareConnection = { static_PMifareDestroyConnection,
                                    null,
                                    static_PMifareGetPropertyNumber,
                                    static_PMifareGetProperties,
                                    static_PMifareCheckProperties,
                                    null, null,
                                    static_PMifareExchangeData,
                                    null };

#define P_HANDLE_TYPE_MIFARE_CONNECTION (&g_sMifareConnection)


#define GetBit(value, bit)    (((value) >> (bit)) & 0x01)
#define SetBit(value, bit)    ((value) = (value) | (1 << (bit)))

/**
  * Retreive the lock status of a sector by parsing the lock bytes of the card
  *
  * return W_TRUE if the sector is locked
  */

static bool_t static_PMifareIsSectorLocked(
      tContext * pContext,
      tMifareConnection * pMifareConnection,
      uint32_t            nSector)
{
   CDebugAssert((pMifareConnection->nType != W_PROP_MIFARE_UL_C) || ((pMifareConnection->bBypassULCAccessRightsRetrieved != W_FALSE) || (pMifareConnection->bULCAccessRightsRetrieved != W_FALSE)));

   if (nSector <= 1)
   {
      /* Sectors [0 - 1] are locked */
      return W_TRUE;
   }

   if (nSector == 2)
   {
      /* Check the block locking lockbits */
      return (pMifareConnection->aLockBytes[0] & 0x07) == 0x07;
   }

   if ((3 <= nSector) && (nSector <= 7))
   {
      /* sectors 3-7 locks are located in aLockBytes[0] */
      return GetBit(pMifareConnection->aLockBytes[0], nSector);
   }

   if ((8 <= nSector) && (nSector <= 15))
   {
      /* sectors 8-15 locks are located in aLockBytes[1] */
      return GetBit(pMifareConnection->aLockBytes[1], nSector - 8);
   }

   /* UL-C only */
   if (W_PROP_MIFARE_UL_C == pMifareConnection->nType)
   {
      if ((16 <= nSector)  && (nSector <= 27))
      {
         return GetBit(pMifareConnection->aLockBytes[2], 1 + (nSector - 16) / 4);
      }

      if ((28 <= nSector)  && (nSector <= 39))
      {
         return GetBit(pMifareConnection->aLockBytes[2], 5 + (nSector - 28) / 4);
      }

      if (nSector == 40)
      {
         /* Check the block locking lockbits */
         return ((pMifareConnection->aLockBytes[2] & 0x11) == 0x11) && ((pMifareConnection->aLockBytes[3] & 0x0F) == 0x0F);
      }

      if ((41 <= nSector) && (nSector <= 43))
      {
         return GetBit(pMifareConnection->aLockBytes[3], nSector - 37);
      }

      if ((44 <= nSector) && (nSector <= 47))
      {
         return GetBit(pMifareConnection->aLockBytes[3], 7);
      }
   }

   /* should not occur */
   CDebugAssert(0);
   return W_TRUE;
}


static void static_PMifareLockSector(
      tContext * pContext,
      tMifareConnection * pMifareConnection,
      uint32_t            nSector)
{
   CDebugAssert((pMifareConnection->nType != W_PROP_MIFARE_UL_C) || (pMifareConnection->bULCAccessRightsRetrieved != W_FALSE));

   if ((3 <= nSector) && (nSector <= 7))
   {
      /* sectors 3-7 locks are located in aLockBytes[0] */
      SetBit(pMifareConnection->aLockBytes[0], nSector);
      return;
   }

   if ((8 <= nSector) && (nSector <= 15))
   {
      /* sectors 8-15 locks are located in aLockBytes[1] */
      SetBit(pMifareConnection->aLockBytes[1], nSector - 8);
      return;
   }

   /* UL-C only */
   if (W_PROP_MIFARE_UL_C == pMifareConnection->nType)
   {
      if ((16 <= nSector)  && (nSector <= 27))
      {
         SetBit(pMifareConnection->aLockBytes[2], 1 + (nSector - 16) / 4);
         return;
      }

      if ((28 <= nSector)  && (nSector <= 39))
      {
         SetBit(pMifareConnection->aLockBytes[2], 5 + (nSector - 28) / 4);
         return;
      }

      if (nSector == 40)
      {
         return;
      }

      if ((41 <= nSector) && (nSector <= 43))
      {
         SetBit(pMifareConnection->aLockBytes[3], 4 + nSector - 41);
         return;
      }

      if ((nSector <= 44) && (nSector <= 47))
      {
         SetBit(pMifareConnection->aLockBytes[3], 7);
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
static bool_t static_PMifareIsSectorLockable(
      tContext * pContext,
      tMifareConnection * pMifareConnection,
      uint32_t            nSector)
{
   CDebugAssert((pMifareConnection->nType != W_PROP_MIFARE_UL_C) || ((pMifareConnection->bBypassULCAccessRightsRetrieved != W_FALSE) || (pMifareConnection->bULCAccessRightsRetrieved != W_FALSE)));

   if (nSector <= 2)
   {
      /* Lock bytes shall be locked with the dedicated API */
      return W_FALSE;
   }

   if (nSector == 3)
   {
      return GetBit(pMifareConnection->aLockBytes[0], 0) == 0;
   }

   if ((4 <= nSector) && (nSector <= 9))
   {
      return GetBit(pMifareConnection->aLockBytes[0], 1) == 0;
   }

   if ((10 <= nSector) && (nSector <= 15))
   {
      return GetBit(pMifareConnection->aLockBytes[0], 2) == 0;
   }

   /* UL-C only */
   if (W_PROP_MIFARE_UL_C == pMifareConnection->nType)
   {
      if ((16 <= nSector) && (nSector <= 27))
      {
         return GetBit(pMifareConnection->aLockBytes[2], 0) == 0;
      }

      if ((28 <= nSector) && (nSector <= 39))
      {
         return GetBit(pMifareConnection->aLockBytes[2], 4) == 0;
      }

      if (nSector == 40)
      {
         /* Lock bytes shall be locked with the dedicated API */
         return W_FALSE;
      }

      if (nSector == 41)
      {
         return GetBit(pMifareConnection->aLockBytes[3], 0) == 0;
      }

      if (nSector == 42)
      {
         return GetBit(pMifareConnection->aLockBytes[3], 1) == 0;
      }

      if (nSector == 43)
      {
         return GetBit(pMifareConnection->aLockBytes[3], 2) == 0;
      }

      if ((44 <= nSector) && (nSector <= 47))
      {
         return GetBit(pMifareConnection->aLockBytes[3], 3) == 0;
      }
   }

   /* should not occur... */
   CDebugAssert(0);
   return W_FALSE;
}

/**
 * @brief   Sends the result.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pMifareConnection  The Mifare connection.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PMifareSendResult(
            tContext* pContext,
            tMifareConnection* pMifareConnection,
            W_ERROR nError )
{
   PDebugTrace("static_PMifareSendResult");

   if (pMifareConnection->hOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pMifareConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PMifareSendResult: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pMifareConnection->hOperation);
      PHandleClose(pContext, pMifareConnection->hOperation);
      pMifareConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the error */
   PDFCPostContext2(&pMifareConnection->sCallbackContext, nError);

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, pMifareConnection->hConnection);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pMifareConnection);
}

/**
 *  Lock Bytes 0-1 have been written.
 */
static void static_PMifareWriteLockBytesCompleted(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tMifareConnection* pMifareConnection = (tMifareConnection*) pCallbackParameter;

   PDebugTrace("static_PMifareWriteLockBytesCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PMifareWriteLockBytesCompleted: returning %s", PUtilTraceError(nError));
   }

   static_PMifareSendResult(pContext, pMifareConnection, nError);
}

/**
 *  Lock Bytes 2-3 have been written.
 */
static void static_PMifareWriteLockBytes2Completed(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tMifareConnection* pMifareConnection = (tMifareConnection*) pCallbackParameter;

   PDebugTrace("static_PMifareWriteLockBytes2Completed");

   if (nError == W_SUCCESS)
   {
      PNDEF2GenWrite(pContext,
                     pMifareConnection->hConnection,
                     static_PMifareWriteLockBytesCompleted,
                     pMifareConnection,
                     &pMifareConnection->aLockBytes[0],
                     P_NDEF2GEN_STATIC_LOCK_BYTE_ADDRESS,
                     P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH);
   }
   else
   {
      PDebugError("static_PMifareWriteLockBytes2Completed: returning %s", PUtilTraceError(nError));

      static_PMifareSendResult(pContext, pMifareConnection, nError);
   }
}


/**
 *  Writes the lock bytes into the card
 */
static void static_PMifareWriteLockBytes(
            tContext* pContext,
            tMifareConnection* pMifareConnection)
{
   PDebugTrace("static_PMifareWriteLockBytes");

   if (pMifareConnection->nType == W_PROP_MIFARE_UL_C)
   {
      PNDEF2GenWrite(pContext,
                     pMifareConnection->hConnection,
                     static_PMifareWriteLockBytes2Completed,
                     pMifareConnection,
                     &pMifareConnection->aLockBytes[2],
                     pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_LOCK_BLOCK),
                     P_MIFARE_ULC_LOCK_LENGTH);
   }
   else
   {
      /* fake the write of the Lock Bytes 2-3 */
      static_PMifareWriteLockBytes2Completed(pContext, pMifareConnection, W_SUCCESS);
   }
}

/**
 * Bytes have been read
 */
static void static_PMifareReadCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tMifareConnection* pMifareConnection = (tMifareConnection*)pCallbackParameter;

   PDebugTrace("static_PMifareReadCompleted");

   static_PMifareSendResult(pContext, pMifareConnection, nError );
}

/**
 * Bytes have been written
 */
static void static_PMifareWriteCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tMifareConnection* pMifareConnection = (tMifareConnection*)pCallbackParameter;

   PDebugTrace("static_PMifareWriteCompleted");

   if(nError == W_SUCCESS)
   {
      /* Check if we need to lock the card */
      if ( pMifareConnection->nLengthToLock != 0 )
      {
         uint32_t nBlockStart, nBlockEnd, nIndex;

         nBlockStart = pMifareConnection->pSectorSize->pDivide(pMifareConnection->nOffsetToLock);
         nBlockEnd   = pMifareConnection->pSectorSize->pDivide(pMifareConnection->nLengthToLock + pMifareConnection->nOffsetToLock - 1);

         for( nIndex = nBlockStart; nIndex <= nBlockEnd; nIndex ++ )
         {
            static_PMifareLockSector(pContext, pMifareConnection, nIndex);
         }

         pMifareConnection->nLengthToLock = 0;

         /* Write lock bytes */
         static_PMifareWriteLockBytes(pContext, pMifareConnection);

         return;
      }
   }

   /* Send the result */
   static_PMifareSendResult( pContext, pMifareConnection, nError );
}

/** access rigths configuration automaton  */
static void static_PMifareULCRetrieveAccessRightsAutomaton(
      tContext * pContext,
      void     * pCallbackParameter,
      W_ERROR    nError)
{
   tMifareConnection* pMifareConnection = (tMifareConnection*) pCallbackParameter;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PMifareULCRetrieveAccessRightsAutomaton at state %d returning %s",
                     pMifareConnection->nCurrentOperationState, PUtilTraceError(nError));
      goto return_error;
   }

   switch (pMifareConnection->nCurrentOperationState)
   {
      case 0 :
         /* Read Auth byte 0 (in block 0x2A) */
         PNDEF2GenRead(pContext,
                       pMifareConnection->hConnection,
                       static_PMifareULCRetrieveAccessRightsAutomaton,
                       pMifareConnection,
                       &pMifareConnection->nAuthenticateThreshold,
                       pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_AUTH0_BLOCK),
                       1);
         break;

      case 1 :
         /* Read Auth byte 1 (in block 0x2B) */
         PNDEF2GenRead(pContext,
                       pMifareConnection->hConnection,
                       static_PMifareULCRetrieveAccessRightsAutomaton,
                       pMifareConnection,
                       &pMifareConnection->nAuthenticateMode,
                       pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_AUTH1_BLOCK),
                       1);
         break;

      case 2 :
         /* All bytes have been read */
         pMifareConnection->nCurrentOperationState = 0;
         pMifareConnection->bULCAccessRightsRetrieved = W_TRUE;
         static_PMifareSendResult(pContext, pMifareConnection, W_SUCCESS);
         return;
   }

   pMifareConnection->nCurrentOperationState++;

   return;

return_error:

   /* Send error */
   static_PMifareSendResult(pContext, pMifareConnection, nError);
}

/** access rigths configuration automaton  */
static void static_PMifareULCSetAccessRightsAutomaton(
      tContext * pContext,
      void     * pCallbackParameter,
      W_ERROR    nError)
{
   tMifareConnection* pMifareConnection = pCallbackParameter;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PMifareULCSetAccessRightsAutomaton : nError %d", nError);
      static_PMifareSendResult(pContext, pMifareConnection, nError);
      return;
   }

   switch (pMifareConnection->nCurrentOperationState)
   {
      case 0 :

         /* to avoid to make the card unusable if something goes wrong during the configuration,
          *  we first set the threshold to P_MIFARE_BLOCK_NUMBER_UL_C to allow non authenticated access
          *  to the whole card content.
          *
          *  The effective threshold value will be set once all other operations (e.g key programming successfully done)
          */

         pMifareConnection->aTempBuffer[0] = P_MIFARE_BLOCK_NUMBER_UL_C;

         PNDEF2GenWrite(pContext, pMifareConnection->hConnection,
                        static_PMifareULCSetAccessRightsAutomaton, pMifareConnection,
                        pMifareConnection->aTempBuffer,
                        pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_AUTH0_BLOCK), 1);

         break;

      case 1 :

         /* update the current authentication threshold */
         pMifareConnection->nAuthenticateThreshold = P_MIFARE_BLOCK_NUMBER_UL_C;

         /* Write the requested authentication mode */
         PNDEF2GenWrite(pContext, pMifareConnection->hConnection,
                        static_PMifareULCSetAccessRightsAutomaton, pMifareConnection,
                        &pMifareConnection->nRequestedAuthenticateMode,
                        pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_AUTH1_BLOCK), 1);
         break;

      case 2 :

         /* update the current authentication mode */
         pMifareConnection->nAuthenticateMode = pMifareConnection->nRequestedAuthenticateMode;

         /* write the requested authentication keys : we do not use the smart cache for this section
            since smart cache performs a read prior a write, and authencication keys are not readable ! */

         /* First 4 bytes of the key */
         PNDEF2GenDirectWrite(pContext, pMifareConnection->hConnection,
                              static_PMifareULCSetAccessRightsAutomaton, pMifareConnection,
                              pMifareConnection->aKey, P_MIFARE_ULC_KEY_BLOCK);
         break;

      case 3 :

         /* Second 4 bytes of the key */
         PNDEF2GenDirectWrite(pContext, pMifareConnection->hConnection,
                              static_PMifareULCSetAccessRightsAutomaton, pMifareConnection,
                              pMifareConnection->aKey + 4, P_MIFARE_ULC_KEY_BLOCK + 1);
         break;

      case 4 :

         /* Third 4 bytes of the key */
         PNDEF2GenDirectWrite(pContext, pMifareConnection->hConnection,
                              static_PMifareULCSetAccessRightsAutomaton, pMifareConnection,
                              pMifareConnection->aKey + 8, P_MIFARE_ULC_KEY_BLOCK + 2);
         break;

      case 5 :

         /* Last 4 bytes of the key */
         PNDEF2GenDirectWrite(pContext, pMifareConnection->hConnection,
                              static_PMifareULCSetAccessRightsAutomaton, pMifareConnection,
                              pMifareConnection->aKey + 12, P_MIFARE_ULC_KEY_BLOCK + 3);
         break;

      case 6 :

         /* Erase the key to avoid to remain it in memory longer than needed */
         CMemoryFill(pMifareConnection->aKey, 0, sizeof(pMifareConnection->aKey));

         /* write the effective threshold value */
         PNDEF2GenWrite(pContext, pMifareConnection->hConnection,
                        static_PMifareULCSetAccessRightsAutomaton, pMifareConnection,
                        &pMifareConnection->nRequestedAuthenticateThreshold,
                        pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_AUTH0_BLOCK), 1);
         break;

      case 7 :

         /* update the current threshold value */
         pMifareConnection->nAuthenticateThreshold = pMifareConnection->nRequestedAuthenticateThreshold;

         /* after a modification of the authentication configuration, we need to invalidate the smart cache :

            - to avoid further access to authenticated area without prior authentication

            - Mifare UL-C smart cache may be corrupted if cache threshold was not a multiple of 4 sectors
              (in this case, when a read operation goes beyond the threshold, the read operation wraps and returns contents of first sectors of the card)
         */

         PNDEF2GenInvalidateCache(pContext, pMifareConnection->hConnection, 0, P_MIFARE_BLOCK_NUMBER_DATA_UL_C * P_MIFARE_BLOCK_SIZE_UL_C);

         if (pMifareConnection->bLockAuthentication != W_FALSE)
         {
            /* the lock of the authentication area has been requested */

            pMifareConnection->aTempBuffer[0] = 0xE0;

            PNDEF2GenWrite(pContext, pMifareConnection->hConnection,
                           static_PMifareULCSetAccessRightsAutomaton, pMifareConnection,
                           pMifareConnection->aTempBuffer,
                           pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_LOCK_BLOCK) + 1, 1);
         }
         else
         {
            /* no lock requested, the operation is now completed  */
            static_PMifareSendResult(pContext, pMifareConnection, W_SUCCESS);
            return;
         }

         break;

      case 8 :

         static_PMifareSendResult(pContext, pMifareConnection, W_SUCCESS);
         break;
   }

   pMifareConnection->nCurrentOperationState++;
}

/** access rigths configuration automaton  */
static void static_PMifareULCAuthenticateAutomaton(
      tContext * pContext,
      void     * pCallbackParameter,
      uint32_t   nDataLength,
      W_ERROR    nError)
{
   tMifareConnection* pMifareConnection = (tMifareConnection*) pCallbackParameter;

   if (nError == W_SUCCESS)
   {

      switch (pMifareConnection->nCurrentOperationState)
      {
         case 0 :

            /* Send the Authenticate command */

            P14P3UserExchangeData(pContext, pMifareConnection->hConnection, static_PMifareULCAuthenticateAutomaton, pMifareConnection,
                                    g_MifareULCAuthenticateCommand, sizeof(g_MifareULCAuthenticateCommand),
                                    pMifareConnection->aTempBuffer, sizeof(pMifareConnection->aTempBuffer),
                                    null, W_TRUE, W_FALSE);
            break;

         case 1 :

            /* We received the answer of the authenticate command */
            /* the format of this command is 0xAF followed by a 8 bytes random encrypted */

            if ((nDataLength == 9) && (pMifareConnection->aTempBuffer[0] == 0xAF))
            {
               /* Decrypt the received random */
               uint8_t  aTemp[16];
               uint32_t nRandom;

               CMemoryFill(pMifareConnection->aIvec, 0, 8);
               PCrypto3DesDecipherCbc(pMifareConnection->aTempBuffer + 1, pMifareConnection->aKey, pMifareConnection->aKey + 8, pMifareConnection->aKey, pMifareConnection->aIvec, & aTemp[7]);
               aTemp[15] = aTemp[7];

               /* Generate a 8 bytes random value */
               /* @todo here, if the IOCTL failed, the random value will be set to zero */
               nRandom = PContextDriverGenerateRandom(pContext);
               CMemoryCopy(aTemp, &nRandom, 4);

               /* @todo here, if the IOCTL failed, the random value will be set to zero */
               nRandom = PContextDriverGenerateRandom(pContext);
               CMemoryCopy(aTemp + 4, &nRandom, 4);

               /* Save rotated random in the context for later comparison */
               CMemoryCopy(pMifareConnection->aRandom, aTemp + 1, 7);
               pMifareConnection->aRandom[7] = aTemp[0];

               PCrypto3DesCipherCbc(aTemp, pMifareConnection->aKey, pMifareConnection->aKey + 8, pMifareConnection->aKey, pMifareConnection->aIvec, pMifareConnection->aTempBuffer + 1);
               PCrypto3DesCipherCbc(aTemp + 8, pMifareConnection->aKey, pMifareConnection->aKey + 8, pMifareConnection->aKey, pMifareConnection->aIvec, pMifareConnection->aTempBuffer + 9);

               pMifareConnection->aTempBuffer[0] = 0xAF;

               P14P3UserExchangeData(pContext, pMifareConnection->hConnection, static_PMifareULCAuthenticateAutomaton, pMifareConnection,
                                    pMifareConnection->aTempBuffer, 17,
                                    pMifareConnection->aTempBuffer, sizeof(pMifareConnection->aTempBuffer),
                                    null, W_TRUE, W_FALSE);
            }
            else
            {
               PDebugError("static_PMifareULCAuthenticateAutomaton : bad authenticate answer length / format");
               nError = W_ERROR_CONNECTION_COMPATIBILITY;
               goto return_error;
            }
            break;

         case 2 :

            /* We received the answer of the second part of the authentication */
            /* the format of this command is 0x00 followed by a 8 bytes random encrypted */

            if ((nDataLength == 9) && (pMifareConnection->aTempBuffer[0] == 0x00))
            {
               /* Decrypt the received random */
               uint8_t aRandom[8];

               PCrypto3DesDecipherCbc(pMifareConnection->aTempBuffer + 1, pMifareConnection->aKey, pMifareConnection->aKey + 8, pMifareConnection->aKey, pMifareConnection->aIvec, aRandom);

               if (CMemoryCompare(aRandom, pMifareConnection->aRandom, 8) == 0)
               {
                  /* The decrypted random matches */
                  static_PMifareSendResult(pContext, pMifareConnection, W_SUCCESS);
                  return;
               }
               else
               {
                  PDebugError("static_PMifareULCAuthenticateAutomaton : bad random");
                  nError = W_ERROR_CONNECTION_COMPATIBILITY;
                  goto return_error;
               }
            }
            else
            {
               PDebugError("static_PMifareULCAuthenticateAutomaton : bad authenticate answer length / format");
               nError = W_ERROR_CONNECTION_COMPATIBILITY;
               goto return_error;
            }
            break;
      }

      pMifareConnection->nCurrentOperationState++;
      return;
   }

return_error:

   static_PMifareSendResult(pContext, pMifareConnection, nError);
}


static void static_PMifareCreateConnectionCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tMifareConnection* pMifareConnection = (tMifareConnection*)pCallbackParameter;

   PDebugTrace("static_PMifareCreateConnectionCompleted");

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PMifareCreateConnectionCompleted: returning %s", PUtilTraceError(nError));
   }

   if(nError == W_SUCCESS)
   {
      /* Nothing */
   }else if( (nError == W_ERROR_RF_PROTOCOL_NOT_SUPPORTED) ||
             (nError == W_ERROR_RF_COMMUNICATION)          ||
             (nError == W_ERROR_TIMEOUT))
   {
      nError = W_ERROR_RF_COMMUNICATION;
   }
   else
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
   }


   /* Send the error */
   PDFCPostContext2(&pMifareConnection->sCallbackContext, nError);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pMifareConnection);
}

extern tSmartCacheSectorSize g_sSectorSize4;

/* See Header file */
void PMifareCreateConnection3A(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty )
{
   tMifareConnection* pMifareConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PMifareCreateConnection3A");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the NDEF buffer */
   pMifareConnection = (tMifareConnection*)CMemoryAlloc( sizeof(tMifareConnection) );
   if ( pMifareConnection == null )
   {
      PDebugError("PMifareCreateConnection3A: pMifareConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pMifareConnection, 0, sizeof(tMifareConnection));

   /* Get the 14443-3 information level */
   if ( ( nError = P14P3UserCheckMifare(
                     pContext,
                     hConnection,
                     pMifareConnection->UID,
                     &pMifareConnection->nUIDLength,
                     &pMifareConnection->nType ) ) != W_SUCCESS )
   {
      PDebugLog("PMifareCreateConnection3A: not a Mifare card");
      goto return_error;
   }

   PDebugTrace("PMifareCreateConnection3A: detection of the type %s",
      PUtilTraceConnectionProperty(pMifareConnection->nType));

   switch ( pMifareConnection->nType )
   {
      case W_PROP_MIFARE_UL:
      case W_PROP_MIFARE_UL_C :
         /* for now, we can not distinguish the Mifare UL from the Mifare UL-C card */
         pMifareConnection->nSectorNumber = P_MIFARE_BLOCK_NUMBER_UL;
         pMifareConnection->nSectorSize   = P_MIFARE_BLOCK_SIZE_UL;
         pMifareConnection->pSectorSize   = &g_sSectorSize4;
         break;

      case W_PROP_MIFARE_MINI :
         pMifareConnection->nSectorNumber = P_MIFARE_BLOCK_NUMBER_MINI;
         pMifareConnection->nSectorSize   = P_MIFARE_BLOCK_SIZE_MINI;
         break;

      case W_PROP_MIFARE_1K:
         pMifareConnection->nSectorNumber = P_MIFARE_BLOCK_NUMBER_1K;
         pMifareConnection->nSectorSize   = P_MIFARE_BLOCK_SIZE_1K;
         break;
      case W_PROP_MIFARE_4K:
         pMifareConnection->nSectorNumber = P_MIFARE_BLOCK_NUMBER_4K;
         pMifareConnection->nSectorSize   = P_MIFARE_BLOCK_SIZE_4K;
         break;

      case W_PROP_MIFARE_PLUS_S_2K :
      case W_PROP_MIFARE_PLUS_X_2K :
         pMifareConnection->nSectorNumber = P_MIFARE_BLOCK_NUMBER_PLUS_2K;
         pMifareConnection->nSectorSize   = P_MIFARE_BLOCK_SIZE_PLUS_2K;
         break;

      case W_PROP_MIFARE_PLUS_S_4K :
      case W_PROP_MIFARE_PLUS_X_4K :
         pMifareConnection->nSectorNumber = P_MIFARE_BLOCK_NUMBER_PLUS_4K;
         pMifareConnection->nSectorSize   = P_MIFARE_BLOCK_SIZE_PLUS_4K;
         break;

      default:
         PDebugError(
            "PMifareCreateConnection3A: unknown type" );
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         goto return_error;
   }

   /* Add the Mifare connection structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hConnection,
                     pMifareConnection,
                     P_HANDLE_TYPE_MIFARE_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("PMifareCreateConnection3A: error returned by PHandleAddHeir()");
      goto return_error;
   }

   /* Store the connection information */
   pMifareConnection->hConnection = hConnection;

   if( W_PROP_MIFARE_1K == pMifareConnection->nType || W_PROP_MIFARE_4K != pMifareConnection->nType)
   {
      P14Part3SetTimeout(pContext,
                         hConnection,
                         P_MIFARE_CLASSIC_DEFAULT_TIMEOUT);

   }

   if (W_PROP_MIFARE_UL != pMifareConnection->nType && W_PROP_MIFARE_UL_C != pMifareConnection->nType)
   {
      /* Nothing to read in this type of card */
      PDFCPostContext2(&sCallbackContext, W_SUCCESS);
   }
   else
   {
      /* --- Read Lock bytes 0-1 (in block 2) --- */
      /* Store the callback context */
      pMifareConnection->sCallbackContext = sCallbackContext;

      /* Increment the reference count to keep the connection object alive during
         the operation. The reference count is decreased in static_PMifareSendResult()
         when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Read data */
      PNDEF2GenRead(pContext,
                    hConnection,
                    static_PMifareCreateConnectionCompleted,
                    pMifareConnection,
                    &pMifareConnection->aLockBytes[0],
                    P_NDEF2GEN_STATIC_LOCK_BYTE_ADDRESS,
                    P_NDEF2GEN_STATIC_LOCK_BYTE_LENGTH);
   }

   return;

return_error:

   if (nError != W_ERROR_CONNECTION_COMPATIBILITY)
   {
      PDebugError("PMifareCreateConnection3A: return error %s", PUtilTraceError(nError));
   }

   if(pMifareConnection != null)
   {
      CMemoryFree(pMifareConnection);
   }

   PDFCPostContext2(&sCallbackContext, nError);
}

/** See tPReaderUserRemoveSecondaryConnection */
void PMifareRemoveConnection3A(
            tContext* pContext,
            W_HANDLE hUserConnection )
{
   tMifareConnection* pMifareConnection = (tMifareConnection*)PHandleRemoveLastHeir(
            pContext, hUserConnection,
            P_HANDLE_TYPE_MIFARE_CONNECTION);

   PDebugTrace("PMifareRemoveConnection3A");

   /* Remove the connection object */
   if(pMifareConnection != null)
   {
      CMemoryFree(pMifareConnection);
   }
}

static void static_PMifareGetVersionCompleted(
      tContext * pContext,
      void * pCallbackParameter,
      uint32_t nLength,
      W_ERROR  nError)
{
   tMifareConnection4A* pMifareConnection4A = (tMifareConnection4A*) pCallbackParameter;
   uint32_t             i;

   if (nError != W_SUCCESS)
   {
      goto end;
   }

   switch (pMifareConnection4A->nState)
   {
      case 0 :  /* First answer */

         if (nLength != sizeof (g_MifareDesfireGetVersionAnswerMask))
         {
            PDebugError("static_PMifareGetVersionCompleted : Invalid answer length");
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
            goto end;
         }

         /* apply the mask to the answer received from the card */
         for (i=0; i<nLength; i++)
         {
            if ((pMifareConnection4A->aBuffer[i] & g_MifareDesfireGetVersionAnswerMask[i]) != g_MifareDesfireGetVersionAnswerMasked[i])
            {
               PDebugError("static_PMifareGetVersionCompleted : Invalid answer payload");
               nError = W_ERROR_CONNECTION_COMPATIBILITY;
               goto end;
            }
         }

         switch (pMifareConnection4A->aBuffer[3])     /* Major version numer */
         {
            case 0x00 :

               /* DESFIRE D40 - size should be 4 K */

               if (pMifareConnection4A->aBuffer[5] == 0x18)    /* Size */
               {
                  pMifareConnection4A->nType = W_PROP_MIFARE_DESFIRE_D40;
               }
               else
               {
                  PDebugError("static_PMifareGetVersionCompleted : Invalid size for DESFilre D40 !!!");
                  nError = W_ERROR_CONNECTION_COMPATIBILITY;
                  goto end;
               }
               break;

            case 0x01 :

               /* DESFIRE EV1 series */

               switch (pMifareConnection4A->aBuffer[5])        /* Size */
               {
                  case 0x16 :
                     pMifareConnection4A->nType = W_PROP_MIFARE_DESFIRE_EV1_2K;
                     nError = W_SUCCESS;
                     break;

                  case 0x18 :
                     pMifareConnection4A->nType = W_PROP_MIFARE_DESFIRE_EV1_4K;
                     nError = W_SUCCESS;
                     break;

                  case 0x1A :
                     pMifareConnection4A->nType = W_PROP_MIFARE_DESFIRE_EV1_8K;
                     nError = W_SUCCESS;
                     break;

                  default :

                     PDebugError("static_PMifareGetVersionCompleted : Invalid size for DESFire EV1 !!!");
                     nError = W_ERROR_CONNECTION_COMPATIBILITY;
                     goto end;
               }
               break;

            default :

               PDebugError("static_PMifareGetVersionCompleted : Invalid major version");
               nError = W_ERROR_CONNECTION_COMPATIBILITY;
               goto end;
         }


         /* Ok, all checks passed */
         PReaderExchangeDataInternal(pContext, pMifareConnection4A->hConnection, static_PMifareGetVersionCompleted, pMifareConnection4A,
                                 g_MifareDesfireGetNextVersion, sizeof(g_MifareDesfireGetNextVersion),
                                 pMifareConnection4A->aBuffer, sizeof(pMifareConnection4A->aBuffer), null);
         pMifareConnection4A->nState++;

         /* no more processing here */
         return;

      case 1 : /* Second answer */

         if (nLength != 9)
         {
            PDebugError("static_PMifareGetVersionCompleted : Invalid answer length/3");
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }

         PReaderExchangeDataInternal(pContext, pMifareConnection4A->hConnection, static_PMifareGetVersionCompleted, pMifareConnection4A,
                                 g_MifareDesfireGetNextVersion, sizeof(g_MifareDesfireGetNextVersion),
                                 pMifareConnection4A->aBuffer, sizeof(pMifareConnection4A->aBuffer), null);

         pMifareConnection4A->nState++;

         /* no more processing here */
         return;

      case 2 :  /* Third and last answer */

         if (nLength != 16)
         {
            PDebugError("static_PMifareGetVersionCompleted : Invalid answer length/3");
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }
   }

end:

   /* Send the result */

   if (nError == W_SUCCESS)
   {
      /* Add the Mifare connection structure */
      if ( ( nError = PHandleAddHeir(
                        pContext,
                        pMifareConnection4A->hConnection,
                        pMifareConnection4A,
                        P_HANDLE_TYPE_MIFARE_CONNECTION_4_A ) ) != W_SUCCESS )
      {
         PDebugError("PMifareCreateConnection4A: Error in PHandleAddHeir()");
      }
   }else if ( (nError == W_ERROR_RF_COMMUNICATION) ||
              (nError == W_ERROR_RF_PROTOCOL_NOT_SUPPORTED) ||
              (nError == W_ERROR_TIMEOUT)
            )
   {
      nError = W_ERROR_RF_COMMUNICATION;
   }
   else
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Send the result */
   PDFCPostContext2( & pMifareConnection4A->sCallbackContext, nError );

   if (nError != W_SUCCESS)
   {
      CMemoryFree(pMifareConnection4A);
   }
}

/* See Header file */
void PMifareCreateConnection4A(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProperty )
{
   tMifareConnection4A* pMifareConnection4A = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PMifareCreateConnection4A");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the NDEF buffer */
   pMifareConnection4A = (tMifareConnection4A*)CMemoryAlloc( sizeof(tMifareConnection4A) );
   if ( pMifareConnection4A == null )
   {
      PDebugError("PMifareCreateConnection4A: pMifareConnection == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pMifareConnection4A, 0, sizeof(tMifareConnection4A));

   nError = P14P4UserCheckMifare(
                     pContext,
                     hConnection,
                     pMifareConnection4A->UID,
                     &pMifareConnection4A->nUIDLength,
                     &pMifareConnection4A->nType );

   if (nError != W_SUCCESS)
   {
      goto return_error;
   }

   /* Check the property */
   switch (pMifareConnection4A->nType)
   {
      /* DESFire series */
      case W_PROP_MIFARE_DESFIRE_D40 :

         /* generic desfire detected, send the Get Version command to get the variant */

         pMifareConnection4A->hConnection = hConnection;
         pMifareConnection4A->sCallbackContext = sCallbackContext;

         PReaderExchangeDataInternal(pContext, hConnection, static_PMifareGetVersionCompleted, pMifareConnection4A,
                                 g_MifareDesfireGetVersion, sizeof(g_MifareDesfireGetVersion),
                                 pMifareConnection4A->aBuffer, sizeof(pMifareConnection4A->aBuffer), null);
         return;

      case W_PROP_MIFARE_DESFIRE_EV1_2K :
      case W_PROP_MIFARE_DESFIRE_EV1_4K :
      case W_PROP_MIFARE_DESFIRE_EV1_8K :
      case W_PROP_MIFARE_PLUS_X_2K :
      case W_PROP_MIFARE_PLUS_X_4K :
      case W_PROP_MIFARE_PLUS_S_2K :
      case W_PROP_MIFARE_PLUS_S_4K :

         break;


      default :
         PDebugError("PMifareCreateConnection4A: not an expected Mifare");
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         goto return_error;
   }

   /* Add the Mifare connection structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hConnection,
                     pMifareConnection4A,
                     P_HANDLE_TYPE_MIFARE_CONNECTION_4_A ) ) != W_SUCCESS )
   {
      PDebugError("PMifareCreateConnection4A: Error in PHandleAddHeir()");
      goto return_error;
   }

   /* Send the result */
   PDFCPostContext2( &sCallbackContext, W_SUCCESS );
   return;

return_error:

   if (nError != W_ERROR_CONNECTION_COMPATIBILITY)
   {
      PDebugError("PMifareCreateConnection4A: return error %s", PUtilTraceError(nError));
   }

   PDFCPostContext2( &sCallbackContext, nError );

   CMemoryFree(pMifareConnection4A);
}

/* See Header file */
W_ERROR PMifareCheckType2(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t* pnMaximumSpaceSize,
            uint8_t* pnSectorSize,
            bool_t* pbIsLocked,
            bool_t* pbIsLockable,
            bool_t* pbIsFormattable )
{
   tMifareConnection* pMifareConnection;
   uint8_t nIndex;
   W_ERROR nError;
   bool_t    bIsLocked = W_FALSE;
   bool_t    bIsLockable = W_TRUE;
   uint32_t nLastBlockIndex;

   PDebugTrace("PMifareCheckType2");

   /* Reset the maximum tag size */
   if (pnMaximumSpaceSize != null) *pnMaximumSpaceSize = 0;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);
   if ( nError == W_SUCCESS )
   {
      /* Maximum size */
      switch ( pMifareConnection->nType )
      {
      case W_PROP_MIFARE_UL:
         nLastBlockIndex = P_MIFARE_UL_LAST_DATA_BLOCK;
         break;
      case W_PROP_MIFARE_UL_C :
         nLastBlockIndex = P_MIFARE_ULC_LAST_DATA_BLOCK;

         if ((pMifareConnection->bBypassULCAccessRightsRetrieved  == W_FALSE)
          && (pMifareConnection->bULCAccessRightsRetrieved == W_FALSE))
         {
            /* Access rights information have not been retreived, we can not format this card */
            PDebugError("PMifareCheckType2: access rights unavailable");
            return W_ERROR_MISSING_INFO;
         }


         break;
      default:
         /* other mifare cards are not supported (since we do not support read/write) */
         PDebugError("PMifareCheckType2: Unknown type");
         return W_ERROR_CONNECTION_COMPATIBILITY;
      }

      if (pnMaximumSpaceSize != null)
      {
         *pnMaximumSpaceSize = pMifareConnection->pSectorSize->pMultiply(nLastBlockIndex - P_MIFARE_FIRST_DATA_BLOCK + 1);
      }

      /* Go through the lock byte */
      for (nIndex=4; nIndex <= nLastBlockIndex; nIndex++)
      {
         bIsLocked |= static_PMifareIsSectorLocked(pContext, pMifareConnection, nIndex);
         bIsLockable &= static_PMifareIsSectorLockable(pContext, pMifareConnection, nIndex);
      }

      if (pbIsLocked != null)       *pbIsLocked = bIsLocked;
      if (pbIsLockable != null)     *pbIsLockable = bIsLockable;
      if (pnSectorSize != null)     *pnSectorSize = (uint8_t)pMifareConnection->pSectorSize->nValue;
      if (pbIsFormattable != null)  *pbIsFormattable = ! bIsLocked;

      pMifareConnection->bBypassULCAccessRightsRetrieved = W_FALSE;

      return W_SUCCESS;
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR PMifareGetConnectionInfo(
            tContext* pContext,
            W_HANDLE hConnection,
            tWMifareConnectionInfo *pConnectionInfo )
{
   tMifareConnection* pMifareConnection;
   W_ERROR nError;

   PDebugTrace("PMifareGetConnectionInfo");

   /* Check the parameters */
   if ( pConnectionInfo == null )
   {
      PDebugError("PMifareGetConnectionInfo: pConnectionInfo == null");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);
   if ( nError == W_SUCCESS )
   {
      /* UID */
      CMemoryCopy(
         pConnectionInfo->UID,
         pMifareConnection->UID,
         pMifareConnection->nUIDLength );
      /* Sector size */
      pConnectionInfo->nSectorSize = (uint16_t)pMifareConnection->nSectorSize;
      /* Sector number */
      pConnectionInfo->nSectorNumber = (uint16_t)pMifareConnection->nSectorNumber;
      return W_SUCCESS;
   }
   else
   {
      tMifareConnection4A * pMifareConnection4A;

      nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION_4_A, (void**)&pMifareConnection4A);

      if ( nError == W_SUCCESS )
      {
         /* UID */
         CMemoryCopy(
            pConnectionInfo->UID,
            pMifareConnection4A->UID,
            pMifareConnection4A->nUIDLength );
         /* Sector size is meaningless for DESFire */
         pConnectionInfo->nSectorSize = 0;
         /* Sector number is meaningless for DESFire */
         pConnectionInfo->nSectorNumber = 0;
      }
      else
      {
         PDebugError("PMifareGetConnectionInfo: could not get pMifareConnection buffer");

         /* Fill in the structure with zeros */
         CMemoryFill(pConnectionInfo, 0, sizeof(tWMifareConnectionInfo));
      }

      return nError;
   }
}

/* Execute the queued operation (after polling) */
static void static_PMifareExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tMifareConnection* pMifareConnection = (tMifareConnection*) pObject;
   uint32_t i;

   PDebugTrace("static_PMifareExecuteQueuedExchange");

   /* Restore operation handle */
   pMifareConnection->hOperation = pMifareConnection->sQueuedOperation.hOperation;
   /* Restore callback context */
   pMifareConnection->sCallbackContext = pMifareConnection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (pMifareConnection->hOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pMifareConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PMifareExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_PMifareSendResult(pContext, pMifareConnection, nResult);
   }
   else
   {
      switch (pMifareConnection->sQueuedOperation.nType)
      {
      case P_MIFARE_QUEUED_READ:
         /* Read */
         PNDEF2GenRead(pContext,
                       pMifareConnection->hConnection,
                       static_PMifareReadCompleted,
                       pMifareConnection,
                       pMifareConnection->sQueuedOperation.pBuffer,
                       pMifareConnection->sQueuedOperation.nOffset,
                       pMifareConnection->sQueuedOperation.nLength);

         break;

      case P_MIFARE_QUEUED_WRITE:
         /* Write */
         if(pMifareConnection->sQueuedOperation.bLockSectors != W_FALSE)
         {
            pMifareConnection->nOffsetToLock = pMifareConnection->sQueuedOperation.nOffset;
            pMifareConnection->nLengthToLock = pMifareConnection->sQueuedOperation.nLength;
         }
         else
         {
            pMifareConnection->nOffsetToLock = 0;
            pMifareConnection->nLengthToLock = 0;
         }

         if (pMifareConnection->sQueuedOperation.pBuffer != null)
         {
            PNDEF2GenWrite(pContext,
                           pMifareConnection->hConnection,
                           static_PMifareWriteCompleted,
                           pMifareConnection,
                           pMifareConnection->sQueuedOperation.pBuffer,
                           pMifareConnection->sQueuedOperation.nOffset,
                           pMifareConnection->sQueuedOperation.nLength);
         }
         else
         {
            static_PMifareWriteCompleted(pContext, pMifareConnection, W_SUCCESS);
         }

         break;

      case P_MIFARE_QUEUED_AUTHENTICATE:
         /* Authenticate */
         if (pMifareConnection->sQueuedOperation.pKey != null)
         {
            CMemoryCopy(pMifareConnection->aKey, pMifareConnection->sQueuedOperation.pKey, 16);
         }
         else
         {
            PDebugWarning("PMifareULCAuthenticate : pKey is null, using default key");
            CMemoryCopy(pMifareConnection->aKey, g_MifareULCDefaultKey, sizeof(g_MifareULCDefaultKey));
         }

         /* set the current opeation state */
         pMifareConnection->nCurrentOperationState = 0;

         /* all checks are done, perform the operation */
         static_PMifareULCAuthenticateAutomaton(pContext, pMifareConnection, 0, W_SUCCESS);

         break;

      case P_MIFARE_QUEUED_SET_ACCESS_RIGHTS:
         /* Set Access Rights */
         for (i=0; i<8; i++)
         {
            pMifareConnection->aKey[i]   = pMifareConnection->sQueuedOperation.pKey[7-i];
            pMifareConnection->aKey[8+i] = pMifareConnection->sQueuedOperation.pKey[15-i];
         }
         pMifareConnection->nRequestedAuthenticateThreshold = pMifareConnection->sQueuedOperation.nRequestedAuthenticateThreshold;
         pMifareConnection->nRequestedAuthenticateMode = pMifareConnection->sQueuedOperation.nRequestedAuthenticateMode;
         pMifareConnection->bLockAuthentication = pMifareConnection->sQueuedOperation.bLockAuthentication;

         /* set the current opeation state */
         pMifareConnection->nCurrentOperationState = 0;

         /* all checks are done, perform the operation */
         static_PMifareULCSetAccessRightsAutomaton(pContext, pMifareConnection, W_SUCCESS);

         break;

      case P_MIFARE_QUEUED_RETRIEVE_ACCESS_RIGHTS:
         /* For UL-C, access rights (lock and auth bytes) must be read */
         if (W_PROP_MIFARE_UL_C == pMifareConnection->nType)
         {
            /* Init automaton */
            pMifareConnection->nCurrentOperationState = 0;

            /* Read Lock bytes 2-3 (in block 0x28) */
            PNDEF2GenRead(pContext,
                          pMifareConnection->hConnection,
                          static_PMifareULCRetrieveAccessRightsAutomaton,
                          pMifareConnection,
                          &pMifareConnection->aLockBytes[2],
                          pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_LOCK_BLOCK),
                          P_MIFARE_ULC_LOCK_LENGTH);
         }
         else
         {
            /* For Mifare UL, only the lock bytes are needed, and have been already read, so nothing to do */
            static_PMifareSendResult(pContext, pMifareConnection, W_SUCCESS);
         }

         break;

      case P_MIFARE_QUEUED_FREEZE_LOCK_CONF:
         /* Freeze data lock configuration */
         /* LOCK byte # 0 : Set the bits 1 and 2 that lock the other lock bits */
         pMifareConnection->aLockBytes[0] |= 0x06;

         if (W_PROP_MIFARE_UL_C == pMifareConnection->nType)
         {
            /* LOCK byte # 3 : Set the bits 0 and 4 that lock the other lock bits */
            pMifareConnection->aLockBytes[2] |= 0x11;
         }

         /* Write lock bytes */
         static_PMifareWriteLockBytes(pContext, pMifareConnection);

         break;

      default:
         /* Return an error */
         PDebugError("static_PMifareExecuteQueuedExchange: unknown type of operation!");
         static_PMifareSendResult(pContext, pMifareConnection, W_ERROR_BAD_STATE);
      }
   }

   /* Reset data */
   CMemoryFill(&pMifareConnection->sQueuedOperation, 0, sizeof(pMifareConnection->sQueuedOperation));
}

/* See Client API Specifications */
void PMifareRead(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void *pCallbackParameter,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            W_HANDLE *phOperation )
{
   tMifareConnection* pMifareConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nError;

   PDebugTrace("PMifareRead");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareRead: Bad handle");
      goto return_error;
   }

   /* For now, we only support MIFARE UL and MIFARE UL-C */
   if ((pMifareConnection->nType != W_PROP_MIFARE_UL) && (pMifareConnection->nType != W_PROP_MIFARE_UL_C))
   {
      PDebugError("PMifareRead: not a Mifare UL / UL-C");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   /* For reading blocks from 3 to n, check if access rights have been retrieved */
   /* Blocks 0 to 2 are always readable */
   if ( W_PROP_MIFARE_UL_C == pMifareConnection->nType && pMifareConnection->bULCAccessRightsRetrieved == W_FALSE &&
        (nOffset + nLength) > pMifareConnection->pSectorSize->pMultiply(P_MIFARE_FIRST_DATA_BLOCK) )
   {
      PDebugError("PMifareRead : called without prior call to PMifareULRetrieveAccessRights");
      nError = W_ERROR_MISSING_INFO;
      goto return_error;
   }

   /* Check parameters */
   if ( (null == pBuffer) || (0 == nLength) )
   {
      PDebugError("PMifareRead: null parameter");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* for Mifare UL-C, specific check to avoid read of the keys, which can make the card unusable... */
   if (W_PROP_MIFARE_UL_C == pMifareConnection->nType)
   {
      if ( (nOffset + nLength) > pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_KEY_BLOCK) )
      {
         PDebugError("PMifareRead: the data to read/write is too large (reading keys is not allowed)");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }
   }
   else
   {
      if ( (nOffset + nLength) > pMifareConnection->pSectorSize->pMultiply(pMifareConnection->nSectorNumber) )
      {
         PDebugError("PMifareRead: the data to read/write is too large");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PMifareRead: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PMifareRead: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMifareExecuteQueuedExchange, pMifareConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Store the operation handle */
      CDebugAssert(pMifareConnection->hOperation == W_NULL_HANDLE);
      pMifareConnection->hOperation = hOperation;

      /* Store the callback context */
      pMifareConnection->sCallbackContext = sCallbackContext;

      PNDEF2GenRead(pContext,
                    hConnection,
                    static_PMifareReadCompleted,
                    pMifareConnection,
                    pBuffer,
                    nOffset,
                    nLength);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Save the operation handle */
      CDebugAssert(pMifareConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pMifareConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pMifareConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMifareConnection->sQueuedOperation.nType = P_MIFARE_QUEUED_READ;

      /* Save data */
      pMifareConnection->sQueuedOperation.pBuffer = pBuffer;
      pMifareConnection->sQueuedOperation.nOffset = nOffset;
      pMifareConnection->sQueuedOperation.nLength = nLength;

      return;

   default:
      if(hOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hOperation);
      }

      /* Return this error */
      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("PMifareRead: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
void PMifareWrite(
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
   tMifareConnection* pMifareConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nError;
   uint32_t nIndex;

   PDebugTrace("PMifareWrite");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareWrite: error occured");
      goto return_error;
   }

   /* For now, we only support MIFARE UL and MIFARE UL-C */
   if ((pMifareConnection->nType != W_PROP_MIFARE_UL) && (pMifareConnection->nType != W_PROP_MIFARE_UL_C))
   {
      PDebugError("PMifareWrite: not a Mifare UL / UL-C");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   /* Check if access rights have been retrieved */
   if (W_PROP_MIFARE_UL_C == pMifareConnection->nType && pMifareConnection->bULCAccessRightsRetrieved == W_FALSE)
   {
      PDebugError("PMifareWrite : called without prior call to PMifareULRetrieveAccessRights");
      nError = W_ERROR_MISSING_INFO;
      goto return_error;
   }

   /* Check the parameters */
   if ((pBuffer == null) && (bLockSectors == W_FALSE))
   {
      /* pBuffer null is only allowed for lock */
      PDebugError("PMifareWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nLength == 0) && ((pBuffer != null) || (nOffset != 0) || (bLockSectors == W_FALSE)))
   {
      /* nLength == 0 is only valid for whole tag lock */
      PDebugError("PMifareWrite: W_ERROR_BAD_PARAMETER");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( (nOffset + nLength) > pMifareConnection->pSectorSize->pMultiply(pMifareConnection->nSectorNumber) )
   {
      PDebugError("PMifareWrite: the data to read/write is too large");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* for Mifare UL-C, specific check to avoid direct write of the keys, which can
      make the card unusable... */
   if (pMifareConnection->nType == W_PROP_MIFARE_UL_C)
   {
      if ((nOffset + nLength) > pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_KEY_BLOCK))
      {
         PDebugError("PMifareWrite: direct write of the key is not supported (unsafe) - use the specialised API !!!");
         nError = W_ERROR_FEATURE_NOT_SUPPORTED;
         goto return_error;
      }
   }

   if ((pBuffer == null) && (nOffset == 0) && (nLength == 0))
   {
      /* specific case used to lock the entire data area of the card */
      if (pMifareConnection->nType == W_PROP_MIFARE_UL)
      {
         nOffset =  pMifareConnection->pSectorSize->pMultiply(P_MIFARE_FIRST_DATA_BLOCK);
         nLength =  pMifareConnection->pSectorSize->pMultiply(P_MIFARE_UL_LAST_DATA_BLOCK - P_MIFARE_FIRST_DATA_BLOCK + 1);
      }
      else
      {
         nOffset =  pMifareConnection->pSectorSize->pMultiply(P_MIFARE_FIRST_DATA_BLOCK);
         nLength =  pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_LAST_DATA_BLOCK - P_MIFARE_FIRST_DATA_BLOCK + 1);
      }
   }

   /* Check if the Mifare card is locked/lockable */
   for (nIndex = pMifareConnection->pSectorSize->pDivide(nOffset);
            nIndex <= pMifareConnection->pSectorSize->pDivide(nOffset + nLength - 1); nIndex++)
   {
      if ((pBuffer != null) && (static_PMifareIsSectorLocked(pContext, pMifareConnection, nIndex) != W_FALSE))
      {
         PDebugError("PMifareWrite: item locked");
         nError = W_ERROR_ITEM_LOCKED;
         goto return_error;
      }

      if ((bLockSectors != W_FALSE) && (static_PMifareIsSectorLockable(pContext, pMifareConnection, nIndex) == W_FALSE))
      {
         PDebugError("PMifareWrite: item not lockable");
         nError = W_ERROR_ITEM_LOCKED;
         goto return_error;
      }
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PMifareWrite: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PMifareWrite: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMifareExecuteQueuedExchange, pMifareConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Store the operation handle */
      CDebugAssert(pMifareConnection->hOperation == W_NULL_HANDLE);
      pMifareConnection->hOperation = hOperation;

      /* Store the callback context */
      pMifareConnection->sCallbackContext = sCallbackContext;

      /* Store the exchange information */
      if(bLockSectors != W_FALSE)
      {
         pMifareConnection->nOffsetToLock = nOffset;
         pMifareConnection->nLengthToLock = nLength;
      }
      else
      {
         pMifareConnection->nOffsetToLock = 0;
         pMifareConnection->nLengthToLock = 0;
      }

      if (pBuffer != null)
      {
         PNDEF2GenWrite(pContext,
                        hConnection,
                        static_PMifareWriteCompleted,
                        pMifareConnection,
                        pBuffer,
                        nOffset,
                        nLength);
      }
      else
      {
         static_PMifareWriteCompleted(pContext, pMifareConnection, W_SUCCESS);
      }

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Save the operation handle */
      CDebugAssert(pMifareConnection->sQueuedOperation.hOperation == W_NULL_HANDLE);
      pMifareConnection->sQueuedOperation.hOperation = hOperation;

      /* Save callback context */
      pMifareConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMifareConnection->sQueuedOperation.nType = P_MIFARE_QUEUED_WRITE;

      /* Save data */
      pMifareConnection->sQueuedOperation.pBuffer = (uint8_t*)pBuffer;
      pMifareConnection->sQueuedOperation.nOffset = nOffset;
      pMifareConnection->sQueuedOperation.nLength = nLength;
      pMifareConnection->sQueuedOperation.bLockSectors = bLockSectors;

      return;

   default:
      if(hOperation != W_NULL_HANDLE)
      {
         PHandleClose(pContext, hOperation);
      }

      /* Return this error */
      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("PMifareWrite: returning %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
W_ERROR WMifareReadSync(
            W_HANDLE hConnection,
            uint8_t *pBuffer,
            uint32_t nOffset,
            uint32_t nLength )
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareReadSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareRead(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pBuffer, nOffset, nLength,
            null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See Client API Specifications */
W_ERROR WMifareWriteSync(
            W_HANDLE hConnection,
            const uint8_t* pBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            bool_t bLockCard )
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareWriteSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareWrite(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pBuffer, nOffset, nLength, bLockCard,
            null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See client API specification */
W_ERROR PMifareULForceULC(
                  tContext * pContext,
                  W_HANDLE hConnection)
{
   tMifareConnection* pMifareConnection = null;
   W_ERROR            nError;

   PDebugTrace("PMifareULForceULC");

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareULForceULC : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      return nError;
   }

   switch (pMifareConnection->nType)
   {
      case W_PROP_MIFARE_UL :

         /* Create the smart cache for the dynamic area (>64bytes) */
         if ( (nError = PNDEF2GenCreateSmartCacheDynamic(pContext, hConnection, P_MIFARE_BLOCK_NUMBER_UL_C))  != W_SUCCESS)
         {
            PDebugError("PMifareULForceULC : PNDEF2GenCreateSmartCacheDynamic returned %s", PUtilTraceError(nError));
            return nError;
         }

         pMifareConnection->bBypassULCAccessRightsRetrieved = W_TRUE;

         /* Update the sector number */
         pMifareConnection->nSectorNumber = P_MIFARE_BLOCK_NUMBER_UL_C;

         /* upgrade the connection to a Mifare UL-C */
         pMifareConnection->nType = W_PROP_MIFARE_UL_C;
         break;

      case W_PROP_MIFARE_UL_C :
         /* already a Mifare UL-C connection, nothing to to */
         break;

      default :
         PDebugError("PMifareULForceULC : not a Mifare UL / UL-C connection");
         return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   return W_SUCCESS;
}

/* ------------------------------------------------------ */

/* See client API */
void PMifareULFreezeDataLockConfiguration(
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tMifareConnection* pMifareConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PMifareULFreezeDataLockConfiguration");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareULFreezeDataLockConfiguration : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   if ((pMifareConnection->nType != W_PROP_MIFARE_UL) && (pMifareConnection->nType != W_PROP_MIFARE_UL_C))
   {
      /* Not a Mifare UL / Mifare UL-C connection */
      PDebugError("PMifareULFreezeDataLockConfiguration : not a Mifare UL / UL-C");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check if access rights have been retrieved */
   if (W_PROP_MIFARE_UL_C == pMifareConnection->nType && pMifareConnection->bULCAccessRightsRetrieved == W_FALSE)
   {
      PDebugError("PMifareULFreezeDataLockConfiguration : called without prior call to PMifareULRetrieveAccessRights");
      nError = W_ERROR_MISSING_INFO;
      goto return_error;
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMifareExecuteQueuedExchange, pMifareConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Save callback context */
      pMifareConnection->sCallbackContext = sCallbackContext;

      /* LOCK byte # 0 : Set the bits 1 and 2 that lock the other lock bits */
      pMifareConnection->aLockBytes[0] |= 0x06;

      if (W_PROP_MIFARE_UL_C == pMifareConnection->nType)
      {
         /* LOCK byte # 3 : Set the bits 0 and 4 that lock the other lock bits */
         pMifareConnection->aLockBytes[2] |= 0x11;
      }

      /* Write lock bytes */
      static_PMifareWriteLockBytes(pContext, pMifareConnection);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Save callback context */
      pMifareConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMifareConnection->sQueuedOperation.nType = P_MIFARE_QUEUED_FREEZE_LOCK_CONF;

      return;

   default:
      goto return_error;
   }

return_error :

   /* Send the error */
   PDFCPostContext2(&sCallbackContext, nError);
}

/* See client API */
W_ERROR WMifareULFreezeDataLockConfigurationSync(
      W_HANDLE hConnection)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareULFreezeDataLockConfigurationSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareULFreezeDataLockConfiguration(
            hConnection,
            PBasicGenericSyncCompletion,
            &param);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* ------------------------------------------------------ */

/* See client API */
void PMifareULRetrieveAccessRights (
                  tContext * pContext,
                  W_HANDLE hConnection,
                  tPBasicGenericCallbackFunction *pCallback,
                  void *pCallbackParameter)
{
   tMifareConnection* pMifareConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PMifareULRetrieveAccessRights");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareULRetrieveAccessRights : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   /* Only Mifare UL / UL-C are supported */
   if ((pMifareConnection->nType != W_PROP_MIFARE_UL) && (pMifareConnection->nType != W_PROP_MIFARE_UL_C))
   {
      PDebugError("PMifareULRetrieveAccessRights: Not a Mifare UL / UL-C");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMifareExecuteQueuedExchange, pMifareConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Store the callback context */
      pMifareConnection->sCallbackContext = sCallbackContext;

      /* For UL-C, access rights (lock and auth bytes) must be read */
      if (W_PROP_MIFARE_UL_C == pMifareConnection->nType)
      {
         /* Init automaton */
         pMifareConnection->nCurrentOperationState = 0;

         /* Read Lock bytes 2-3 (in block 0x28) */
         PNDEF2GenRead(pContext,
                       pMifareConnection->hConnection,
                       static_PMifareULCRetrieveAccessRightsAutomaton,
                       pMifareConnection,
                       &pMifareConnection->aLockBytes[2],
                       pMifareConnection->pSectorSize->pMultiply(P_MIFARE_ULC_LOCK_BLOCK),
                       P_MIFARE_ULC_LOCK_LENGTH);
      }
      else
      {
         /* For Mifare UL, only the lock bytes are needed, and have been already read, so nothing to do */
         static_PMifareSendResult(pContext, pMifareConnection, W_SUCCESS);
      }

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Save callback context */
      pMifareConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMifareConnection->sQueuedOperation.nType = P_MIFARE_QUEUED_RETRIEVE_ACCESS_RIGHTS;

      return;

   default:
      goto return_error;
   }

return_error :

   /* Send the error */
   PDFCPostContext2(&sCallbackContext, nError);
}


void PMifareULInitializeAccessRightsAccordingToType2TagCC(
      tContext * pContext,
      W_HANDLE hConnection,
      bool_t bLocked,
      tPBasicGenericCallbackFunction *pCallback,
      void *pCallbackParameter)
{
   tMifareConnection* pMifareConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PMifareULInitializeAccessRightsAccordingToType2TagCC");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareULInitializeAccessRightsAccordingToType2TagCC : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   /* Only Mifare UL / UL-C are supported */
   if ((pMifareConnection->nType != W_PROP_MIFARE_UL) && (pMifareConnection->nType != W_PROP_MIFARE_UL_C))
   {
      PDebugError("PMifareULInitializeAccessRightsAccordingToType2TagCC: Not a Mifare UL / UL-C");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* For UL-C, access rights (lock and auth bytes) must be read */
   if (W_PROP_MIFARE_UL_C == pMifareConnection->nType && pMifareConnection->bULCAccessRightsRetrieved == W_FALSE)
   {
      if (bLocked == W_FALSE)
      {
         pMifareConnection->aLockBytes[2] = 0x00;
         pMifareConnection->aLockBytes[3] = 0x00;
      }
      else
      {
         pMifareConnection->aLockBytes[2] = 0xFF;
         pMifareConnection->aLockBytes[3] = 0xFF;
      }

      pMifareConnection->nAuthenticateThreshold = 0x30;
      pMifareConnection->nAuthenticateMode = 0x01;
      pMifareConnection->bULCAccessRightsRetrieved = W_TRUE;
   }

   /* For Mifare UL, only the lock bytes are needed, and have been already read, so nothing to do */
   PDFCPostContext2(&sCallbackContext, W_SUCCESS);
   return;

return_error:

   /* Send the error */
   PDFCPostContext2(&sCallbackContext, nError);
}


/* See client API */
W_ERROR WMifareULRetrieveAccessRightsSync (
                  W_HANDLE hConnection)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareULRetrieveAccessRightsSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareULRetrieveAccessRights(
            hConnection,
            PBasicGenericSyncCompletion,
            &param);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See header */
W_ERROR PMifareNDEF2Lock(tContext * pContext,
                         W_HANDLE hConnection)
{
   tMifareConnection* pMifareConnection = null;
   W_ERROR nError;

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);
   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareNDEF2Lock : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      return nError;
   }

   /* Only Mifare UL / UL-C are supported */
   if ((pMifareConnection->nType != W_PROP_MIFARE_UL) && (pMifareConnection->nType != W_PROP_MIFARE_UL_C))
   {
      PDebugError("PMifareNDEF2Lock: Not a Mifare UL / UL-C");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Update lock bytes */
   CMemoryFill(pMifareConnection->aLockBytes, 0xFF, sizeof(pMifareConnection->aLockBytes));

   return W_SUCCESS;
}

/* ------------------------------------------------------ */

/* See client API */
W_ERROR PMifareULGetAccessRights (
                  tContext * pContext,
                  W_HANDLE hConnection,
                  uint32_t nOffset,
                  uint32_t nLength,
                  uint32_t *pnRights)
{
   tMifareConnection* pMifareConnection = null;
   bool_t               bIsLocked;
   W_ERROR nError;
   uint32_t i, nStart, nStop;

   PDebugTrace("PMifareULGetAccessRights");

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);
   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareULGetAccessRights : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      return (nError);
   }

   /* Only Mifare UL / UL-C are supported */
   if ((pMifareConnection->nType != W_PROP_MIFARE_UL) && (pMifareConnection->nType != W_PROP_MIFARE_UL_C))
   {
      PDebugError("PMifareULGetAccessRights : not a Mifare UL / UL-C");
      return (W_ERROR_CONNECTION_COMPATIBILITY);
   }

   /* Check if access rights have been retrieved */
   if (W_PROP_MIFARE_UL_C == pMifareConnection->nType && pMifareConnection->bULCAccessRightsRetrieved == W_FALSE)
   {
      PDebugError("PMifareULGetAccessRights : called without prior call to PMifareULRetrieveAccessRights");
      return (W_ERROR_MISSING_INFO);
   }

   /* Check parameters */
   if (null == pnRights)
   {
      PDebugError("PMifareULGetAccessRights : pnRights == null");
      return (W_ERROR_BAD_PARAMETER);
   }

   nStart = pMifareConnection->pSectorSize->pDivide(nOffset);
   nStop  = pMifareConnection->pSectorSize->pDivide(nOffset + nLength - 1);

   if ((nStart >= pMifareConnection->nSectorNumber) || (nStop >= pMifareConnection->nSectorNumber))
   {
      PDebugError("PMifareULGetAccessRights : specified area goes beyond end of the tag");
      return (W_ERROR_BAD_PARAMETER);
   }

   /* Ok, all is fine, compute the access rights */

   /* compute lock access */

   * pnRights = 0;

   bIsLocked = static_PMifareIsSectorLocked(pContext, pMifareConnection, nStart);

   for (i = nStart + 1; i <= nStop; i++)
   {
      if (static_PMifareIsSectorLocked(pContext, pMifareConnection, i) != bIsLocked)
      {
         /* The locks are not consistent through the whole area */
         return (W_ERROR_HETEROGENEOUS_DATA);
      }
   }

   if (pMifareConnection->nType == W_PROP_MIFARE_UL_C)
   {
      /* Mifare UL-C */
      if ((nStart < pMifareConnection->nAuthenticateThreshold) && (pMifareConnection->nAuthenticateThreshold <= nStop))
      {
         /* the authentication access is not consistent through all the whole area */
         return (W_ERROR_HETEROGENEOUS_DATA);
      }

      if (nStop < pMifareConnection->nAuthenticateThreshold)
      {
         /* we are under the authenticate threshold */

         * pnRights |= W_MIFARE_UL_READ_OK;

         if (bIsLocked != W_FALSE)
         {
            * pnRights |= (W_MIFARE_UL_WRITE_LOCKED << 4);
         }
         else
         {
            * pnRights |= (W_MIFARE_UL_WRITE_OK << 4);
         }
      }
      else
      {
         /* we are above the authenticate threshold */

         if (pMifareConnection->nAuthenticateMode & 0x01)
         {
            * pnRights |= W_MIFARE_UL_READ_OK;
         }
         else
         {
            * pnRights |= W_MIFARE_UL_READ_AUTHENTICATED;
         }

         if (bIsLocked != W_FALSE)
         {
            * pnRights |= (W_MIFARE_UL_WRITE_LOCKED << 4);
         }
         else
         {
            * pnRights |= (W_MIFARE_UL_WRITE_AUTHENTICATED << 4);
         }
      }
   }
   else
   {
      /* Mifare UL */

      * pnRights |= W_MIFARE_UL_READ_OK;

      if (bIsLocked)
      {
         * pnRights |= (W_MIFARE_UL_WRITE_LOCKED << 4);
      }
      else
      {
         * pnRights |= W_MIFARE_UL_WRITE_OK;
      }
   }

   return (W_SUCCESS);
}

/* ------------------------------------------------------ */

/* See client API */
void PMifareULCSetAccessRights (
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericCallbackFunction *pCallback,
      void *pCallbackParameter,
      const uint8_t * pKey,
      uint32_t nKeyLength,
      uint8_t nThreshold,
      uint32_t nRights,
      bool_t bLockConfiguration)
{
   tMifareConnection* pMifareConnection = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t i;
   uint8_t nRequestedAuthenticateMode = 0;

   PDebugTrace("PMifareULCSetAccessRights");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareULSetAccessRights : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   /* Only Mifare UL-C are supported */
   if (pMifareConnection->nType != W_PROP_MIFARE_UL_C)
   {
      PDebugError("PMifareULCSetAccessRights : not a Mifare UL-C connection");
      nError =  W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Check if access rights have been retrieved */
   if (pMifareConnection->bULCAccessRightsRetrieved == W_FALSE)
   {
      PDebugError("PMifareULSetAccessRights : called without prior call to PMifareULRetrieveAccessRights");
      nError = W_ERROR_MISSING_INFO;
      goto return_error;
   }

   /* check the key pointer */
   if (pKey == null)
   {
      PDebugError("PMifareULSetAccessRights : pKey is null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if(nKeyLength != 16)
   {
      PDebugError("PMifareULSetAccessRights : wrong key length");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* check the Threshold value */
   if ((nThreshold < P_MIFARE_FIRST_DATA_BLOCK) || (nThreshold > P_MIFARE_BLOCK_NUMBER_UL_C))
   {
      PDebugError("PMifareULSetAccessRights : invalid threashold value");
      nError =  W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the rigths value */
   switch (nRights)
   {
      case W_MIFARE_UL_READ_OK | (W_MIFARE_UL_WRITE_AUTHENTICATED << 4) :
         /* write authentication */
         nRequestedAuthenticateMode = 0x01;
         break;

      case W_MIFARE_UL_READ_AUTHENTICATED | (W_MIFARE_UL_WRITE_AUTHENTICATED << 4) :
         /* both read and write authentication */
         nRequestedAuthenticateMode = 0x00;
         break;

      default :
         PDebugError("PMifareULSetAccessRights : invalid access rights value");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMifareExecuteQueuedExchange, pMifareConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Store the callback context */
      pMifareConnection->sCallbackContext = sCallbackContext;

      /* Store data */
      for (i=0; i<8; i++)
      {
         pMifareConnection->aKey[i]   = pKey[7-i];
         pMifareConnection->aKey[8+i] = pKey[15-i];
      }
      pMifareConnection->nRequestedAuthenticateThreshold = nThreshold;
      pMifareConnection->nRequestedAuthenticateMode = nRequestedAuthenticateMode;
      pMifareConnection->bLockAuthentication = bLockConfiguration;

      /* set the current opeation state */
      pMifareConnection->nCurrentOperationState = 0;

      /* all checks are done, perform the operation */
      static_PMifareULCSetAccessRightsAutomaton(pContext, pMifareConnection, W_SUCCESS);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Save callback context */
      pMifareConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMifareConnection->sQueuedOperation.nType = P_MIFARE_QUEUED_SET_ACCESS_RIGHTS;

      /* Save data */
      pMifareConnection->sQueuedOperation.pKey = pKey;
      pMifareConnection->sQueuedOperation.nRequestedAuthenticateThreshold = nThreshold;
      pMifareConnection->sQueuedOperation.nRequestedAuthenticateMode = nRequestedAuthenticateMode;
      pMifareConnection->sQueuedOperation.bLockAuthentication = bLockConfiguration;

      return;

   default:
      goto return_error;
   }

return_error:

   /* Send the error */
   PDFCPostContext2(&sCallbackContext, nError);
}


/* See client API */
W_ERROR WMifareULCSetAccessRightsSync (
      W_HANDLE hConnection,
      const uint8_t * pKey,
      uint32_t nKeyLength,
      uint8_t nThreshold,
      uint32_t nRights,
      bool_t bLockConfiguration)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareULCSetAccessRightsSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareULCSetAccessRights(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pKey, nKeyLength,
            nThreshold,
            nRights,
            bLockConfiguration);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* ------------------------------------------------------ */

/* See Client API */
void PMifareULCAuthenticate (
      tContext * pContext,
      W_HANDLE hConnection,
      tPBasicGenericCallbackFunction *pCallback,
      void *pCallbackParameter,
      const uint8_t * pKey,
      uint32_t nKeyLength)
{
   tMifareConnection* pMifareConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR             nError;

   PDebugTrace("PMifareULCAuthenticate");

   /* Prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if (nError != W_SUCCESS)
   {
      PDebugError("PMifareULCAuthenticate : PReaderUserGetConnectionObject returned %s", PUtilTraceError(nError));
      goto return_error;
   }

   /* Only Mifare UL-C are supported */
   if (pMifareConnection->nType != W_PROP_MIFARE_UL_C)
   {
      PDebugError("PMifareULCAuthenticate : not a Mifare UL-C connection");
      nError =  W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* check the key pointer */
   if ( ((pKey != null) && (nKeyLength != 16)) ||
        ((pKey == null) && (nKeyLength != 0)) )
   {
      PDebugError("PMifareULCAuthenticate : wrong key length");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PMifareExecuteQueuedExchange, pMifareConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Store the callback context */
      pMifareConnection->sCallbackContext = sCallbackContext;

      if (pKey != null)
      {
         CMemoryCopy(pMifareConnection->aKey, pKey, 16);
      }
      else
      {
         PDebugWarning("PMifareULCAuthenticate : pKey is null, using default key");
         CMemoryCopy(pMifareConnection->aKey, g_MifareULCDefaultKey, sizeof(g_MifareULCDefaultKey));
      }

      /* set the current opeation state */
      pMifareConnection->nCurrentOperationState = 0;

      /* all checks are done, perform the operation */
      static_PMifareULCAuthenticateAutomaton(pContext, pMifareConnection, 0, W_SUCCESS);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the reference count to keep the connection object alive during the operation.
         The reference count is decreased in static_PMifareSendResult() when the operation is completed */
      PHandleIncrementReferenceCount(pMifareConnection);

      /* Save callback context */
      pMifareConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pMifareConnection->sQueuedOperation.nType = P_MIFARE_QUEUED_AUTHENTICATE;

      /* Save data */
      pMifareConnection->sQueuedOperation.pKey = pKey;

      return;

   default:
      goto return_error;
   }

return_error:

   /* Send the error */
   PDFCPostContext2(&sCallbackContext, nError);
}

/* See client API */
W_ERROR WMifareULCAuthenticateSync (
                  W_HANDLE hConnection,
                  const uint8_t* pKey,
                  uint32_t nKeyLength)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareULCAuthenticateSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareULCAuthenticate(
            hConnection,
            PBasicGenericSyncCompletion,
            &param,
            pKey, nKeyLength);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

#ifdef P_INCLUDE_MIFARE_CLASSIC

/* -------------------------  API Mifare Classic ---------------------------*/

#define P_MIFARE_CLASSIC_HIDLE                        0

#define P_MIFARE_CLASSIC_CMD_AUTH_A                   0x60
#define P_MIFARE_CLASSIC_CMD_AUTH_B                   0x61
#define P_MIFARE_CLASSIC_CMD_AUTH_LENGTH              8
#define P_MIFARE_CLASSIC_CMD_AUTH_KEY_LENGTH          6

#define P_MIFARE_CLASSIC_CMD_READ_BLOCK               0x30
#define P_MIFARE_CLASSIC_CMD_READ_BLOCK_LENGTH        2

#define P_MIFARE_CLASSIC_CMD_WRITE_BLOCK              0xA0
#define P_MIFARE_CLASSIC_CMD_WRITE_BLOCK_LENGTH       18

#define P_MIFARE_CLASSIC_1K_MAX_TAG_SIZE              716
#define P_MIFARE_CLASSIC_4K_MAX_TAG_SIZE              3356

#define P_MIFARE_CLASSIC_BIT_SECTOR_TRAILER          0x03
#define P_MIFARE_CLASSIC_BIT_BLOCK_2                 0x02
#define P_MIFARE_CLASSIC_BIT_BLOCK_1                 0x01
#define P_MIFARE_CLASSIC_BIT_BLOCK_0                 0x00


const static uint8_t static_gaSectorTrailerAccessConditionKey[2][8] = {
   /* --------- Use Key B ---------- */
                  { /* C1 C2 C3 => 0 0 0 */
                  0,

                  /* C1 C2 C3 => 0 0 1 */
                  0,

                  /* C1 C2 C3 => 0 1 0 */
                  0,

                  /* C1 C2 C3 => 0 1 1 */
                  (P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_A |
                  P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE | P_MIFARE_CLASSIC_ACCESS_WRITE_ACCESS_BYTE |
                  P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_B),

                  /* C1 C2 C3 => 1 0 0 */
                  (P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_A |
                  P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE |
                  P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_B),

                  /* C1 C2 C3 => 1 0 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE | P_MIFARE_CLASSIC_ACCESS_WRITE_ACCESS_BYTE),

                  /* C1 C2 C3 => 1 1 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE),

                  /* C1 C2 C3 => 1 1 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE)
                  },
      /* --------- Use Key A ---------- */

                  {/* C1 C2 C3 => 0 0 0 */
                  (P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_A | P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE |
                  P_MIFARE_CLASSIC_ACCESS_READ_KEY_B | P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_B),

                  /* C1 C2 C3 => 0 0 1 */
                  (P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_A |
                  P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE | P_MIFARE_CLASSIC_ACCESS_WRITE_ACCESS_BYTE |
                  P_MIFARE_CLASSIC_ACCESS_READ_KEY_B | P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_B),

                  /* C1 C2 C3 => 0 1 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE | P_MIFARE_CLASSIC_ACCESS_READ_KEY_B),

                  /* C1 C2 C3 => 0 1 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE),

                  /* C1 C2 C3 => 1 0 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE),

                  /* C1 C2 C3 => 1 0 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE),

                  /* C1 C2 C3 => 1 1 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE),

                  /* C1 C2 C3 => 1 1 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ_ACCESS_BYTE)
                  }};


const static uint8_t static_gaBlockAccessConditionKey[2][8]=
{
            /* ----------- Use Key B ------------ */
                  { /* C1 C2 C3 => 0 0 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ | P_MIFARE_CLASSIC_ACCESS_WRITE),

                  /* C1 C2 C3 => 0 0 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ),

                  /* C1 C2 C3 => 0 1 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ),

                  /* C1 C2 C3 => 0 1 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ | P_MIFARE_CLASSIC_ACCESS_WRITE),

                  /* C1 C2 C3 => 1 0 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ | P_MIFARE_CLASSIC_ACCESS_WRITE),

                  /* C1 C2 C3 => 1 0 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ),

                  /* C1 C2 C3 => 1 1 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ | P_MIFARE_CLASSIC_ACCESS_WRITE),

                  /* C1 C2 C3 => 1 1 1 */
                  0},

            /* -------------- Use Key A ---------- */


                   { /* C1 C2 C3 => 0 0 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ | P_MIFARE_CLASSIC_ACCESS_WRITE),

                  /* C1 C2 C3 => 0 0 1 */
                  (P_MIFARE_CLASSIC_ACCESS_READ),

                  /* C1 C2 C3 => 0 1 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ),

                  /* C1 C2 C3 => 0 1 1 */
                  0,

                  /* C1 C2 C3 => 1 0 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ),

                  /* C1 C2 C3 => 1 0 1 */
                  0,

                  /* C1 C2 C3 => 1 1 0 */
                  (P_MIFARE_CLASSIC_ACCESS_READ),

                  /* C1 C2 C3 => 1 1 1 */
                  0}
};



W_ERROR PMifareClassicGetAccessConditions(
      uint8_t  nSectorNumber,
      bool_t   bUseKeyA,
      const uint8_t * pAccessCondition,
      uint32_t nAccessContionLength,
      tPMifareClassicAccessConditions * pMifareClassicAccess)
{
   /* Verify the integrity of the conditions access */

   uint8_t C1 = 0x0F & (~ pAccessCondition[0]);
   uint8_t C2 = 0x0F & ((~ pAccessCondition[0]) >> 4);
   uint8_t C3 = 0x0F & (~ pAccessCondition[1]);

   uint8_t nAccessBitSectorTrailer, nAccessBlock0, nAccessBlock1, nAccessBlock2;

   if( (C1 != (pAccessCondition[1] >> 4))
     ||(C2 != (pAccessCondition[2] & 0x0F))
     ||(C3 != ((pAccessCondition[2] & 0xF0) >> 4)))
   {
      return W_ERROR_TAG_DATA_INTEGRITY;
   }

   nAccessBitSectorTrailer = (((C1 >> P_MIFARE_CLASSIC_BIT_SECTOR_TRAILER) & 0x01) << 2) +
                             (((C2 >> P_MIFARE_CLASSIC_BIT_SECTOR_TRAILER) & 0x01) << 1) +
                             ( (C3 >> P_MIFARE_CLASSIC_BIT_SECTOR_TRAILER) & 0x01);

   nAccessBlock0           = (((C1 >> P_MIFARE_CLASSIC_BIT_BLOCK_0) & 0x01) << 2) +
                             (((C2 >> P_MIFARE_CLASSIC_BIT_BLOCK_0) & 0x01) << 1) +
                             ( (C3 >> P_MIFARE_CLASSIC_BIT_BLOCK_0) & 0x01) ;

   nAccessBlock1           = (((C1 >> P_MIFARE_CLASSIC_BIT_BLOCK_1) & 0x01) << 2) +
                             (((C2 >> P_MIFARE_CLASSIC_BIT_BLOCK_1) & 0x01) << 1) +
                             ( (C3 >> P_MIFARE_CLASSIC_BIT_BLOCK_1) & 0x01) ;

   nAccessBlock2           = (((C1 >> P_MIFARE_CLASSIC_BIT_BLOCK_2) & 0x01) << 2) +
                             (((C2 >> P_MIFARE_CLASSIC_BIT_BLOCK_2) & 0x01) << 1) +
                             ( (C3 >> P_MIFARE_CLASSIC_BIT_BLOCK_2) & 0x01) ;

   bUseKeyA = !!bUseKeyA;

   pMifareClassicAccess->nSectorTrailer = static_gaSectorTrailerAccessConditionKey[bUseKeyA][nAccessBitSectorTrailer];

   if(nSectorNumber < P_MIFARE_CLASSIC_4K_EXTENDED_SECTOR)
   {
      pMifareClassicAccess->nNumberOfBlock = 3;
      pMifareClassicAccess->aBlock[0] = static_gaBlockAccessConditionKey[bUseKeyA][nAccessBlock0];
      pMifareClassicAccess->aBlock[1] = static_gaBlockAccessConditionKey[bUseKeyA][nAccessBlock1];
      pMifareClassicAccess->aBlock[2] = static_gaBlockAccessConditionKey[bUseKeyA][nAccessBlock2];
   }else
   {
      pMifareClassicAccess->nNumberOfBlock = 15;
      CMemoryFill( &pMifareClassicAccess->aBlock[0], static_gaBlockAccessConditionKey[bUseKeyA][nAccessBlock0], 5);
      CMemoryFill( &pMifareClassicAccess->aBlock[5], static_gaBlockAccessConditionKey[bUseKeyA][nAccessBlock1], 5);
      CMemoryFill( &pMifareClassicAccess->aBlock[10], static_gaBlockAccessConditionKey[bUseKeyA][nAccessBlock2], 5);
   }

   return W_SUCCESS;
}

W_ERROR PMifareClassicCheckType7(
                  tContext* pContext,
                  W_HANDLE hConnection,
                  uint32_t* pnMaximumTagSize,
                  bool_t* pbIsLocked,
                  bool_t* pbIsLockable,
                  bool_t* pbIsFormattable)
{
   W_ERROR nError = W_SUCCESS;
   tMifareConnection * pMifareConnection;

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if(nError != W_SUCCESS)
   {
      PDebugError("error during the retrieve of the Mifare Connection instance");
      return nError;
   }


   if( pMifareConnection->nType == W_PROP_MIFARE_1K)
   {
      *pnMaximumTagSize = P_MIFARE_CLASSIC_1K_MAX_TAG_SIZE;
   }else if(pMifareConnection->nType == W_PROP_MIFARE_4K)
   {
      *pnMaximumTagSize = P_MIFARE_CLASSIC_4K_MAX_TAG_SIZE;
   }else
   {
      PDebugError("Invalid object connection. This API must be only used for Mifare Classic 1K/4K");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   *pbIsLocked       = W_FALSE;
   *pbIsLockable     = W_TRUE;
   *pbIsFormattable  = W_TRUE;

   return W_SUCCESS;

}


uint8_t static_PMifareClassicGetNumberOfSector(uint8_t nCardType)
{
   switch(nCardType)
   {
      case W_PROP_MIFARE_1K:
         return P_MIFARE_CLASSIC_1K_NUMBER_OF_SECTOR;
      case W_PROP_MIFARE_4K:
         return P_MIFARE_CLASSIC_4K_NUMBER_OF_SECTOR;
      default:
         return 0;
   }
}

W_ERROR static_PMifareClassicGetBlockAddressFromSector(
         uint8_t nCardType,
         uint8_t nSectorNumber,
         uint8_t * nBlockAddr)
{
   uint8_t nMaxNumberOfSector = static_PMifareClassicGetNumberOfSector(nCardType);
   *nBlockAddr = 0;


   if(nMaxNumberOfSector <= nSectorNumber)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if( (nCardType == W_PROP_MIFARE_4K)
    && (nSectorNumber >= 32 ))
   {
      *nBlockAddr = (uint8_t) ((32 * 4) + ( (nSectorNumber - 32) * 16 ));
   }else
   {
      *nBlockAddr = (uint8_t) (nSectorNumber * 4);
   }

   return W_SUCCESS;
}

W_ERROR static_PMifareClassicGetSectorNumberFromBlockNumber(
         uint8_t nCardType,
         uint8_t nBlockNumber,
         uint8_t * pnSector)
{
   *pnSector = 0;

   if(nCardType == W_PROP_MIFARE_4K)
   {
      if(nBlockNumber < 128)
      {
         *pnSector = nBlockNumber / 4;
      }
      else
      {
         *pnSector = 32 + ((nBlockNumber - 128) / 16);
      }
   }else if(nCardType == W_PROP_MIFARE_1K)
   {
      *pnSector = nBlockNumber / 4;
   }

   return W_SUCCESS;
}

W_ERROR PMifareClassicGetConnectionInfo(
         tContext * pContext,
         W_HANDLE hConnection,
         tWMifareClassicConnectionInfo* pConnectionInfo)
{
   W_ERROR nError = W_SUCCESS;
   tMifareConnection * pMifareConnection;

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if(nError != W_SUCCESS)
   {
      PDebugError("error during the retrieve of the Mifare Connection instance");
      return nError;
   }

   if( ( pMifareConnection->nType != W_PROP_MIFARE_1K )
    && ( pMifareConnection->nType != W_PROP_MIFARE_4K ) )
   {
      PDebugError("Invalid object connection. This API must be only used for Mifare Classic 1K/4K");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   pConnectionInfo->nSectorNumber = static_PMifareClassicGetNumberOfSector(pMifareConnection->nType);

   return W_SUCCESS;
}

W_ERROR PMifareClassicGetSectorInfo(
                  tContext * pContext,
                  W_HANDLE hConnection,
                  uint16_t nSectorNumber,
                  tWMifareClassicSectorInfo* pSectorInfo )
{
   W_ERROR nError = W_SUCCESS;
   tMifareConnection * pMifareConnection;
   uint8_t nMaxNumberOfSector;

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if(nError != W_SUCCESS)
   {
      PDebugError("error during the retrieve of the Mifare Connection instance");
      return nError;
   }

   if( ( pMifareConnection->nType != W_PROP_MIFARE_1K )
    && ( pMifareConnection->nType != W_PROP_MIFARE_4K ) )
   {
      PDebugError("Invalid object connection. This API must be only used for Mifare Classic 1K/4K");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   nMaxNumberOfSector = static_PMifareClassicGetNumberOfSector(pMifareConnection->nType);
   if(nSectorNumber > nMaxNumberOfSector)
   {
      PDebugError("Invalid Parameter, the desired nSectorNumber must be lower than the number of sector returned by the function PMifareClassicGetConnectionInfo");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Exceptional case */
   if( (pMifareConnection->nType == W_PROP_MIFARE_4K)
     &&(nSectorNumber >= 32))
   {
      pSectorInfo->nBlockNumber  = P_MIFARE_CLASSIC_4K_BLOCK_NUMBER_PER_EXTENDED_SECTOR;
      pSectorInfo->nBlockSize    = P_MIFARE_CLASSIC_BLOCK_SIZE;
      pSectorInfo->nTrailer      = P_MIFARE_CLASSIC_4K_BLOCK_EXTENDED_SECTOR_TRAILER;
   }else
   {
      pSectorInfo->nBlockNumber  = P_MIFARE_CLASSIC_BLOCK_NUMBER_PER_SECTOR;
      pSectorInfo->nBlockSize    = P_MIFARE_CLASSIC_BLOCK_SIZE;
      pSectorInfo->nTrailer      = P_MIFARE_CLASSIC_BLOCK_SECTOR_TRAILER;
   }

   return W_SUCCESS;
}

static void static_PMifareClassicExchangeCallback(
                  tContext * pContext,
                  void * pCallbackParameter,
                  uint32_t nLength,
                  W_ERROR nError)
{
   tMifareConnection * pMifareConnection = (tMifareConnection *) pCallbackParameter;

   switch(pMifareConnection->nMifareClassicOperation)
   {
      case P_MIFARE_CLASSIC_CMD_AUTH_A:
      case P_MIFARE_CLASSIC_CMD_AUTH_B:
         if(nError != W_SUCCESS)
         {
            pMifareConnection->bMifareClassicAuthenticated = W_FALSE;
         }
         else
         {
            pMifareConnection->bMifareClassicAuthenticated = W_TRUE;
         }
         PDFCPostContext2(&pMifareConnection->sCallbackContext, nError);
         break;
      case P_MIFARE_CLASSIC_CMD_READ_BLOCK:
         PDFCPostContext2(&pMifareConnection->sCallbackContext, nError);
         break;
      case P_MIFARE_CLASSIC_CMD_WRITE_BLOCK:
         PDFCPostContext2(&pMifareConnection->sCallbackContext, nError);
         break;
   }

   pMifareConnection->nMifareClassicOperation = P_MIFARE_CLASSIC_HIDLE;
   PHandleDecrementReferenceCount(pContext, pMifareConnection);
}


void PMifareClassicAuthenticate(
                  tContext * pContext,
                  W_HANDLE hConnection,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint16_t nSectorNumber,
                  bool_t   bWithKeyA,
                  const uint8_t* pKey,
                  uint8_t  nKeyLength)
{
   W_ERROR nError = W_SUCCESS;
   tMifareConnection * pMifareConnection;
   uint8_t aCmdBuffer[P_MIFARE_CLASSIC_CMD_AUTH_LENGTH];
   uint8_t nOffset = 0;

   uint8_t nAddr = 0;

   tDFCCallbackContext sCallbackContext;

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if(nError != W_SUCCESS)
   {
      PDebugError("error during the retrieve of the Mifare Connection instance");
      goto return_error;
   }

   if( ( pMifareConnection->nType != W_PROP_MIFARE_1K )
    && ( pMifareConnection->nType != W_PROP_MIFARE_4K ) )
   {
      PDebugError("Invalid object connection. This API must be only used for Mifare Classic 1K/4K");
      goto return_error;
   }

   if(pMifareConnection->nMifareClassicOperation != P_MIFARE_CLASSIC_HIDLE)
   {
      PDebugError("Operation is Pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   nError = static_PMifareClassicGetBlockAddressFromSector(pMifareConnection->nType,
                                                           (uint8_t) nSectorNumber,
                                                           &nAddr);

   if(nError != W_SUCCESS)
   {
      PDebugError("error BAD Parameter, the sector number is too big");
      goto return_error;
   }

   if( ( pKey == null )
      ||  ( nKeyLength != P_MIFARE_CLASSIC_CMD_AUTH_KEY_LENGTH))
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Change state to avoid the refill of memory */
   pMifareConnection->nMifareClassicOperation = P_MIFARE_CLASSIC_CMD_AUTH_A;
   pMifareConnection->nMifareClassicAuthenticatedSectorNumber = (uint8_t) nSectorNumber;

   aCmdBuffer[nOffset++] = (bWithKeyA == W_TRUE) ? P_MIFARE_CLASSIC_CMD_AUTH_A
                                                 : P_MIFARE_CLASSIC_CMD_AUTH_B;
   aCmdBuffer[nOffset++] = nAddr;

   CMemoryCopy(&aCmdBuffer[nOffset],
               pKey,
               nKeyLength);
   nOffset += nKeyLength;


   pMifareConnection->sCallbackContext = sCallbackContext;

   P14Part3ExchangeRawMifare( pContext,
                                 pMifareConnection->hConnection,
                                 static_PMifareClassicExchangeCallback,
                                 pMifareConnection,
                                 aCmdBuffer,
                                 nOffset,
                                 null,
                                 0,
                                 null);

   PHandleIncrementReferenceCount( pMifareConnection);
   return;

return_error:
   PDFCPostContext2(&sCallbackContext, nError);
}


void PMifareClassicReadBlock(
                  tContext * pContext,
                  W_HANDLE hConnection,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint16_t nBlockNumber,
                  uint8_t * pBuffer,
                  uint8_t  nBufferLength)
{
   W_ERROR nError = W_SUCCESS;
   tMifareConnection * pMifareConnection;
   uint8_t aCmdBuffer[P_MIFARE_CLASSIC_CMD_READ_BLOCK_LENGTH];
   uint8_t nOffset = 0;
   uint8_t nAuthenticatedSector = 0;

   tDFCCallbackContext sCallbackContext;

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if(nError != W_SUCCESS)
   {
      PDebugError("error during the retrieve of the Mifare Connection instance");
      goto return_error;
   }

   if( ( pMifareConnection->nType != W_PROP_MIFARE_1K )
    && ( pMifareConnection->nType != W_PROP_MIFARE_4K ) )
   {
      PDebugError("Invalid object connection. This API must be only used for Mifare Classic 1K/4K");
      goto return_error;
   }

   if(pMifareConnection->nMifareClassicOperation != P_MIFARE_CLASSIC_HIDLE)
   {
      PDebugError("Operation is Pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   (void) static_PMifareClassicGetSectorNumberFromBlockNumber(
                                       pMifareConnection->nType,
                                       (uint8_t)nBlockNumber,
                                       &nAuthenticatedSector);

   if(pMifareConnection->bMifareClassicAuthenticated == W_FALSE
      || nAuthenticatedSector  != pMifareConnection->nMifareClassicAuthenticatedSectorNumber)
   {
      PDebugError("Must be authenticated");
      nError = W_ERROR_SECURITY;
      goto return_error;
   }

   if( ( pBuffer == null )
      ||  ( nBufferLength == 0))
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if( nBufferLength < P_MIFARE_CLASSIC_BLOCK_SIZE)
   {
      nError = W_ERROR_BUFFER_TOO_SHORT;
      goto return_error;
   }

   /* Change state to avoid the refill of memory */
   pMifareConnection->nMifareClassicOperation = P_MIFARE_CLASSIC_CMD_READ_BLOCK;
   aCmdBuffer[nOffset++] = P_MIFARE_CLASSIC_CMD_READ_BLOCK;
   aCmdBuffer[nOffset++] = (uint8_t) nBlockNumber;

   pMifareConnection->sCallbackContext = sCallbackContext;

   P14Part3ExchangeRawMifare( pContext,
                                 pMifareConnection->hConnection,
                                 static_PMifareClassicExchangeCallback,
                                 pMifareConnection,
                                 aCmdBuffer,
                                 nOffset,
                                 pBuffer,
                                 nBufferLength,
                                 null);

   PHandleIncrementReferenceCount( pMifareConnection);
   return;
return_error:
   PDFCPostContext2(&sCallbackContext, nError);
}


void PMifareClassicWriteBlock(
                  tContext * pContext,
                  W_HANDLE hConnection,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint16_t nBlockNumber,
                  const uint8_t * pBuffer,
                  uint8_t  nBufferLength)
{
   W_ERROR nError = W_SUCCESS;
   tMifareConnection * pMifareConnection;
   uint8_t aCmdBuffer[P_MIFARE_CLASSIC_CMD_WRITE_BLOCK_LENGTH];
   uint8_t nOffset = 0;
   uint8_t nAuthenticatedSector = 0;

   tDFCCallbackContext sCallbackContext;

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* retrieve the context */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_MIFARE_CONNECTION, (void**)&pMifareConnection);

   if(nError != W_SUCCESS)
   {
      PDebugError("error during the retrieve of the Mifare Connection instance");
      goto return_error;
   }

   if( ( pMifareConnection->nType != W_PROP_MIFARE_1K )
    && ( pMifareConnection->nType != W_PROP_MIFARE_4K ) )
   {
      PDebugError("Invalid object connection. This API must be only used for Mifare Classic 1K/4K");
      goto return_error;
   }

   if(pMifareConnection->nMifareClassicOperation != P_MIFARE_CLASSIC_HIDLE)
   {
      PDebugError("Operation is Pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   (void) static_PMifareClassicGetSectorNumberFromBlockNumber(
                                       pMifareConnection->nType,
                                       (uint8_t) nBlockNumber,
                                       &nAuthenticatedSector);

   if(pMifareConnection->bMifareClassicAuthenticated == W_FALSE
      || nAuthenticatedSector  != pMifareConnection->nMifareClassicAuthenticatedSectorNumber)
   {
      PDebugError("Must be authenticated");
      nError = W_ERROR_SECURITY;
      goto return_error;
   }

   if( ( pBuffer == null )
      ||  ( nBufferLength != P_MIFARE_CLASSIC_BLOCK_SIZE))
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Change state to avoid the refill of memory */
   pMifareConnection->nMifareClassicOperation = P_MIFARE_CLASSIC_CMD_WRITE_BLOCK;
   aCmdBuffer[nOffset++] = P_MIFARE_CLASSIC_CMD_WRITE_BLOCK;
   aCmdBuffer[nOffset++] = (uint8_t) nBlockNumber;

   CMemoryCopy(&aCmdBuffer[nOffset],
               pBuffer,
               nBufferLength);

   nOffset += nBufferLength;

   pMifareConnection->sCallbackContext = sCallbackContext;

   P14Part3ExchangeRawMifare( pContext,
                                 pMifareConnection->hConnection,
                                 static_PMifareClassicExchangeCallback,
                                 pMifareConnection,
                                 aCmdBuffer,
                                 nOffset,
                                 null,
                                 0,
                                 null);

   PHandleIncrementReferenceCount( pMifareConnection);
   return;
return_error:
   PDFCPostContext2(&sCallbackContext, nError);
}

W_ERROR WMifareClassicAuthenticateSync(
                  W_HANDLE hConnection,
                  uint16_t nSectorNumber,
                  bool_t   bWithKeyA,
                  const uint8_t*  pKey,
                  uint8_t  nKeyLength)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareClassicAuthenticateSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareClassicAuthenticate(hConnection,
                                 PBasicGenericSyncCompletion,
                                 &param,
                                 nSectorNumber,
                                 bWithKeyA,
                                 pKey,
                                 nKeyLength);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

W_ERROR WMifareClassicReadBlockSync(
                  W_HANDLE hConnection,
                  uint16_t nBlockNumber,
                  uint8_t * pBuffer,
                  uint8_t  nBufferLength)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareClassicReadBlockSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareClassicReadBlock(hConnection,
                              PBasicGenericSyncCompletion,
                              &param,
                              nBlockNumber,
                              pBuffer,
                              nBufferLength);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

W_ERROR WMifareClassicWriteBlockSync(
                  W_HANDLE hConnection,
                  uint16_t nBlockNumber,
                  uint8_t * pBuffer,
                  uint8_t  nBufferLength)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WMifareClassicWriteBlockSync");

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WMifareClassicWriteBlock(hConnection,
                              PBasicGenericSyncCompletion,
                              &param,
                              nBlockNumber,
                              pBuffer,
                              nBufferLength);
   }
   return PBasicGenericSyncWaitForResult(&param);
}

/* ---------------------- End of API Mifare Classic ------------------------*/
#else /* P_INCLUDE_MIFARE_CLASSIC */


W_ERROR PMifareClassicGetConnectionInfo  (
   tContext * pContext,
   W_HANDLE  hConnection,
   tWMifareClassicConnectionInfo *  pConnectionInfo
 )
{
   if (pConnectionInfo != null)
   {
      CMemoryFill(pConnectionInfo, 0, sizeof(* pConnectionInfo));
   }

   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}


W_ERROR PMifareClassicGetSectorInfo  (
   tContext * pContext,
   W_HANDLE  hConnection,
   uint16_t  nSectorNumber,
   tWMifareClassicSectorInfo *  pSectorInfo
 )
{
   if (pSectorInfo != null)
   {
      CMemoryFill(pSectorInfo, 0, sizeof(* pSectorInfo));
   }

   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

void PMifareClassicAuthenticate(
   tContext * pContext,
   W_HANDLE hConnection,
   tPBasicGenericCallbackFunction* pCallback,
   void* pCallbackParameter,
   uint16_t nSectorNumber,
   bool_t   bWithKeyA,
   const uint8_t* pKey,
   uint8_t  nKeyLength)
{
   tDFCCallbackContext sCallbackContext;

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);

   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

W_ERROR WMifareClassicAuthenticateSync(
   W_HANDLE hConnection,
   uint16_t nSectorNumber,
   bool_t   bWithKeyA,
   const uint8_t*  pKey,
   uint8_t  nKeyLength)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}


void PMifareClassicReadBlock(
   tContext * pContext,
   W_HANDLE hConnection,
   tPBasicGenericCallbackFunction* pCallback,
   void* pCallbackParameter,
   uint16_t nBlockNumber,
   uint8_t * pBuffer,
   uint8_t  nBufferLength)
{
   tDFCCallbackContext sCallbackContext;

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);

   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}


W_ERROR WMifareClassicReadBlockSync(
   W_HANDLE hConnection,
   uint16_t nBlockNumber,
   uint8_t * pBuffer,
   uint8_t  nBufferLength)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}


void PMifareClassicWriteBlock(
   tContext * pContext,
   W_HANDLE hConnection,
   tPBasicGenericCallbackFunction* pCallback,
   void* pCallbackParameter,
   uint16_t nBlockNumber,
   const uint8_t * pBuffer,
   uint8_t  nBufferLength)
{
   tDFCCallbackContext sCallbackContext;

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);

   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

W_ERROR WMifareClassicWriteBlockSync(
   W_HANDLE hConnection,
   uint16_t nBlockNumber,
   uint8_t * pBuffer,
   uint8_t  nBufferLength)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

#endif /* P_INCLUDE_MIFARE_CLASSIC */

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
