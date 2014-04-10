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
   Contains the NDEF Type 3 API implementation.
*******************************************************************************/
#define P_MODULE P_MODULE_DEC( NDEFA3 )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* NFC Type 3 Tag defines */
#define P_NDEF_3_MAPPING_VERSION             0x10       /* 1.0 */
#define P_NDEF_3_BLOCK_SIZE                  0x10

/* Tag 3 states */

#define P_NDEF_3_STATE_READWRITE             0x01
#define P_NDEF_3_STATE_READONLY              0x02

#define P_NDEF_3_CC_BLOCK                    0x01

#define P_NDEF_3_SERVICE_READ_ONLY           0x000B
#define P_NDEF_3_SERVICE_READ_WRITE          0x0009

/* NDEF type 3 capability container description */

#define NDEF_TYPE_3_VER             0
#define NDEF_TYPE_3_NBR             1
#define NDEF_TYPE_3_NBW             2
#define NDEF_TYPE_3_MAX_NB_H        3
#define NDEF_TYPE_3_MAX_NB_L        4
#define NDEF_TYPE_3_WRITE_F         9
#define NDEF_TYPE_3_RW_FLAG         10
#define NDEF_TYPE3_LN_H             11
#define NDEF_TYPE3_LN_M             12
#define NDEF_TYPE3_LN_L             13
#define NDEF_TYPE3_CKSUM_H          14
#define NDEF_TYPE3_CKSUM_L          15

#define P_DFC_TYPE3                 3

/* ------------------------------------------------------- */
/*                CONNECTION CREATION                      */
/* ------------------------------------------------------- */

static void static_PNDEFtype3ReadCCCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError);
static W_ERROR static_PNDEFType3ReadCapabilityContainer(tContext* pContext, tNDEFConnection* pNDEFConnection );
static W_ERROR static_PNDEFType3WriteCapabilityContainer(tContext* pContext, tNDEFConnection* pNDEFConnection );

/**
 * @brief   Creates a NDEF TAG Type 3 connection
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The PNDEF connection
 *
 * @return W_SUCCESS if the current connection is NDEF type 3 capable
 *
 * The NDEF connection is completed when the PNDEFSendError() is called
 **/
static W_ERROR static_PNDEFType3CreateConnection(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )

{
   W_ERROR nError;
   uint16_t nServiceCode   = P_NDEF_3_SERVICE_READ_ONLY;

   PDebugTrace("static_PNDEFType3CreateConnection");

   /* Check the type */
   if ( ( nError = PFeliCaUserCheckType(pContext, pNDEFConnection->hConnection ) ) != W_SUCCESS )
   {
      PDebugError("PNDEFtype3CreateConnection: not correct Type 3");
      return nError;
   }
   /* Set the maximum space size */
   pNDEFConnection->nMaximumSpaceSize = 0;

   /* Reset the index value */
   pNDEFConnection->sType.t3.nSectorSize = P_NDEF_3_BLOCK_SIZE;

   /* Read the Capability Container */
   pNDEFConnection->nReceivedDataLength = P_NDEF_3_BLOCK_SIZE;

   pNDEFConnection->sType.t3.pBlockElement = CMemoryAlloc(2);

   if (pNDEFConnection->sType.t3.pBlockElement == null)
   {
      PDebugError("static_PNDEFType3CreateConnection : could not allocate block list");
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pNDEFConnection->sType.t3.pBlockElement[0] = 0x80;
   pNDEFConnection->sType.t3.pBlockElement[1] = 0x00;

   PFeliCaReadInternal(
      pContext,
      pNDEFConnection->hConnection,
      static_PNDEFtype3ReadCCCompleted,
      pNDEFConnection,
      pNDEFConnection->aCCFile,
      P_NDEF_3_BLOCK_SIZE,
      1,
      &nServiceCode,
      1,
      pNDEFConnection->sType.t3.pBlockElement);

   return nError;
}

/**
 * @brief Capability Container Read callback
 *
 */
static void static_PNDEFtype3ReadCCCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;

   /* free the block element list allocated for the operation */
   CMemoryFree(pNDEFConnection->sType.t3.pBlockElement);
   pNDEFConnection->sType.t3.pBlockElement = null;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFtype3ReadCCCompleted : nError %s", PUtilTraceError(nError));
      goto end;
   }

   /* Parse the capability container */
   nError =  static_PNDEFType3ReadCapabilityContainer(pContext, pNDEFConnection);

