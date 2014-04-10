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
   Contains the NDEF Type 2 API implementation.
*******************************************************************************/
#define P_MODULE P_MODULE_DEC( NDEFA2 )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* NFC Type 2 memory mapping */
#define P_NDEF_2_BLOCK_SIZE                  4

#define P_NDEF_2_LOCK_BLOCK_OFFSET           2
#define P_NDEF_2_CC_BLOCK_OFFSET             3
#define P_NDEF_2_DATA_BLOCK_OFFSET           4

#define P_NDEF_2_LOCK_OFFSET                 (P_NDEF_2_LOCK_BLOCK_OFFSET * P_NDEF_2_BLOCK_SIZE + 2)
#define P_NDEF_2_LOCK_LENGTH                 2

#define P_NDEF_2_MIFARE_ULC_LOCK_OFFSET      (0x28 * P_NDEF_2_BLOCK_SIZE)
#define P_NDEF_2_MY_D_MOVE_LOCK_OFFSET       (0x24 * P_NDEF_2_BLOCK_SIZE)

#define P_NDEF_2_CC_OFFSET                   (P_NDEF_2_CC_BLOCK_OFFSET * P_NDEF_2_BLOCK_SIZE)
#define P_NDEF_2_CC_LENGTH                   P_NDEF_2_BLOCK_SIZE

#define P_NDEF_2_CC_TAG_BYTE_OFFSET          0
#define P_NDEF_2_CC_VERSION_BYTE_OFFSET      1
#define P_NDEF_2_CC_SIZE_BYTE_OFFSET         2
#define P_NDEF_2_CC_ACCESS_BYTE_OFFSET       3

#define P_NDEF_2_TLV_OFFSET                  (P_NDEF_2_DATA_BLOCK_OFFSET * P_NDEF_2_BLOCK_SIZE)

/* NFC Type 2 Tag defines */
#define P_NDEF_2_MAGIC_NUMBER                0xE1
#define P_NDEF_2_MAPPING_VERSION             0x10       /* 1.0 */

#define P_NDEF_2_TLV_NULL                    0x00
#define P_NDEF_2_TLV_LOCK_CONTROL            0x01
#define P_NDEF_2_TLV_LOCK_CONTROL_LENGTH        3
#define P_NDEF_2_TLV_MEMORY_CONTROL          0x02
#define P_NDEF_2_TLV_MEMORY_CONTROL_LENGTH      3
#define P_NDEF_2_TLV_NDEF_MESSAGE            0x03
#define P_NDEF_2_TLV_PROPRIETARY             0xFD
#define P_NDEF_2_TLV_TERMINATOR              0xFE

/* lock tag states */
#define P_NDEF_2_WRITE_START                 0x00
#define P_NDEF_2_WRITE_CC                    0x03
#define P_NDEF_2_LOCK_TAG                    0x04
#define P_NDEF_2_LOCK2_TAG                   0x05

   /* move NDEF states */
#define P_NDEF_2_MOVE_START                  0x00
#define P_NDEF_2_MOVE_READ                   0x01
#define P_NDEF_2_MOVE_WRITE                  0x02


/* ------------------------------------------------------- */
/*                CONNECTION CREATION                      */
/* ------------------------------------------------------- */

static void static_PNDEFType2ReadStaticAreaCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError);
static W_ERROR static_PNDEFType2ParseStaticLockBytes(tContext* pContext, tNDEFConnection* pNDEFConnection);
static W_ERROR static_PNDEFType2ParseCapabilityContainer(tContext* pContext, tNDEFConnection* pNDEFConnection );
static W_ERROR static_PNDEFType2ParseTLV(tContext* pContext, tNDEFConnection* pNDEFConnection,
                                         uint8_t* pReceivedBuffer, uint32_t nReceivedOffset, uint32_t nReceivedLength);


/**
 * @brief   Creates a NDEF TAG Type 2 connection
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The PNDEF connection
 *
 * @return W_SUCCESS if the current connection is NDEF type 2 capable
 *
 * The NDEF connection is completed when the PNDEFSendError() is called
 **/
static W_ERROR static_PNDEFType2CreateConnection(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   W_ERROR nError;

   PDebugTrace("static_PNDEFType2CreateConnection");

   /* Check if the connection handle is valid */
   if ((nError = PNDEF2GenCheckType2(pContext,
                                     pNDEFConnection->hConnection)) != W_SUCCESS)
   {
      PDebugError("static_PNDEFType2CreateConnection: not a NDEF Type 2 Generic");
      return nError;
   }

   /* check that pNDEFConnection->pReceivedBuffer can store the read static aera */
   if (PNDEFUpdateBufferSize(pNDEFConnection,
                             P_NDEF2GEN_STATIC_BYTE_LENGTH) != W_SUCCESS)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Read the static area */
   PNDEF2GenRead( pContext,
                  pNDEFConnection->hConnection,
                  static_PNDEFType2ReadStaticAreaCompleted,
                  pNDEFConnection,
                  pNDEFConnection->pReceivedBuffer,
                  0,
                  P_NDEF2GEN_STATIC_BYTE_LENGTH);

   return W_SUCCESS;
}


static void static_PMifareULRetrieveAccessRightsCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;

   /* Send the result */
   if (nError != W_SUCCESS)
   {
      PDFCPostContext2(&pNDEFConnection->sCallbackContext, W_ERROR_CONNECTION_COMPATIBILITY);
   }
   else
   {
      PDFCPostContext2(&pNDEFConnection->sCallbackContext, W_SUCCESS);
   }
}


/**
 * @brief Capability Container Read callback
 *
 */