end:

   switch(nError)
   {
      case W_SUCCESS: break;

      case W_ERROR_RF_COMMUNICATION:
      case W_ERROR_RF_PROTOCOL_NOT_SUPPORTED:
      case W_ERROR_TIMEOUT:
         nError = W_ERROR_RF_COMMUNICATION;
         break;

      default:
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         break;
   }

   PDFCPostContext2(&pNDEFConnection->sCallbackContext, nError);
}

/**
 * @brief   Parses the content of the capability container
 *
 */
static W_ERROR static_PNDEFType3ReadCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   uint16_t nComputedChecksum;
   uint8_t  i;

   pNDEFConnection->sType.t3.nVersion =  pNDEFConnection->aCCFile[NDEF_TYPE_3_VER];

   /* Mapping version */
   PDebugTrace(
      "PNDEFType3ReadCapabilityContainer: version %d.%d",
      (pNDEFConnection->sType.t3.nVersion >> 4),
      (pNDEFConnection->sType.t3.nVersion & 0x0F) );

   /* Check the mapping version */
   if ( ( P_NDEF_3_MAPPING_VERSION & 0xF0 ) < ( pNDEFConnection->sType.t3.nVersion & 0xF0 ) )
   {
      PDebugError("PNDEFType3ReadCapabilityContainer: higher version");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   pNDEFConnection->sType.t3.nNumberofBlockforCheckCommand =  pNDEFConnection->aCCFile[NDEF_TYPE_3_NBR];
   pNDEFConnection->sType.t3.nNumberofBlockforUpdateCommand =  pNDEFConnection->aCCFile[NDEF_TYPE_3_NBW];
   pNDEFConnection->sType.t3.nMaximumNumberofBlock =  (pNDEFConnection->aCCFile[NDEF_TYPE_3_MAX_NB_H]<<8) & 0xFF00;
   pNDEFConnection->sType.t3.nMaximumNumberofBlock +=  pNDEFConnection->aCCFile[NDEF_TYPE_3_MAX_NB_L];

   pNDEFConnection->nMaximumSpaceSize = pNDEFConnection->sType.t3.nMaximumNumberofBlock * P_NDEF_3_BLOCK_SIZE;
   if (pNDEFConnection->nMaximumSpaceSize == 0)
   {
      PDebugError("PNDEFType3ReadCapabilityContainer: NDEF file size is ZERO");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* check that pNDEFConnection->pReceivedBuffer can store the nMaximumSpaceSize */
   if (PNDEFUpdateBufferSize(pNDEFConnection,
                             pNDEFConnection->nMaximumSpaceSize) != W_SUCCESS)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   pNDEFConnection->sType.t3.nWritingState =  pNDEFConnection->aCCFile[NDEF_TYPE_3_WRITE_F];
   pNDEFConnection->sType.t3.nAccessAttribute = pNDEFConnection->aCCFile[NDEF_TYPE_3_RW_FLAG];

   pNDEFConnection->nNDEFFileLength  =  ((pNDEFConnection->aCCFile[NDEF_TYPE3_LN_H]<<16)&0xFF0000);
   pNDEFConnection->nNDEFFileLength +=  ((pNDEFConnection->aCCFile[NDEF_TYPE3_LN_M]<<8)&0xFF00);
   pNDEFConnection->nNDEFFileLength +=  pNDEFConnection->aCCFile[NDEF_TYPE3_LN_L];

   pNDEFConnection->nFreeSpaceSize = pNDEFConnection->nMaximumSpaceSize - pNDEFConnection->nNDEFFileLength;

   pNDEFConnection->sType.t3.nChecksum =  ((pNDEFConnection->aCCFile[NDEF_TYPE3_CKSUM_H]<<8)&0xFF00);
   pNDEFConnection->sType.t3.nChecksum +=  pNDEFConnection->aCCFile[NDEF_TYPE3_CKSUM_L];

   nComputedChecksum = 0;
   /* check the checksum */
   for (i=0;i<14;i++)
   {
      nComputedChecksum = nComputedChecksum + (uint16_t)(pNDEFConnection->aCCFile[i]);
   }

   pNDEFConnection->sType.t3.bIsChecksumCorrect = W_FALSE;

   /* Check checksums are identical */
   if (nComputedChecksum != pNDEFConnection->sType.t3.nChecksum)
   {
      PDebugError("PNDEFType3ReadCapabilityContainer: checksum error");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   pNDEFConnection->sType.t3.bIsChecksumCorrect = W_TRUE;


   if (pNDEFConnection->sType.t3.nAccessAttribute == 0x00)
   {
      pNDEFConnection->sType.t3.nTagState = P_NDEF_3_STATE_READONLY;
   }
   else if (pNDEFConnection->sType.t3.nAccessAttribute == 0x00)
   {
      pNDEFConnection->sType.t3.nTagState = P_NDEF_3_STATE_READWRITE;
   }

   return W_SUCCESS;
}

/* ------------------------------------------------------- */
/*                COMMAND PROCESSING                       */
/* ------------------------------------------------------- */

static void static_PNDEFtype3WriteCCCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void static_PNDEFType3ReadWriteNDEFSlice(tContext* pContext, tNDEFConnection* pNDEFConnection, uint32_t nOffset, uint32_t nLength);


static void static_PNDEFtype3ReadWriteNDEFSliceCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = pCallbackParameter;
   W_ERROR nError2;

   PDebugTrace("static_PNDEFtype3ReadWriteNDEFSliceCompleted : nError %s", PUtilTraceError(nError));

   if (pNDEFConnection->sType.t3.pBlockElement != null)
   {
      CMemoryFree(pNDEFConnection->sType.t3.pBlockElement);
      pNDEFConnection->sType.t3.pBlockElement = null;
   }

   if (nError != W_SUCCESS)
   {

      /* Call the generic callback function */
      nError2 = PNDEFSendCommandCompleted( pContext, pNDEFConnection, null, 0, nError );

      if (nError2 != W_SUCCESS)
      {
         /* Send the error */
         PNDEFSendError( pContext, pNDEFConnection, nError2 );
      }

      return;
   }

   pNDEFConnection->sType.t3.nBytesProcessed += pNDEFConnection->sType.t3.nBytesPending;
   pNDEFConnection->sType.t3.nBytesPending = 0;

   if (pNDEFConnection->sType.t3.nBytesProcessed < pNDEFConnection->sType.t3.nBytesToProcess)
   {
      static_PNDEFType3ReadWriteNDEFSlice(pContext, pNDEFConnection,
         pNDEFConnection->sType.t3.nBegin + pNDEFConnection->sType.t3.nBytesProcessed,
         pNDEFConnection->sType.t3.nBytesToProcess - pNDEFConnection->sType.t3.nBytesProcessed);
   }
   else
   {
      /* Call the generic callback function */
      if (pNDEFConnection->sType.t3.nCurrentOperation == P_NDEF_COMMAND_READ_NDEF)
      {
         nError2 = PNDEFSendCommandCompleted( pContext, pNDEFConnection,
                     pNDEFConnection->pReceivedBuffer + pNDEFConnection->sType.t3.nBegin % P_NDEF_3_BLOCK_SIZE,
                     pNDEFConnection->sType.t3.nBytesProcessed, W_SUCCESS );
      }
      else
      {
         nError2 = PNDEFSendCommandCompleted( pContext, pNDEFConnection, null, 0, W_SUCCESS );
      }


      if (nError2 != W_SUCCESS)
      {
         /* Send the error */
         PNDEFSendError( pContext, pNDEFConnection, nError2 );
      }
   }
}


static void static_PNDEFtype3ReadFirstBlockCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = pCallbackParameter;
   W_ERROR nError2;
   uint8_t  aServiceCode[2] = { P_NDEF_3_SERVICE_READ_WRITE & 0xFF, P_NDEF_3_SERVICE_READ_WRITE >> 8 };
   uint8_t  aBlock[3];
   uint32_t nBlock;

   PDebugTrace("static_PNDEFtype3ReadFirstBlockCompleted : nError %s", PUtilTraceError(nError));

   if (nError != W_SUCCESS)
   {
      /* Call the generic callback function */
      nError2 = PNDEFSendCommandCompleted( pContext, pNDEFConnection, null, 0, nError );

      if (nError2 != W_SUCCESS)
      {
         /* Send the error */
         PNDEFSendError( pContext, pNDEFConnection, nError2 );
      }

      return;
   }

   CMemoryCopy(pNDEFConnection->pReceivedBuffer + pNDEFConnection->sType.t3.nBegin % P_NDEF_3_BLOCK_SIZE, pNDEFConnection->pMessageBuffer, P_NDEF_3_BLOCK_SIZE);

   nBlock = pNDEFConnection->sType.t3.nBegin / P_NDEF_3_BLOCK_SIZE + 1;

   if (nBlock < 255)
   {
      aBlock[0] = 0x80;
      aBlock[1] = (uint8_t) nBlock;
   }
   else
   {
      aBlock[0] = 0;
      aBlock[1] = (uint8_t)((nBlock & 0xFF00) >> 8);
      aBlock[2] = (uint8_t)(nBlock & 0xFF);
   }

   pNDEFConnection->sType.t3.nBytesPending = P_NDEF_3_BLOCK_SIZE - pNDEFConnection->sType.t3.nBegin % P_NDEF_3_BLOCK_SIZE;

   PFeliCaWriteInternal(
      pContext,
      pNDEFConnection->hConnection,
      static_PNDEFtype3ReadWriteNDEFSliceCompleted,
      pNDEFConnection,
      pNDEFConnection->pReceivedBuffer,
      P_NDEF_3_BLOCK_SIZE,
      1,
      (uint16_t *) aServiceCode,
      1,
      aBlock);
}


static void static_PNDEFType3UpdateFirstBlock(
               tContext* pContext,
               tNDEFConnection* pNDEFConnection,
               uint32_t nOffset,
               uint32_t nLength)
{
   uint8_t  aServiceCode[2] = { P_NDEF_3_SERVICE_READ_ONLY & 0xFF, P_NDEF_3_SERVICE_READ_ONLY >> 8 };
   uint8_t  aBlock[3];
   uint32_t nBlock;

   nBlock = nOffset / P_NDEF_3_BLOCK_SIZE + 1;

   PDebugTrace("static_PNDEFType3ReadFirstBlock : reading block %d", nBlock);

   if (nBlock < 255)
   {
      aBlock[0] = 0x80;
      aBlock[1] = (uint8_t) nBlock;
   }
   else
   {
      aBlock[0] = 0;
      aBlock[1] = (uint8_t)((nBlock & 0xFF00) >> 8);
      aBlock[2] = (uint8_t)(nBlock & 0xFF);
   }

   pNDEFConnection->sType.t3.pBlockElement = null;

   PFeliCaReadInternal(
      pContext,
      pNDEFConnection->hConnection,
      static_PNDEFtype3ReadFirstBlockCompleted,
      pNDEFConnection,
      pNDEFConnection->pReceivedBuffer,
      P_NDEF_3_BLOCK_SIZE,
      1,
      (uint16_t *) aServiceCode,
      1,
      aBlock);
}