static void static_PNDEFType2ReadStaticAreaCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEFType2ReadStaticAreaCompleted");

   if (nError != W_SUCCESS)
   {
      goto return_error;
   }

   /* Parse the static lock bytes */
   if ( (nError = static_PNDEFType2ParseStaticLockBytes(pContext, pNDEFConnection)) != W_SUCCESS)
   {
      goto return_error;
   }

   /* Copy CC bytes */
   CMemoryCopy(pNDEFConnection->aCCFile,
               pNDEFConnection->pReceivedBuffer + P_NDEF_2_CC_OFFSET,
               P_NDEF_2_CC_LENGTH);
   pNDEFConnection->nCCLength = P_NDEF_2_CC_LENGTH;

   /* Parse the CC contents */
   if ( (nError = static_PNDEFType2ParseCapabilityContainer(pContext, pNDEFConnection)) != W_SUCCESS)
   {
      goto return_error;
   }

   /* Parse TLVs */
   if ((nError = static_PNDEFType2ParseTLV(
                              pContext,
                              pNDEFConnection,
                              pNDEFConnection->pReceivedBuffer,
                              P_NDEF_2_TLV_OFFSET,
                              P_NDEF2GEN_STATIC_BYTE_LENGTH)) != W_SUCCESS )
   {
      goto return_error;
   }

   /* Check if a NDEF Message has been found */
   if ( pNDEFConnection->nNDEFId != 0xFFFF )
   {
      /* Create dynamic smart cache */
      if ( (pNDEFConnection->sType.t2.nTagSize > P_NDEF2GEN_STATIC_BYTE_LENGTH) &&
           (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MIFARE_UL) != W_SUCCESS) &&
           (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MY_D_MOVE) != W_SUCCESS) &&
           (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MY_D_NFC)  != W_SUCCESS)  &&
           (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_KOVIO_RFID)!= W_SUCCESS))
      {
         uint32_t nSectorNumber = (pNDEFConnection->sType.t2.nTagSize + P_NDEF2GEN_BLOCK_SIZE - 1) / P_NDEF2GEN_BLOCK_SIZE;

         if ( (nError = PNDEF2GenCreateSmartCacheDynamic(pContext, pNDEFConnection->hConnection, nSectorNumber)) != W_SUCCESS)
         {
            PDebugError("static_PNDEFType2ReadStaticAreaCompleted : PNDEF2GenCreateSmartCacheDynamic returned %s", PUtilTraceError(nError));
            goto return_error;
         }
      }

      /* Special case for OTP Card (Only Kovio) */
      if (  ( PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_KOVIO_RFID) == W_SUCCESS) &&
            ( pNDEFConnection->nNDEFFileLength > 0) )
      {
         /* Force the tag to read only */
         pNDEFConnection->bIsLocked = W_TRUE;
         pNDEFConnection->bIsLockable = W_FALSE;
      }

      if (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MIFARE_UL_C) == W_SUCCESS)
      {
         /* A Mifare UL C card has been found, retreive the access rights */
         PMifareULInitializeAccessRightsAccordingToType2TagCC(pContext, pNDEFConnection->hConnection, pNDEFConnection->bIsLocked,
                  static_PMifareULRetrieveAccessRightsCompleted, pNDEFConnection);
      }
      else
      {
         /* NDEF tag is initialized */
         PDFCPostContext2(&pNDEFConnection->sCallbackContext, W_SUCCESS);
      }

      return;
   }

return_error:

   if( (nError == W_ERROR_RF_COMMUNICATION)          ||
       (nError == W_ERROR_RF_PROTOCOL_NOT_SUPPORTED) ||
       (nError == W_ERROR_TIMEOUT))
   {
      nError = W_ERROR_RF_COMMUNICATION;
   }else
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
   }

   PDFCPostContext2(&pNDEFConnection->sCallbackContext, nError);
}

/**
 * @brief   Parses the static lock bytes
 */
static W_ERROR static_PNDEFType2ParseStaticLockBytes(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   PDebugTrace("static_PNDEFType2ParseStaticLockBytes");


   if (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MIFARE_UL) == W_SUCCESS)
   {
      PMifareCheckType2(pContext, pNDEFConnection->hConnection, null, null, &pNDEFConnection->bIsLocked, &pNDEFConnection->bIsLockable, null);
   }
   else if ( (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MY_D_MOVE) == W_SUCCESS) ||
             (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MY_D_NFC) == W_SUCCESS))
   {
      PMyDCheckType2(pContext, pNDEFConnection->hConnection, null, null, &pNDEFConnection->bIsLocked, &pNDEFConnection->bIsLockable, null);
   }
   else
   {
      pNDEFConnection->bIsLocked = ( (pNDEFConnection->pReceivedBuffer[P_NDEF_2_LOCK_OFFSET + 0] != 0x00) ||
                                     (pNDEFConnection->pReceivedBuffer[P_NDEF_2_LOCK_OFFSET + 1] != 0x00) );

      pNDEFConnection->bIsLockable = !pNDEFConnection->bIsLocked;
   }

   if (pNDEFConnection->bIsLocked != W_FALSE)
   {
      pNDEFConnection->bIsLockable = W_FALSE;
   }

   return W_SUCCESS;
}

/**
 * @brief   Parses the content of the capability container
 */
static W_ERROR static_PNDEFType2ParseCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   uint8_t nReadWriteAccess;
   bool_t bLocked;

   PDebugTrace("static_PNDEFType2ParseCapabilityContainer");

   /* Check the NDEF identification byte */
   if ( pNDEFConnection->aCCFile[P_NDEF_2_CC_TAG_BYTE_OFFSET] != P_NDEF_2_MAGIC_NUMBER )
   {
      PDebugLog("static_PNDEFType2ParseCapabilityContainer: wrong identification byte");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Mapping version */
   PDebugTrace("static_PNDEFType2ParseCapabilityContainer: version %d.%d",
               (pNDEFConnection->aCCFile[P_NDEF_2_CC_VERSION_BYTE_OFFSET] >> 4),
               (pNDEFConnection->aCCFile[P_NDEF_2_CC_VERSION_BYTE_OFFSET] & 0x0F) );

   /* Check the mapping version */
   if ( ( P_NDEF_2_MAPPING_VERSION & 0xF0 ) < ( pNDEFConnection->aCCFile[P_NDEF_2_CC_VERSION_BYTE_OFFSET] & 0xF0 ) )
   {
      PDebugError("static_PNDEFType2ParseCapabilityContainer: higher version");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Retrieve the maximum message length */
   pNDEFConnection->nMaximumSpaceSize = pNDEFConnection->aCCFile[P_NDEF_2_CC_SIZE_BYTE_OFFSET] * 8;
   pNDEFConnection->nFreeSpaceSize = pNDEFConnection->nMaximumSpaceSize;
   PDebugTrace("static_PNDEFType2ParseCapabilityContainer: nMaximumSpaceSize %d bytes", pNDEFConnection->nMaximumSpaceSize);

   /* check that pNDEFConnection->pReceivedBuffer can store the nMaximumSpaceSize */
   if (PNDEFUpdateBufferSize(pNDEFConnection,
                             pNDEFConnection->nMaximumSpaceSize) != W_SUCCESS)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Init the size of the tag */
   pNDEFConnection->sType.t2.nTagSize = pNDEFConnection->nMaximumSpaceSize + P_NDEF_2_TLV_OFFSET;

   /* Check the tag's size */
   if (pNDEFConnection->sType.t2.nTagSize < P_NDEF2GEN_STATIC_BYTE_LENGTH)
   {
      PDebugError("static_PNDEFType2ParseCapabilityContainer: length is too small (%d bytes)", pNDEFConnection->sType.t2.nTagSize);
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   else if ( (pNDEFConnection->sType.t2.nTagSize > P_NDEF2GEN_STATIC_BYTE_LENGTH) &&
             (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MIFARE_UL) == W_SUCCESS) )
   {
      /* This tag is a Mifare UL-C, upgrade the connection */
      PMifareULForceULC(pContext, pNDEFConnection->hConnection);
   }

   /* Check the read access value */
   nReadWriteAccess = (pNDEFConnection->aCCFile[P_NDEF_2_CC_ACCESS_BYTE_OFFSET] >> 4) & 0x0F;
   if ( ( (nReadWriteAccess > 0x00) && (nReadWriteAccess <= 0x07) )
       || (nReadWriteAccess == 0x0F) )
   {
      PDebugWarning("static_PNDEFType2ParseCapabilityContainer: incorrect read access 0x%02X", nReadWriteAccess);
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   /* Check the write access value */
   nReadWriteAccess = pNDEFConnection->aCCFile[P_NDEF_2_CC_ACCESS_BYTE_OFFSET] & 0x0F;
   if ( (nReadWriteAccess > 0x00) && (nReadWriteAccess <= 0x07) )
   {
      PDebugWarning("static_PNDEFType2ParseCapabilityContainer: incorrect write access 0x%02X", nReadWriteAccess);
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   bLocked = (0xF == nReadWriteAccess);
   PDebugTrace("static_PNDEFType2ParseCapabilityContainer: %s", bLocked ? "locked" : "unlocked");

   if (pNDEFConnection->bIsLocked != bLocked)
   {
      PDebugWarning("static_PNDEFType2ParseCapabilityContainer: inconsistent write access. It is considered not granted!");

      /* Inconsistency between physical lock and NDEF logical lock => consider the Tag as Read-Only, but allow Reading of the Tag */
      pNDEFConnection->bIsLocked = W_TRUE;
   }

   return W_SUCCESS;
}

/**
 * @brief   Parses the Type 2 Tag Message TLV blocks.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The NDEF connection structure.
 *
 * @param[in]  nReceivedLength  The length of the received buffer.
 **/
static W_ERROR static_PNDEFType2ParseTLV(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t* pReceivedBuffer,
            uint32_t nReceivedOffset,
            uint32_t nReceivedLength )
{
   uint32_t nIndex = nReceivedOffset;
   uint8_t  nLengthFieldSize;
   uint32_t nLength;
   uint32_t nPagesAddr;
   uint32_t nByteOffset;
   uint32_t nBytesPerPage;

   bool_t     bIsNDEFTLVfound  = W_FALSE;
   bool_t     bIsProprietaryTLVfound = W_FALSE;
   bool_t     bIsLockControlTLVfound = W_FALSE;
   bool_t     bIsMemoryControlTLVfound = W_FALSE;

   PDebugTrace("static_PNDEFType2ParseTLV");

   /* default Dynamic lock bits position / length :
      located after the data are (see NDEF Type 2 spec) */
   pNDEFConnection->sType.t2.nDynamicLockAddress = P_NDEF_2_TLV_OFFSET +  pNDEFConnection->nMaximumSpaceSize;
   pNDEFConnection->sType.t2.nLockedBytesPerBit = 8;
   if (pNDEFConnection->nMaximumSpaceSize > 48)
   {
      if (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MIFARE_UL_C) == W_SUCCESS)
      {
         /* For Mifare UL-C, there are 1 lock byte (n3) for the dynamic area. The lock byte 4 is for Auth, CNT and Key. */
         pNDEFConnection->sType.t2.nDynamicLockAddress = P_NDEF_2_MIFARE_ULC_LOCK_OFFSET;
         pNDEFConnection->sType.t2.nLockedBytesPerBit = 16;
         pNDEFConnection->sType.t2.nDynamicLockBits = 8;
      }
      else if (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MY_D_MOVE) == W_SUCCESS)
      {
         /* For My-d move, there are 20 lock bits for the dynamic area */
         pNDEFConnection->sType.t2.nDynamicLockAddress = P_NDEF_2_MY_D_MOVE_LOCK_OFFSET;
         pNDEFConnection->sType.t2.nLockedBytesPerBit = 4;
         pNDEFConnection->sType.t2.nDynamicLockBits = 20;
      }
      else
      {
         /* Default dynamic lock bits */
         pNDEFConnection->sType.t2.nDynamicLockBits = (uint16_t) (
            (pNDEFConnection->nMaximumSpaceSize - 48 + (pNDEFConnection->sType.t2.nLockedBytesPerBit - 1)) / pNDEFConnection->sType.t2.nLockedBytesPerBit );
      }
   }
   else
   {
      /* No dynamic lock bytes */
      pNDEFConnection->sType.t2.nDynamicLockBits = 0;
   }
   pNDEFConnection->sType.t2.nDynamicLockSize = (uint16_t) ( (pNDEFConnection->sType.t2.nDynamicLockBits + 7) / 8 );

   /* No default dynamic reserved memory area :
      if not specified, consider if is located after the dynamic lock bit area
      we don't mind about the length (it's located after the data area, so we won't access it */
   pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress = pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicLockSize;
   pNDEFConnection->sType.t2.nDynamicReservedMemorySize = 0;

   /* Walk through the TLV block(s) */
   while ( nIndex < nReceivedLength )
   {
      /* Check the TLV type */
      switch ( pReceivedBuffer[nIndex] )
      {
         /* null */
         case P_NDEF_2_TLV_NULL:
            PDebugTrace("static_PNDEFType1ParseTLV: null");

            if (bIsNDEFTLVfound == W_FALSE)
            {
               pNDEFConnection->nMaximumSpaceSize--;
            }
            nIndex ++;
            break;

         /* Lock Control */
         case P_NDEF_2_TLV_LOCK_CONTROL:

            PDebugTrace("static_PNDEFType2ParseTLV: Lock Control");

            if ((bIsNDEFTLVfound != W_FALSE) || (bIsProprietaryTLVfound != W_FALSE))
            {
               PDebugError("static_PNDEFType2ParseTLV : lock control after NDEF or PROPRIETARRY TLV !");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            if ((nIndex + P_NDEF_2_TLV_LOCK_CONTROL_LENGTH) >= nReceivedLength)
            {
               /* we've reached the end of the memory */
               PDebugError("static_PNDEFType2ParseTLV: end of tag reached");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            if (pReceivedBuffer[nIndex + 1] != P_NDEF_2_TLV_LOCK_CONTROL_LENGTH)
            {
               PDebugError("static_PNDEFType2ParseTLV: wrong TLV length");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            if (bIsLockControlTLVfound == W_FALSE)
            {
               bIsLockControlTLVfound = W_TRUE;

               /* Position (MSB) */
               nPagesAddr =  (pReceivedBuffer[nIndex + 2] & 0xF0) >> 4;
               nByteOffset = (pReceivedBuffer[nIndex + 2] & 0x0F);

               /* Size (Middle) */
               pNDEFConnection->sType.t2.nDynamicLockBits = pReceivedBuffer[nIndex + 3];
               if (pNDEFConnection->sType.t2.nDynamicLockBits == 0)
               {
                  pNDEFConnection->sType.t2.nDynamicLockBits = 256;
               }
               pNDEFConnection->sType.t2.nDynamicLockSize = (uint16_t) ( (pNDEFConnection->sType.t2.nDynamicLockBits + 7) / 8 );

               /* Page control (LSB) */
               nBytesPerPage = pReceivedBuffer[nIndex + 4] & 0x0F;
               nBytesPerPage = 1 << nBytesPerPage;

               pNDEFConnection->sType.t2.nLockedBytesPerBit = (pReceivedBuffer[nIndex + 4] & 0xF0) >> 4;
               pNDEFConnection->sType.t2.nLockedBytesPerBit = 1 << pNDEFConnection->sType.t2.nLockedBytesPerBit;

               pNDEFConnection->sType.t2.nDynamicLockAddress = (nPagesAddr * nBytesPerPage) + nByteOffset;

               /* Add dynamic lock size which is not in data area size */
               pNDEFConnection->sType.t2.nTagSize += pNDEFConnection->sType.t2.nDynamicLockSize;
            }
            else
            {
               PDebugError("static_PNDEFType2ParseTLV : several lock control TLV is not supported");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            nIndex += 2 + P_NDEF_2_TLV_LOCK_CONTROL_LENGTH ;

            if (bIsNDEFTLVfound == W_FALSE)
            {
               pNDEFConnection->nMaximumSpaceSize-= 2 + P_NDEF_2_TLV_LOCK_CONTROL_LENGTH;
            }
            break;

         /* Memory control */
         case P_NDEF_2_TLV_MEMORY_CONTROL:

            PDebugTrace("static_PNDEFType2ParseTLV: Memory Control");

            if ((bIsNDEFTLVfound != W_FALSE) || (bIsProprietaryTLVfound != W_FALSE))
            {
               PDebugError("static_PNDEFType2ParseTLV : memory control after NDEF TLV !");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            if (nIndex + P_NDEF_2_TLV_MEMORY_CONTROL_LENGTH >= nReceivedLength)
            {
               /* we've reached the end of the memory */
               PDebugError("static_PNDEFType2ParseTLV: end of tag reached");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            if (pReceivedBuffer[nIndex + 1] != P_NDEF_2_TLV_MEMORY_CONTROL_LENGTH)
            {
               PDebugError("static_PNDEFType2ParseTLV: wrong TLV length");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            if (bIsMemoryControlTLVfound == W_FALSE)
            {
               bIsMemoryControlTLVfound = W_TRUE;

               /* Position (MSB) */
               nPagesAddr =  (pReceivedBuffer[nIndex + 2] & 0xF0) >> 4;
               nByteOffset = (pReceivedBuffer[nIndex + 2] & 0x0F);

               /* Size (Middle) */
               pNDEFConnection->sType.t2.nDynamicReservedMemorySize = pReceivedBuffer[nIndex + 3];

               if (pNDEFConnection->sType.t2.nDynamicReservedMemorySize == 0)
               {
                  pNDEFConnection->sType.t2.nDynamicReservedMemorySize = 256;
               }

               /* Partial page control (LSB) */

               nBytesPerPage = pReceivedBuffer[nIndex + 4] & 0x0F;
               nBytesPerPage = 1 << nBytesPerPage;

               pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress = (nPagesAddr * nBytesPerPage) + nByteOffset;

               /* Add reserved memory size which is not in data area size */
               pNDEFConnection->sType.t2.nTagSize += pNDEFConnection->sType.t2.nDynamicReservedMemorySize;
            }
            else
            {
               PDebugError("static_PNDEFType2ParseTLV : several reserved memory TLV is not supported");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            nIndex += 2 + P_NDEF_2_TLV_MEMORY_CONTROL_LENGTH;

            if (bIsNDEFTLVfound == W_FALSE)
            {
               pNDEFConnection->nMaximumSpaceSize-= 2 + P_NDEF_2_TLV_MEMORY_CONTROL_LENGTH;
            }
            break;

         /* NDEF Message */
         case P_NDEF_2_TLV_NDEF_MESSAGE:

            PDebugTrace("static_PNDEFType2ParseTLV: NDEF Message");

            if ((nIndex + 1) >= nReceivedLength)
            {
               PDebugError("static_PNDEFType2ParseTLV: end of tag reached");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            /* Check the length */
            if ( pReceivedBuffer[nIndex + 1] < 0xFF )
            {
               nLength = pReceivedBuffer[nIndex + 1];
               nLengthFieldSize = 1;
            }
            else
            {
               if ((nIndex + 3) >= nReceivedLength)
               {
                  PDebugError("static_PNDEFType2ParseTLV: end of tag reached");
                  return W_ERROR_CONNECTION_COMPATIBILITY;
               }

               nLength = (pReceivedBuffer[nIndex + 2] << 8) + (pReceivedBuffer[nIndex + 3]);

               if (nLength < 0xFF)
               {
                  PDebugWarning("static_PNDEFType2ParseTLV: wrong TLV length");
               }

               nLengthFieldSize = 3;
            }

            if (nIndex + 1 + nLengthFieldSize + nLength > pNDEFConnection->sType.t2.nTagSize)
            {
               PDebugError("static_PNDEFType2ParseTLV : end of tag reached");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            if (bIsNDEFTLVfound == W_FALSE)
            {
               bIsNDEFTLVfound = W_TRUE;

               pNDEFConnection->nNDEFId = (uint16_t)(pNDEFConnection->nOffset + nIndex - nReceivedOffset);

               pNDEFConnection->nByteLength = nLengthFieldSize;
               pNDEFConnection->nNDEFFileLength = nLength;

               pNDEFConnection->nMaximumSpaceSize = pNDEFConnection->nMaximumSpaceSize - (1 + pNDEFConnection->nByteLength);

               if ((pNDEFConnection->nByteLength == 1) && (pNDEFConnection->nMaximumSpaceSize >= 255))
               {
                  /* Length can grow on 3 bytes */
                  pNDEFConnection->nMaximumSpaceSize -= 2;
               }

               pNDEFConnection->nFreeSpaceSize  = pNDEFConnection->nMaximumSpaceSize - pNDEFConnection->nNDEFFileLength;
            }

            nIndex += 1 + nLengthFieldSize + nLength;

            break;

         /* Proprietary and other TLVs*/

         default :

            if ((pReceivedBuffer[nIndex]) == P_NDEF_2_TLV_PROPRIETARY)
            {
               PDebugTrace("static_PNDEFType2ParseTLV: Proprietary");
               bIsProprietaryTLVfound = W_TRUE;
            }
            else
            {
               PDebugTrace("static_PNDEFType2ParseTLV: unkown TLV");
            }

            if ((nIndex + 1) >= nReceivedLength)
            {
               PDebugError("static_PNDEFType2ParseTLV: end of tag reached");
               return W_ERROR_CONNECTION_COMPATIBILITY;
            }

            if ( pReceivedBuffer[nIndex + 1] < 0xFF )
            {
               nLength = pReceivedBuffer[nIndex + 1];
               nLengthFieldSize = 1;
            }
            else
            {
               if ((nIndex + 3) >= nReceivedLength)
               {
                  PDebugError("static_PNDEFType2ParseTLV: end of tag reached");
                  return W_ERROR_CONNECTION_COMPATIBILITY;
               }
               nLength = (pReceivedBuffer[nIndex + 2] << 8) + (pReceivedBuffer[nIndex + 3]);
               nLengthFieldSize = 3;

               if (nLength < 0xFF)
               {
                  PDebugWarning("static_PNDEFType2ParseTLV: wrong TLV length");
               }
            }

            nIndex += 1 + nLengthFieldSize + nLength;

            if (bIsNDEFTLVfound == W_FALSE)
            {
               pNDEFConnection->nMaximumSpaceSize-= 1 + nLengthFieldSize + nLength;
            }
            break;

         /* Terminator */
         case P_NDEF_2_TLV_TERMINATOR:

            PDebugTrace("static_PNDEFType2ParseTLV: Terminator");
            goto end;
      }
   }

   PDebugTrace("static_PNDEFType2ParseTLV : end of tag reached");

end:

   if (bIsNDEFTLVfound == W_FALSE)
   {
      PDebugError("static_PNDEFType2ParseTLV : no NDEF TLV found");
      return (W_ERROR_CONNECTION_COMPATIBILITY);
   }

   return W_SUCCESS;
}


/* ------------------------------------------------------- */
/*                COMMAND PROCESSING                       */
/* ------------------------------------------------------- */

static bool_t static_PNDEFType2ReadWriteAreaAutomaton(tContext* pContext, tNDEFConnection* pNDEFConnection);
static void static_PNDEFType2ReadNDEFCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError);
static void static_PNDEFType2WriteNDEFCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError);
static void static_PNDEFType2LockAutomaton(tContext* pContext, void* pCallbackParameter, W_ERROR nError);
static void static_PNDEFType2MoveAutomaton(tContext* pContext, void* pCallbackParameter, W_ERROR nError);

/**
  * @brief Processes the different NDEF commands
  *
  * @param[in] pContext The context
  *
  * @param[in] pNDEFConnection The NDEF connection
  *
  * @param[in] nOffset The offset, for read / write operations
  *
  * @param[in] nLength The length, for read / write operations
  *
  * @return W_SUCCESS
  */
static W_ERROR static_PNDEFType2SendCommand(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nOffset,
            uint32_t nLength )
{
   uint8_t nRequestedByteLength;

   /* Send the corresponding command */
   switch ( pNDEFConnection->nCommandType )
   {
      case P_NDEF_COMMAND_READ_NDEF:

         PDebugTrace("static_PNDEFType2SendCommand : P_NDEF_COMMAND_READ_NDEF");

         /* pNDEFConnection->nNDEFId contains the offset of the NDEV TLV in the TLV area */

         pNDEFConnection->sType.t2.nBegin  = P_NDEF_2_TLV_OFFSET + pNDEFConnection->nNDEFId + 1 + pNDEFConnection->nByteLength + nOffset;
         pNDEFConnection->sType.t2.nBytesToProcess = nLength;
         pNDEFConnection->sType.t2.nBytesProcessed  = 0;
         pNDEFConnection->sType.t2.nBytesPending  = 0;
         pNDEFConnection->sType.t2.pBuffer = pNDEFConnection->pReceivedBuffer;
         pNDEFConnection->sType.t2.nCurrentOperation = P_NDEF_COMMAND_READ_NDEF;

         static_PNDEFType2ReadNDEFCompleted(pContext, pNDEFConnection, W_SUCCESS);
         break;

      case P_NDEF_COMMAND_WRITE_NDEF:

         PDebugTrace("static_PNDEFType2SendCommand: P_NDEF_COMMAND_WRITE_NDEF");

         nRequestedByteLength = (pNDEFConnection->nUpdatedNDEFFileLength <= 254) ? 1 : 3;

         /* specific case when writing a nOffset = 0 (e.g we are replacing the content of the NDEF file)
            we directly set the nByteLength to the correct length since there's no need to keep
            previous tag content */

         if (nOffset == 0)
         {
            pNDEFConnection->nByteLength = nRequestedByteLength;
         }

         if (nRequestedByteLength == pNDEFConnection->nByteLength)
         {
            /* the NDEF file length format  is not changing :o) */

            pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_WRITE_START;

            pNDEFConnection->sType.t2.nBegin = P_NDEF_2_TLV_OFFSET + pNDEFConnection->nNDEFId + 1 + pNDEFConnection->nByteLength + nOffset;
            pNDEFConnection->sType.t2.nBytesToProcess = nLength;
            pNDEFConnection->sType.t2.nBytesProcessed  = 0;
            pNDEFConnection->sType.t2.nBytesPending  = 0;
            pNDEFConnection->sType.t2.pBuffer = pNDEFConnection->pMessageBuffer;
            pNDEFConnection->sType.t2.nCurrentOperation = P_NDEF_COMMAND_WRITE_NDEF;

            static_PNDEFType2WriteNDEFCompleted(pContext, pNDEFConnection, W_SUCCESS);
         }
         else
         {
            /* Need to change the format of the length of the NDEF file */

            PDebugTrace("static_PNDEFType2SendCommand : the NDEF size is changing...");

            /* store the write command for later processing */

            pNDEFConnection->sType.t2.nPendingWriteOffset = nOffset;
            pNDEFConnection->sType.t2.nPendingWriteLength = nLength;

            pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_MOVE_START;

            static_PNDEFType2MoveAutomaton(pContext, pNDEFConnection, W_SUCCESS);
         }
         break;

      case P_NDEF_COMMAND_WRITE_NDEF_LENGTH:

         PDebugTrace("static_PNDEFType2SendCommand: P_NDEF_COMMAND_WRITE_NDEF_LENGTH");

         pNDEFConnection->sType.t2.nBegin  = P_NDEF_2_TLV_OFFSET + pNDEFConnection->nNDEFId + 1;

         /* Update the length of the NDEF file */
         pNDEFConnection->nNDEFFileLength = pNDEFConnection->nUpdatedNDEFFileLength;
         pNDEFConnection->nFreeSpaceSize  = pNDEFConnection->nMaximumSpaceSize - pNDEFConnection->nNDEFFileLength;

         /* we use the same format for the NDEF file length as the current one */
         if (pNDEFConnection->nByteLength == 1)
         {
            CDebugAssert(pNDEFConnection->nNDEFFileLength <= 254);

            pNDEFConnection->pSendBuffer[0] = (uint8_t) pNDEFConnection->nNDEFFileLength;
            pNDEFConnection->sType.t2.nBytesToProcess = 1;
         }
         else
         {
            pNDEFConnection->pSendBuffer[0] = 0xFF;
            pNDEFConnection->pSendBuffer[1] = (uint8_t) (pNDEFConnection->nNDEFFileLength >> 8);
            pNDEFConnection->pSendBuffer[2] = (uint8_t) (pNDEFConnection->nNDEFFileLength);
            pNDEFConnection->sType.t2.nBytesToProcess = 3;
         }

         pNDEFConnection->sType.t2.nBytesProcessed  = 0;
         pNDEFConnection->sType.t2.nBytesPending  = 0;
         pNDEFConnection->sType.t2.pBuffer = pNDEFConnection->pSendBuffer;

         static_PNDEFType2WriteNDEFCompleted(pContext, pNDEFConnection, W_SUCCESS);
         break;

      case P_NDEF_COMMAND_LOCK_TAG:

         PDebugTrace("static_PNDEFType2SendCommand: P_NDEF_COMMAND_LOCK_TAG");

         pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_WRITE_START;
         static_PNDEFType2LockAutomaton(pContext, pNDEFConnection, W_SUCCESS);
         break;

      default:
         PDebugError("static_PNDEFType2SendCommand: command 0x%02X not supported", pNDEFConnection->nCommandType);
         return W_ERROR_BAD_PARAMETER;
   }

   return W_SUCCESS;
}

/**
 * @brief  Read / write automaton, deals with skipping the reserved area during read / write operations
 *
 * @param[in] pContext The context
 *
 * @param[in] pNDEFConnection The NDEF connection
 *
 * @return W_TRUE if the operation is completed, W_FALSE
 */
static bool_t static_PNDEFType2ReadWriteAreaAutomaton(
   tContext* pContext,
   tNDEFConnection* pNDEFConnection)
{
   uint32_t nBegin;
   uint32_t nEnd;

   PDebugTrace("static_PNDEFType2ReadWriteAreaAutomaton");

   pNDEFConnection->sType.t2.nBytesProcessed += pNDEFConnection->sType.t2.nBytesPending;

   if (pNDEFConnection->sType.t2.nBytesProcessed >= pNDEFConnection->sType.t2.nBytesToProcess)
   {
      /* the operation is now complete */
      return W_TRUE;
   }

   nBegin = pNDEFConnection->sType.t2.nBegin + pNDEFConnection->sType.t2.nBytesPending;

   /* if the current read operation start belongs to a reserved memory area, skip it */

   if (( pNDEFConnection->sType.t2.nDynamicLockAddress <= nBegin) && (nBegin < pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicLockSize))
   {
      nBegin = pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicLockSize;
   }

   if ((pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress <= nBegin) && (nBegin < pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicReservedMemorySize))
   {
      nBegin = pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicReservedMemorySize;
   }

   /* if a reserved memory area belongs to the current read range, shorten the read range */

   nEnd = nBegin + pNDEFConnection->sType.t2.nBytesToProcess - pNDEFConnection->sType.t2.nBytesProcessed - 1;

   if ((nBegin <= pNDEFConnection->sType.t2.nDynamicLockAddress) && (pNDEFConnection->sType.t2.nDynamicLockAddress < nEnd))
   {
      nEnd = pNDEFConnection->sType.t2.nDynamicLockAddress - 1;
   }

   if ((nBegin <= pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress) && (pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress < nEnd))
   {
      nEnd = pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress - 1;
   }


   /* store the received data length since the callback do not provide this value */

   pNDEFConnection->sType.t2.nBytesPending = nEnd - nBegin + 1;

   pNDEFConnection->sType.t2.nBegin = nBegin;

   switch (pNDEFConnection->sType.t2.nCurrentOperation)
   {
      case P_NDEF_COMMAND_READ_NDEF :

         PDebugTrace("static_PNDEFType2ReadWriteAreaAutomaton : read[%d-%d]", nBegin, nEnd);

         PNDEF2GenRead(pContext,
                       pNDEFConnection->hConnection, static_PNDEFType2ReadNDEFCompleted,
                       pNDEFConnection,
                       pNDEFConnection->sType.t2.pBuffer + pNDEFConnection->sType.t2.nBytesProcessed,
                       nBegin,
                       pNDEFConnection->sType.t2.nBytesPending);

         break;

      case P_NDEF_COMMAND_WRITE_NDEF :

         PDebugTrace("static_PNDEFType2ReadWriteAreaAutomaton : write[%d-%d]", nBegin, nEnd);

         PNDEF2GenWrite(pContext,
                        pNDEFConnection->hConnection, static_PNDEFType2WriteNDEFCompleted,
                        pNDEFConnection,
                        pNDEFConnection->sType.t2.pBuffer + pNDEFConnection->sType.t2.nBytesProcessed,
                        nBegin,
                        pNDEFConnection->sType.t2.nBytesPending);

         break;

      case P_NDEF_COMMAND_MOVE_READ :

         PDebugTrace("static_PNDEFType2ReadWriteAreaAutomaton : MOVE : read[%d-%d]", nBegin, nEnd);

         PNDEF2GenRead(pContext,
                       pNDEFConnection->hConnection, static_PNDEFType2MoveAutomaton,
                       pNDEFConnection,
                       pNDEFConnection->sType.t2.pBuffer + pNDEFConnection->sType.t2.nBytesProcessed,
                       nBegin,
                       pNDEFConnection->sType.t2.nBytesPending);

         break;

      case P_NDEF_COMMAND_MOVE_WRITE :

         PDebugTrace("static_PNDEFType2ReadWriteAreaAutomaton : MOVE : write[%d-%d]", nBegin, nEnd);

         PNDEF2GenWrite(pContext,
                        pNDEFConnection->hConnection, static_PNDEFType2MoveAutomaton,
                        pNDEFConnection,
                        pNDEFConnection->sType.t2.pBuffer + pNDEFConnection->sType.t2.nBytesProcessed,
                        nBegin,
                        pNDEFConnection->sType.t2.nBytesPending);

         break;
   }

   /* the operation is not yet complete */
   return W_FALSE;
}


/* @brief   Receives the answer to a Type 2 Tag read NDEF command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The value provided to the function PNDEF2GenRead() when the operation was initiated.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFType2ReadNDEFCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*) pCallbackParameter;
   bool_t bIsComplete = W_FALSE;

   PDebugTrace("static_PNDEFType2ReadNDEFCompleted");

   if (nError == W_SUCCESS)
   {
      bIsComplete = static_PNDEFType2ReadWriteAreaAutomaton(pContext, pNDEFConnection);
   }

   if ((bIsComplete != W_FALSE) || (nError != W_SUCCESS))
   {
      W_ERROR nError2;

      nError2 = PNDEFSendCommandCompleted(
                  pContext,
                  pNDEFConnection,
                  pNDEFConnection->sType.t2.pBuffer,
                  pNDEFConnection->sType.t2.nBytesProcessed,
                  nError );

      if (nError2 != W_SUCCESS)
      {
         /* Send the error */
         PNDEFSendError(pContext, pNDEFConnection, nError2 );
      }
   }
}


/* @brief   Receives the answer to a Type 2 Tag write NDEF command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The value provided to the function PNDEF2GenWrite() when the operation was initiated.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFType2WriteNDEFCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*) pCallbackParameter;
   W_ERROR nError2;
   bool_t bIsComplete = W_FALSE;

   if (nError == W_SUCCESS)
   {
      bIsComplete = static_PNDEFType2ReadWriteAreaAutomaton(pContext, pNDEFConnection);
   }

   if ((bIsComplete != W_FALSE) || (nError != W_SUCCESS))
   {
      if(bIsComplete != W_FALSE)
      {
         /* Special case for OTP Card (Only Kovio) */
         if (  ( PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_KOVIO_RFID) == W_SUCCESS) &&
               ( pNDEFConnection->nMessageLength > 0) )
         {
            pNDEFConnection->bIsLockable = W_FALSE;
            pNDEFConnection->bIsLocked   = W_TRUE;
         }
      }

      /* Call the generic callback function */
      nError2 = PNDEFSendCommandCompleted(
                  pContext,
                  pNDEFConnection,
                  null,
                  0,
                  nError );

      if (nError2 != W_SUCCESS)
      {
         /* Send the error */
         PNDEFSendError(pContext, pNDEFConnection, nError2 );
      }
   }
}

/**
  * @brief TAG locking automaton
  *
  * @param[in]  pContext  The context.
  *
  * @param[in]  pCallbackParameter  The value provided to the function PNDEF2GenWrite() when the operation was initiated.
  *
  * @param[in]  nError  The error code of the operation.
  */
static void static_PNDEFType2LockAutomaton(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;
   W_ERROR nError2;

   if (nError != W_SUCCESS)
   {
      /* Call the generic callback function */
      nError2 = PNDEFSendCommandCompleted(
               pContext,
               pNDEFConnection,
               null,
               0,
               nError );

      if (nError2 != W_SUCCESS)
      {
         PNDEFSendError(pContext, pNDEFConnection, nError2 );
      }

      return;
   }

   switch (pNDEFConnection->sType.t2.nCommandWriteState)
   {
      case P_NDEF_2_WRITE_START :

         pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_WRITE_CC;

         /* Set the lock flag in the CC file */
         pNDEFConnection->aCCFile[P_NDEF_2_CC_ACCESS_BYTE_OFFSET] |= 0x0F;

         PNDEF2GenWrite(
            pContext,
            pNDEFConnection->hConnection,
            static_PNDEFType2LockAutomaton,
            pNDEFConnection,
            &pNDEFConnection->aCCFile[P_NDEF_2_CC_ACCESS_BYTE_OFFSET],
            P_NDEF_2_CC_OFFSET + P_NDEF_2_CC_ACCESS_BYTE_OFFSET,
            1);

         break;

      case P_NDEF_2_WRITE_CC :

         /* The tag is now considered locked */
         pNDEFConnection->bIsLocked = W_TRUE;

         if (pNDEFConnection->sType.t2.nDynamicLockSize != 0)
         {
            /* the CC has been set to WO, lock the TAG */

            pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_LOCK_TAG;

            /* Set all dynamic lock bits to 1 */
            CMemoryFill(pNDEFConnection->pSendBuffer, 0xFF, pNDEFConnection->sType.t2.nDynamicLockSize);
            /* All unused bits in the last byte shall always be set to 0 */
            pNDEFConnection->pSendBuffer[pNDEFConnection->sType.t2.nDynamicLockSize - 1] >>= pNDEFConnection->sType.t2.nDynamicLockSize * 8 - pNDEFConnection->sType.t2.nDynamicLockBits;

            PNDEF2GenWrite(
               pContext,
               pNDEFConnection->hConnection,
               static_PNDEFType2LockAutomaton,
               pNDEFConnection,
               pNDEFConnection->pSendBuffer,
               pNDEFConnection->sType.t2.nDynamicLockAddress,
               pNDEFConnection->sType.t2.nDynamicLockSize);

            break;
         }

         /* no dynamic lock bits */
         /* fall through */

      case P_NDEF_2_LOCK_TAG :

         /* the CC has been set to WO, lock the TAG */

         pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_LOCK2_TAG;

         /* Set all static lock bits to 1 */
         pNDEFConnection->pSendBuffer[0] = 0xFF;
         pNDEFConnection->pSendBuffer[1] = 0xFF;

         PNDEF2GenWrite(
            pContext,
            pNDEFConnection->hConnection,
            static_PNDEFType2LockAutomaton,
            pNDEFConnection,
            pNDEFConnection->pSendBuffer,
            P_NDEF_2_LOCK_OFFSET,
            P_NDEF_2_LOCK_LENGTH);

         break;

      case P_NDEF_2_LOCK2_TAG :

         /* The TAG has been locked, the operation is now completed */

         /* The tag is no more lockable */
         pNDEFConnection->bIsLockable = W_FALSE;

         /* Update Mifare data if needed */
         if (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MIFARE_UL) == W_SUCCESS)
         {
            nError2 = PMifareNDEF2Lock(pContext, pNDEFConnection->hConnection);
         }
         /* Update My-d data if needed */
         else if ((PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MY_D_MOVE) == W_SUCCESS) ||
                  (PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MY_D_NFC) == W_SUCCESS))
         {
            nError2 = PMyDNDEF2Lock(pContext, pNDEFConnection->hConnection);
         }
         else
         {
            nError2 = W_SUCCESS;
         }

         if (W_SUCCESS == nError2)
         {
            nError2 = PNDEFSendCommandCompleted(
                  pContext,
                  pNDEFConnection,
                  null,
                  0,
                  nError);
         }

         if (W_SUCCESS != nError2)
         {
            PNDEFSendError(pContext, pNDEFConnection, nError2 );
         }

         break;
   }
}