/**
  * Reads a slice from the currently selected file
  *
  * @param[in] pContext The context
  *
  * @param[in] pNDEFConnection The NDEFConnection,
  *
  * @param[in] nOffset The offset
  *
  * @param[in] nLength The length
  */

static void static_PNDEFType3ReadWriteNDEFSlice(
               tContext* pContext,
               tNDEFConnection* pNDEFConnection,
               uint32_t nOffset,
               uint32_t nLength)
{
   uint32_t nBlockStart;
   uint32_t nBlockEnd;
   uint32_t i;
   uint8_t  aServiceCode[2];
   uint8_t *pBuffer;
   uint32_t nNumberOfBlocks;
   uint8_t  nLen;

   /* The NDEF message is stored from second sector of the card, but offset is given from the start of the card,
      so we add one to each block number to skip the first sector */

   pNDEFConnection->sType.t3.nBytesPending = nLength;

   nBlockStart = nOffset / P_NDEF_3_BLOCK_SIZE + 1;
   nBlockEnd   = (nOffset + nLength - 1) / P_NDEF_3_BLOCK_SIZE + 1;

   nNumberOfBlocks = nBlockEnd - nBlockStart + 1;

   if (pNDEFConnection->sType.t3.nCurrentOperation == P_NDEF_COMMAND_READ_NDEF)
   {
      aServiceCode[0] = P_NDEF_3_SERVICE_READ_ONLY & 0xFF;
      aServiceCode[1] = P_NDEF_3_SERVICE_READ_ONLY >> 8;

      if (nNumberOfBlocks > pNDEFConnection->sType.t3.nNumberofBlockforCheckCommand)
      {
         nNumberOfBlocks = pNDEFConnection->sType.t3.nNumberofBlockforCheckCommand;
         nBlockEnd = nBlockStart +  nNumberOfBlocks - 1;

         pNDEFConnection->sType.t3.nBytesPending = nNumberOfBlocks * P_NDEF_3_BLOCK_SIZE;
      }
   }
   else
   {
      aServiceCode[0] = P_NDEF_3_SERVICE_READ_WRITE & 0xFF;
      aServiceCode[1] = P_NDEF_3_SERVICE_READ_WRITE >> 8;


      if (nNumberOfBlocks > pNDEFConnection->sType.t3.nNumberofBlockforUpdateCommand)
      {
         nNumberOfBlocks = pNDEFConnection->sType.t3.nNumberofBlockforUpdateCommand;
         nBlockEnd = nBlockStart +  nNumberOfBlocks - 1;

         pNDEFConnection->sType.t3.nBytesPending = nNumberOfBlocks * P_NDEF_3_BLOCK_SIZE;
      }
   }

   /* prepare the block list */

   if (nBlockEnd <= 0xFF)
   {
      nLen = 2;
   }
   else
   {
      /* need to support 3 bytes block list */
      /* we could decrease amount of memory needed here, but this situation is not likely to occur */
      nLen = 3;
   }

   pBuffer = pNDEFConnection->sType.t3.pBlockElement = (uint8_t*) CMemoryAlloc(nLen*nNumberOfBlocks);

   if (pBuffer == null)
   {
      tDFCCallbackContext sCallbackContext;

      PDebugError("static_PNDEFType3SendCommand : unable to allocate the block element list");
      PDFCFillCallbackContext(pContext, (tDFCCallback*) static_PNDEFtype3ReadWriteNDEFSliceCompleted , pNDEFConnection, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
      return;
   }

   CMemoryFill(pBuffer, 0, nLen*nNumberOfBlocks);

   for (i = nBlockStart; i <= nBlockEnd; i++)
   {
      if (i <= 0xFF)
      {
         /* 2-bytes block descriptor */
         * pBuffer++ = (uint8_t) 0x80;
         * pBuffer++ = (uint8_t) i;
      }
      else
      {
         /* 3-bytes block descriptor (MSB or LSB ?) */
         * pBuffer++ = 0x00;
         * pBuffer++ = (uint8_t)((i & 0xFF00) >> 8);
         * pBuffer++ = (uint8_t)(i & 0xFF);
      }
   }

   if (pNDEFConnection->sType.t3.nCurrentOperation == P_NDEF_COMMAND_READ_NDEF)
   {

      PDebugTrace("static_PNDEFType3ReadWriteNDEFSlice : reading [%d - %d]", nBlockStart * P_NDEF_3_BLOCK_SIZE, (nBlockEnd + 1)  *  P_NDEF_3_BLOCK_SIZE - 1);

      PFeliCaReadInternal(
         pContext,
         pNDEFConnection->hConnection,
         static_PNDEFtype3ReadWriteNDEFSliceCompleted,
         pNDEFConnection,
         pNDEFConnection->pReceivedBuffer + pNDEFConnection->sType.t3.nBytesProcessed,
         nNumberOfBlocks * P_NDEF_3_BLOCK_SIZE,
         1,
         (uint16_t *) aServiceCode,
         (uint8_t)nNumberOfBlocks,
         pBuffer = pNDEFConnection->sType.t3.pBlockElement);
   }
   else
   {
      PDebugTrace("static_PNDEFType3ReadWriteNDEFSlice : writing [%d - %d]", nBlockStart * P_NDEF_3_BLOCK_SIZE, (nBlockEnd + 1)  *  P_NDEF_3_BLOCK_SIZE - 1);

      PFeliCaWriteInternal(
         pContext,
         pNDEFConnection->hConnection,
         static_PNDEFtype3ReadWriteNDEFSliceCompleted,
         pNDEFConnection,
         pNDEFConnection->pMessageBuffer + pNDEFConnection->sType.t3.nBytesProcessed,
         nNumberOfBlocks * P_NDEF_3_BLOCK_SIZE,
         1,
         (uint16_t *) aServiceCode,
         (uint8_t)nNumberOfBlocks,
         pBuffer = pNDEFConnection->sType.t3.pBlockElement);
   }
}

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
static W_ERROR static_PNDEFType3SendCommand(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nOffset,
            uint32_t nLength )
{
   uint32_t nBlockStart;
   uint32_t nBlockEnd;

   W_ERROR nError = W_SUCCESS;

   /* Send the corresponding command */
   switch ( pNDEFConnection->nCommandType )
   {
      /* Generic commands */

      case P_NDEF_COMMAND_READ_NDEF:
      case P_NDEF_COMMAND_WRITE_NDEF:

         PDebugTrace("static_PNDEFType3SendCommand : nOffset %d - nLength %d", nOffset, nLength);

         nBlockStart = nOffset  / P_NDEF_3_BLOCK_SIZE;
         nBlockEnd   = (nOffset + nLength - 1) / P_NDEF_3_BLOCK_SIZE;

         if ((nBlockStart >= pNDEFConnection->sType.t3.nMaximumNumberofBlock) ||
             (nBlockEnd >= pNDEFConnection->sType.t3.nMaximumNumberofBlock))
         {
            PDebugError("static_PNDEFType3SendCommand : trying to access beyond the end of the NDEF file");
            return W_ERROR_BAD_PARAMETER;
         }

         if (pNDEFConnection->nCommandType == P_NDEF_COMMAND_WRITE_NDEF)
         {
            if (pNDEFConnection->sType.t3.nTagState == P_NDEF_3_STATE_READONLY)
            {
               PDebugError("static_PNDEFType3SendCommand: no write access allowed");
               return W_ERROR_BAD_STATE;
            }

            pNDEFConnection->sType.t3.nCurrentOperation = P_NDEF_COMMAND_WRITE_NDEF;
         }
         else
         {
            pNDEFConnection->sType.t3.nCurrentOperation = P_NDEF_COMMAND_READ_NDEF;
         }


         pNDEFConnection->sType.t3.nBegin = nOffset;
         pNDEFConnection->sType.t3.nBytesToProcess = nLength;
         pNDEFConnection->sType.t3.nBytesProcessed = 0;

         if ((pNDEFConnection->nCommandType == P_NDEF_COMMAND_WRITE_NDEF) && ((nOffset % P_NDEF_3_BLOCK_SIZE) != 0))
         {
            static_PNDEFType3UpdateFirstBlock(pContext, pNDEFConnection, nOffset, nLength);
         }
         else
         {
            static_PNDEFType3ReadWriteNDEFSlice(pContext, pNDEFConnection, nOffset, nLength);
         }

         break;

      case P_NDEF_COMMAND_WRITE_NDEF_LENGTH:

         PDebugTrace("static_PNDEFType3SendCommand: P_NDEF_COMMAND_WRITE_NDEF_LENGTH");

         if (pNDEFConnection->sType.t3.nTagState == P_NDEF_3_STATE_READONLY)
         {
            PDebugError("static_PNDEFType3SendCommand: no write access allowed");
            return W_ERROR_BAD_STATE;
         }

         pNDEFConnection->nNDEFFileLength = pNDEFConnection->nUpdatedNDEFFileLength;
         pNDEFConnection->nFreeSpaceSize  = pNDEFConnection->nMaximumSpaceSize - pNDEFConnection->nNDEFFileLength;

         pNDEFConnection->aCCFile[NDEF_TYPE3_LN_H] = (uint8_t)((pNDEFConnection->nNDEFFileLength >> 16) & 0xFF);
         pNDEFConnection->aCCFile[NDEF_TYPE3_LN_M] = (uint8_t)((pNDEFConnection->nNDEFFileLength >> 8) & 0xFF);
         pNDEFConnection->aCCFile[NDEF_TYPE3_LN_L] = (uint8_t)(pNDEFConnection->nNDEFFileLength & 0xFF);

         nError = static_PNDEFType3WriteCapabilityContainer(pContext, pNDEFConnection);
         break;


      case P_NDEF_COMMAND_LOCK_TAG :

         PDebugTrace("static_PNDEFType3SendCommand : P_NDEF_COMMAND_LOCK_TAG");

         /* this operation is not supported on this type of tag,
            simply return success without any processing */

         nError = PNDEFSendCommandCompleted(pContext, pNDEFConnection, null, 0, W_SUCCESS);
         break;

      default:
         PDebugError("static_PNDEFType3SendCommand: command 0x%02X not supported", pNDEFConnection->nCommandType);
         return W_ERROR_BAD_PARAMETER;
   }

   return nError;
}

/**
  * @brief Writes Capability container
  *
  * @param[in] pContext The context
  *
  * @param[in] pNDEFConnection The NDEF connection
  */
static W_ERROR static_PNDEFType3WriteCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   uint16_t nServiceCode   = P_NDEF_3_SERVICE_READ_WRITE;
   uint8_t aBlockElement[] = {0x80, 0x00};
   uint16_t nComputedChecksum = 0;
   uint8_t i;

   pNDEFConnection->sType.t3.pBlockElement = CMemoryAlloc(2);

   if (pNDEFConnection->sType.t3.pBlockElement == null)
   {
      PDebugError("static_PNDEFType3WriteCapabilityContainer : could not allocate block list");
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pNDEFConnection->sType.t3.pBlockElement[0] = 0x80;
   pNDEFConnection->sType.t3.pBlockElement[1] = 0x00;

   for (i=0;i<14;i++)
   {
      nComputedChecksum +=(uint16_t)pNDEFConnection->aCCFile[i];
   }

   pNDEFConnection->aCCFile[14] = (uint8_t) (nComputedChecksum >> 8);
   pNDEFConnection->aCCFile[15] = (uint8_t) nComputedChecksum;

   PFeliCaWriteInternal(
      pContext,
      pNDEFConnection->hConnection,
      static_PNDEFtype3WriteCCCompleted,
      pNDEFConnection,
      pNDEFConnection->aCCFile,
      P_NDEF_3_BLOCK_SIZE,
      1,
      &nServiceCode,
      1,
      (uint8_t *)aBlockElement);

   return W_SUCCESS;
}

/* @brief   Write Capability container callback function
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFtype3WriteCCCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;
   W_ERROR nError2;

   /* free the block element list allocated for the operation */
   CMemoryFree(pNDEFConnection->sType.t3.pBlockElement);
   pNDEFConnection->sType.t3.pBlockElement = null;

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
      PNDEFSendError( pContext, pNDEFConnection, nError2 );
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

static W_ERROR static_PNDEFType3InvalidateCache(
   tContext* pContext,
   tNDEFConnection* pNDEFConnection,
   uint32_t nOffset,
   uint32_t nLength)
{
   /* no cache associated with FeliCa */
   return W_SUCCESS;
}

/* The NDEF type information structure */
tNDEFTypeEntry g_sPNDEFType3Info = {
   W_PROP_NFC_TAG_TYPE_3,
   static_PNDEFType3CreateConnection,
   static_PNDEFType3SendCommand,
   static_PNDEFType3InvalidateCache };

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