/**
 * @brief  NDEF move automaton, used to increase the NDEF length
 *
 * @param[in] pContext The context
 *
 * @param[in] pNDEFConnection The NDEF connection
 *
 * @return W_TRUE if the operation is completed, W_FALSE
 */
static void static_PNDEFType2MoveAutomaton(tContext* pContext, void* pCallbackParameter, W_ERROR nError)
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;
   W_ERROR nError2;
   bool_t bIsComplete;

   if (nError != W_SUCCESS)
   {
      /* Call the generic callback function */
      nError2 = PNDEFSendCommandCompleted(
               pContext,
               pNDEFConnection,
               null,
               0,
               nError );

      if (nError2 != W_SUCCESS)
      {
         PNDEFSendError(pContext, pNDEFConnection, nError2 );
      }

      return;
   }

   switch (pNDEFConnection->sType.t2.nCommandWriteState)
   {
      case P_NDEF_2_MOVE_START :

         PDebugTrace("static_PNDEFType2MoveAutomaton : start reading the area to move");

         pNDEFConnection->sType.t2.nBegin = P_NDEF_2_TLV_OFFSET + pNDEFConnection->nNDEFId + 1 + pNDEFConnection->nByteLength;
         pNDEFConnection->sType.t2.nBytesToProcess = pNDEFConnection->nNDEFFileLength;
         pNDEFConnection->sType.t2.nBytesProcessed  = 0;
         pNDEFConnection->sType.t2.nBytesPending  = 0;

         /* Length byte(s) are temporary set to 0 during move */
         CMemoryFill(pNDEFConnection->pReceivedBuffer, 0, (pNDEFConnection->nByteLength == 1) ? 3 : 1);
         /* Bytes to move are read in tag */
         pNDEFConnection->sType.t2.pBuffer = &pNDEFConnection->pReceivedBuffer[(pNDEFConnection->nByteLength == 1) ? 3 : 1];

         pNDEFConnection->sType.t2.nCurrentOperation = P_NDEF_COMMAND_MOVE_READ;
         pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_MOVE_READ;

         static_PNDEFType2MoveAutomaton(pContext, pNDEFConnection, W_SUCCESS);
         break;

      case P_NDEF_2_MOVE_READ :

         PDebugTrace("static_PNDEFType2MoveAutomaton : reading of the area to move in progress");

         bIsComplete = static_PNDEFType2ReadWriteAreaAutomaton(pContext, pNDEFConnection);

         if (bIsComplete != W_FALSE)
         {

            PDebugTrace("static_PNDEFType2MoveAutomaton : start writing of the area to move");

            /* Switch size of length */
            pNDEFConnection->nByteLength = (pNDEFConnection->nByteLength == 1) ? 3 : 1;

            pNDEFConnection->sType.t2.nBegin  = P_NDEF_2_TLV_OFFSET + pNDEFConnection->nNDEFId + 1;
            pNDEFConnection->sType.t2.nBytesToProcess = pNDEFConnection->nNDEFFileLength + pNDEFConnection->nByteLength;
            pNDEFConnection->sType.t2.nBytesProcessed  = 0;
            pNDEFConnection->sType.t2.nBytesPending  = 0;
            pNDEFConnection->sType.t2.pBuffer = pNDEFConnection->pReceivedBuffer;

            pNDEFConnection->sType.t2.nCurrentOperation = P_NDEF_COMMAND_MOVE_WRITE;
            pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_MOVE_WRITE;

            static_PNDEFType2MoveAutomaton(pContext, pNDEFConnection, W_SUCCESS);
         }
         break;

      case P_NDEF_2_MOVE_WRITE :

         PDebugTrace("static_PNDEFType2MoveAutomaton : writing of the area to move in progress");

         bIsComplete = static_PNDEFType2ReadWriteAreaAutomaton(pContext, pNDEFConnection);

         if (bIsComplete != W_FALSE)
         {

            PDebugTrace("static_PNDEFType2MoveAutomaton : start of the NDEF message write");

            pNDEFConnection->sType.t2.nCommandWriteState = P_NDEF_2_WRITE_START;

            pNDEFConnection->sType.t2.nBegin = P_NDEF_2_TLV_OFFSET + pNDEFConnection->nNDEFId + 1 + pNDEFConnection->nByteLength + pNDEFConnection->sType.t2.nPendingWriteOffset;
            pNDEFConnection->sType.t2.nBytesToProcess = pNDEFConnection->sType.t2.nPendingWriteLength;
            pNDEFConnection->sType.t2.nBytesProcessed  = 0;
            pNDEFConnection->sType.t2.nBytesPending  = 0;
            pNDEFConnection->sType.t2.pBuffer = pNDEFConnection->pMessageBuffer;

            pNDEFConnection->sType.t2.nCurrentOperation = P_NDEF_COMMAND_WRITE_NDEF;

            static_PNDEFType2WriteNDEFCompleted(pContext, pNDEFConnection, W_SUCCESS);
         }

         break;
   }
}


/* ------------------------------------------------------- */
/*                CACHE MANAGEMENT                         */
/* ------------------------------------------------------- */

/**
  * @brief Invalidate cache associated to the connection
  *
  * @param[in] pContext The context
  *
  * @param[in] pNDEFConnection The NDEF connection
  */

static W_ERROR static_PNDEFType2InvalidateCache(
   tContext* pContext,
   tNDEFConnection* pNDEFConnection,
   uint32_t nOffset,
   uint32_t nLength)
{
   uint32_t nBegin = P_NDEF_2_TLV_OFFSET + nOffset + pNDEFConnection->nNDEFId + 1 + pNDEFConnection->nByteLength;
   uint32_t nEnd;
   W_ERROR  nError;

   while (nLength != 0)
   {
      /* if the area start belongs to a reserved memory area, skip it */

      if (( pNDEFConnection->sType.t2.nDynamicLockAddress <= nBegin) && (nBegin < pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicLockSize))
      {
         nBegin = pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicLockSize;
      }

      if ((pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress <= nBegin) && (nBegin < pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress))
      {
         nBegin = pNDEFConnection->sType.t2.nDynamicLockAddress + pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress;
      }

      /* if a reserved memory area belongs to the current area, shorten the read range */

      nEnd = nBegin + nLength - 1;

      if ((nBegin <= pNDEFConnection->sType.t2.nDynamicLockAddress) && (pNDEFConnection->sType.t2.nDynamicLockAddress < nEnd))
      {
         nEnd = pNDEFConnection->sType.t2.nDynamicLockAddress - 1;
      }

      if ((nBegin <= pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress) && (pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress < nEnd))
      {
         nEnd = pNDEFConnection->sType.t2.nDynamicReservedMemoryAddress - 1;
      }

      nError = PNDEF2GenInvalidateCache(pContext, pNDEFConnection->hConnection, nBegin, nEnd - nBegin + 1);

      if (nError != W_SUCCESS)
      {
         PDebugError("static_PNDEFType2InvalidateCache : PType1ChipUserInvalidateCache returned %d", nError);
      }

      /* update nBegin and nLength for the next round */
      nLength -= nEnd - nBegin + 1;
      nBegin = nEnd;
   }

   return W_SUCCESS;
}


/* The NDEF type information structure */
tNDEFTypeEntry g_sPNDEFType2Info = {
   W_PROP_NFC_TAG_TYPE_2,
   static_PNDEFType2CreateConnection,
   static_PNDEFType2SendCommand,
   static_PNDEFType2InvalidateCache
};

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

