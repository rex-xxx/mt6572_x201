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
   Contains the NDEF Type 6 API implementation.
*******************************************************************************/
#define P_MODULE P_MODULE_DEC( NDEFA6 )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* NFC Type 6 Tag defines */
#define P_NDEF_6_CC_OFFSET                   0x00
#define P_NDEF_6_CC_ICODE_LENGTH             0x04
#define P_NDEF_6_CC_TYPE6_LENGTH             0x08
#define P_NDEF_6_CC_MAX_LENGTH               0x08

#define P_NDEF_6_MAGIC_NUMBER                0xE1
#define P_NDEF_6_MAPPING_VERSION             0x10       /* 1.0 */

#define P_NDEF_6_TLV_NULL                    0x00
#define P_NDEF_6_TLV_NDEF_MESSAGE            0x03
#define P_NDEF_6_TLV_PROPRIETARY             0xFD
#define P_NDEF_6_TLV_TERMINATOR              0xFE

#define P_NDEF_6_LOCK_START                  0x00
#define P_NDEF_6_WRITE_CC                    0x01
#define P_NDEF_6_LOCK_TAG                    0x02

/* ------------------------------------------------------- */
/*                CONNECTION CREATION                      */
/* ------------------------------------------------------- */

static void static_PNDEFType6ReadCCCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static W_ERROR static_PNDEFIcodeParseCapabilityContainer(tContext* pContext, tNDEFConnection* pNDEFConnection );
static W_ERROR static_PNDEFType6ParseCapabilityContainer(tContext* pContext, tNDEFConnection* pNDEFConnection );
static void static_PNDEFType6ReadNDEFLengthCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void static_PNDEFICodeReadNDEFFileCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );

/**
 * @brief   Creates a NDEF TAG Type 6 connection
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The PNDEF connection
 *
 * @return W_SUCCESS if the current connection is NDEF type 5 capable
 *
 * The NDEF connection is completed when the PNDEFSendError() is called
 **/
static W_ERROR static_PNDEFType6CreateConnection(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   uint32_t nMaxSize;
   W_ERROR nError;
   bool_t bIsFormattable;

   /* Check the type */
   if ( ( nError = P15P3UserCheckType6(
                        pContext,
                        pNDEFConnection->hConnection,
                        &nMaxSize,
                        &pNDEFConnection->bIsLocked,
                        &pNDEFConnection->bIsLockable,
                        &bIsFormattable ) ) != W_SUCCESS )
   {
      PDebugError("static_PNDEFType6CreateConnection: not correct type 6");
      return nError;
   }
   /* Set the maximum space size */
   pNDEFConnection->nMaximumSpaceSize = nMaxSize;

   /* check that pNDEFConnection->pReceivedBuffer can store the read static aera */
   if (PNDEFUpdateBufferSize(pNDEFConnection, pNDEFConnection->nMaximumSpaceSize) != W_SUCCESS )
   {
       return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Read the Capability Container */
   pNDEFConnection->nReceivedDataLength = P_NDEF_6_CC_MAX_LENGTH;
   P15ReadInternal(
            pContext,
            pNDEFConnection->hConnection,
            static_PNDEFType6ReadCCCompleted,
            pNDEFConnection,
            pNDEFConnection->aCCFile,
            P_NDEF_6_CC_OFFSET,
            P_NDEF_6_CC_MAX_LENGTH);

   return W_SUCCESS;
}


/**
 * @brief Capability Container Read callback
 *
 */
static void static_PNDEFType6ReadCCCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEFType6ReadCCCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFType6ReadCCCompleted : Error %s", PUtilTraceError(nError));
      /* Send the error */
      goto end;
   }

   /* Parse CC with ICODE format */
   nError = static_PNDEFIcodeParseCapabilityContainer(pContext, pNDEFConnection);

   if (nError != W_SUCCESS)
   {
      pNDEFConnection->sType.t6.bICodeFormat = W_FALSE;
      /* If it is not an ICODE formated tag, parse CC with Inside Type 6 format */
      nError = static_PNDEFType6ParseCapabilityContainer(pContext, pNDEFConnection);
   }
   else
   {
      pNDEFConnection->sType.t6.bICodeFormat = W_TRUE;
   }

   if (nError != W_SUCCESS)
   {
      goto end;
   }

   nError = PNDEFUpdateBufferSize(pNDEFConnection, pNDEFConnection->nMaximumSpaceSize);
   /* check that pNDEFConnection->pReceivedBuffer can store the read static aera */
   if ( nError != W_SUCCESS)
   {
       goto end;
   }

   if (pNDEFConnection->sType.t6.bICodeFormat == W_FALSE)
   {
      /* INSIDE TYPE 6 format */
      /* read the actual NDEF file size */
      pNDEFConnection->nReceivedDataLength = 2;

      P15ReadInternal(pContext,
               pNDEFConnection->hConnection,
               static_PNDEFType6ReadNDEFLengthCompleted,
               pNDEFConnection,
               pNDEFConnection->pReceivedBuffer,
               P_NDEF_6_CC_TYPE6_LENGTH,
               2);
   }
   else
   {
      /* ICODE format */
      /* read the NDEF file */
      pNDEFConnection->nReceivedDataLength = pNDEFConnection->nMaximumSpaceSize;

      P15ReadInternal(pContext,
               pNDEFConnection->hConnection,
               static_PNDEFICodeReadNDEFFileCompleted,
               pNDEFConnection,
               pNDEFConnection->pReceivedBuffer,
               P_NDEF_6_CC_ICODE_LENGTH,
               pNDEFConnection->nMaximumSpaceSize);
   }
   return;

end:

   switch(nError)
   {
      case W_SUCCESS:
         CDebugAssert(0);
         return;

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
static W_ERROR static_PNDEFIcodeParseCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   bool_t bWriteStatus = W_FALSE;
   uint8_t nSectorNumber;
   uint32_t nIndex = 0;

   /* Check the first CC byte */
   if (pNDEFConnection->aCCFile[nIndex] != P_NDEF_6_MAGIC_NUMBER)
   {
      PDebugLog("static_PNDEFIcodeParseCapabilityContainer: wrong magic number");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   nIndex++;
   /* Mapping version */
   PDebugTrace(
      "static_PNDEFIcodeParseCapabilityContainer: version %d.%d",
      (pNDEFConnection->aCCFile[nIndex] >> 6),
      ((pNDEFConnection->aCCFile[nIndex] >> 4) & 0x03) );
   if ( ( P_NDEF_6_MAPPING_VERSION >> 4 ) < ( pNDEFConnection->aCCFile[nIndex] >> 6 ) )
   {
      PDebugError("static_PNDEFIcodeParseCapabilityContainer: higher version");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Check the write access value */
   switch ( pNDEFConnection->aCCFile[nIndex] & 0xF )
   {
      case 0x0:
         PDebugTrace("static_PNDEFIcodeParseCapabilityContainer: not locked");
         bWriteStatus = W_FALSE;
         break;
      case 0x3:
         PDebugTrace("static_PNDEFIcodeParseCapabilityContainer: locked");
         bWriteStatus = W_TRUE;
         break;
      default:
         PDebugWarning(
            "static_PNDEFIcodeParseCapabilityContainer: incorrect write access 0x%X",
            pNDEFConnection->aCCFile[nIndex] & 0xF );
         return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   if ( pNDEFConnection->bIsLocked != bWriteStatus )
   {
      /* Inconsistency between physical lock and NDEF logical lock => consider the Tag as Read-Only, but allow Reading of the Tag */
      pNDEFConnection->bIsLocked = W_TRUE;

      PDebugWarning("static_PNDEFIcodeParseCapabilityContainer: inconsistent write access status");
   }

   nIndex++;
   /* Get the tag size */
   pNDEFConnection->sType.t6.nSectorSize = 4;
   nSectorNumber = pNDEFConnection->aCCFile[nIndex] * 8 / pNDEFConnection->sType.t6.nSectorSize;

   /* Set/Check the P15 sector number and size */
   if (P15SetTagSize(pContext, pNDEFConnection->hConnection, nSectorNumber, pNDEFConnection->sType.t6.nSectorSize ) == W_ERROR_BAD_PARAMETER)
   {
      PDebugError("static_PNDEFIcodeParseCapabilityContainer: unable to set the tag size (CC NDEF bad format)");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Set the maximum space size */
   pNDEFConnection->nMaximumSpaceSize = (nSectorNumber - 1) * pNDEFConnection->sType.t6.nSectorSize;
   pNDEFConnection->nFreeSpaceSize = pNDEFConnection->nMaximumSpaceSize;
   PDebugTrace(
      "static_PNDEFIcodeParseCapabilityContainer: nMaximumSpaceSize 0x%04X",
      pNDEFConnection->nMaximumSpaceSize );

   nIndex++;
   /* Get the supported commands */
   P15SetSupportedCommands(
      pContext,
      pNDEFConnection->hConnection,
      (pNDEFConnection->aCCFile[nIndex] & 0x01) != 0,
      W_FALSE);

   return W_SUCCESS;
}

/**
 * @brief   Parses the content of the capability container
 *
 */
static W_ERROR static_PNDEFType6ParseCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   bool_t bWriteStatus = W_FALSE;
   uint8_t nCCBlockNumber;
   uint8_t nSectorNumber;
   uint32_t nIndex = 0;
   uint32_t nLength;
   static const uint8_t pType6String[] = { 0x4E, 0x44, 0x45 }; /* "NDE" */

   /* Check the NDEF identification string */
   if ( CMemoryCompare(
            pType6String,
            pNDEFConnection->aCCFile,
            3 ) != 0 )
   {
      PDebugLog("static_PNDEFType6ParseCapabilityContainer: wrong identification string");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   nIndex = 3;

   /* Mapping version */
   PDebugTrace(
      "static_PNDEFType6ParseCapabilityContainer: version %d.%d",
      (pNDEFConnection->aCCFile[nIndex] >> 4),
      (pNDEFConnection->aCCFile[nIndex] & 0x0F) );
   if ( ( P_NDEF_6_MAPPING_VERSION & 0xF0 ) < ( pNDEFConnection->aCCFile[nIndex] & 0xF0 ) )
   {
      PDebugError("static_PNDEFType6ParseCapabilityContainer: higher version");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   nIndex ++;
   /* Get the block number and size */
   pNDEFConnection->sType.t6.nSectorSize = (pNDEFConnection->aCCFile[nIndex ++] & 0x1F) + 1;
   nSectorNumber = pNDEFConnection->aCCFile[nIndex ++] + 1;

   /* Set the sector number and size */
   if (P15SetTagSize(pContext, pNDEFConnection->hConnection, nSectorNumber, pNDEFConnection->sType.t6.nSectorSize ) == W_ERROR_BAD_PARAMETER)
   {
      PDebugError("static_PNDEFType6ParseCapabilityContainer: unable to set the tag size (CC NDEF bad format)");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Get the CC file block number */
   nCCBlockNumber = (P_NDEF_6_CC_TYPE6_LENGTH + pNDEFConnection->sType.t6.nSectorSize - 1) / pNDEFConnection->sType.t6.nSectorSize;

   pNDEFConnection->nMaximumSpaceSize = (nSectorNumber - nCCBlockNumber) * pNDEFConnection->sType.t6.nSectorSize - 2;

   /* Check the write access value */
   switch ( pNDEFConnection->aCCFile[nIndex ++] )
   {
      case 0x00:
         PDebugTrace("static_PNDEFType6ParseCapabilityContainer: not locked");
         bWriteStatus = W_FALSE;
         break;
      case 0xFF:
         PDebugTrace("static_PNDEFType6ParseCapabilityContainer: locked");
         bWriteStatus = W_TRUE;
         break;
      default:
         PDebugWarning(
            "static_PNDEFType6ParseCapabilityContainer: incorrect write access 0x%02X",
            pNDEFConnection->aCCFile[nIndex - 1] );
         return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   if ( pNDEFConnection->bIsLocked != bWriteStatus )
   {
      /* Inconsistency between physical lock and NDEF logical lock => consider the Tag as Read-Only, but allow Reading of the Tag */
      pNDEFConnection->bIsLocked = W_TRUE;

      PDebugWarning("static_PNDEFType6ParseCapabilityContainer: inconsistent write access status");
   }

   /* Check the maximum number of block */
   if ( pNDEFConnection->aCCFile[nIndex] > ( nSectorNumber - nCCBlockNumber ) )
   {
      PDebugWarning(
         "static_PNDEFType6ParseCapabilityContainer: wrong maximum block number 0x%02X",
         pNDEFConnection->aCCFile[nIndex] );
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   /* Calculate the maximum message length */
   nLength  = pNDEFConnection->aCCFile[nIndex] * pNDEFConnection->sType.t6.nSectorSize - 2;
   if ( nLength > pNDEFConnection->nMaximumSpaceSize )
   {
      PDebugWarning(
         "static_PNDEFType6ParseCapabilityContainer: wrong length 0x%02X",
         nLength );
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   /* Store the maximum file size */
   pNDEFConnection->nMaximumSpaceSize = nLength;
   pNDEFConnection->nFreeSpaceSize = nLength;
   PDebugTrace(
      "static_PNDEFType6ParseCapabilityContainer: nMaximumSpaceSize 0x%04X",
      pNDEFConnection->nMaximumSpaceSize );

   /* Set the default file id */
   pNDEFConnection->nNDEFId = nCCBlockNumber * pNDEFConnection->sType.t6.nSectorSize + 2;

   return W_SUCCESS;
}

/**
 * @brief   Read Type6 NDEF file's length callback function
 *
 */
static void static_PNDEFType6ReadNDEFLengthCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEFType6ReadNDEFLengthCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFType6ReadNDEFLengthCompleted : Error %s", PUtilTraceError(nError));

      /* Send the error */
      goto end;
   }

   pNDEFConnection->nNDEFFileLength = (pNDEFConnection->pReceivedBuffer[0] << 8) + pNDEFConnection->pReceivedBuffer[1];

   if (pNDEFConnection->nNDEFFileLength > pNDEFConnection->nMaximumSpaceSize)
   {
      PDebugError("static_PNDEFType6ReadNDEFLengthCompleted : NDEF actual size larger than NDEF maximum size");

      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto end;
   }

   /* Compute the remaining available size */
   pNDEFConnection->nFreeSpaceSize = pNDEFConnection->nMaximumSpaceSize - pNDEFConnection->nNDEFFileLength;

   /* Ok, the operation is now completed */
   nError = W_SUCCESS;

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

   PDFCPostContext2(&pNDEFConnection->sCallbackContext, nError);
}

/**
 * @brief   Read ICode NDEF file callback function. It parses the NDEF TLV blocks.
 *
 **/
static void static_PNDEFICodeReadNDEFFileCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   uint32_t nIndex = 0;
   uint8_t  nLengthFieldSize;
   uint32_t nLength;

   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEFICodeReadNDEFFileCompleted");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFICodeReadNDEFFileCompleted : Error %s", PUtilTraceError(nError));

      /* Send the error */
      PDFCPostContext2(&pNDEFConnection->sCallbackContext, W_ERROR_CONNECTION_COMPATIBILITY);
      return;
   }

   /* Walk through the TLV block(s) */
   while ( nIndex < pNDEFConnection->nReceivedDataLength )
   {
      /* Check the TLV type */
      switch ( pNDEFConnection->pReceivedBuffer[nIndex] )
      {
         /* null */
         case P_NDEF_6_TLV_NULL:
            PDebugTrace("static_PNDEFICodeReadNDEFFileCompleted: null");

            pNDEFConnection->nMaximumSpaceSize--;

            nIndex ++;
            break;

         /* Terminator */
         case P_NDEF_6_TLV_TERMINATOR:

            PDebugTrace("static_PNDEFICodeReadNDEFFileCompleted: Terminator");
            goto end;

         /* Parse NDEF Message TLV */
         /* Or jump over proprietary and other (reserved) TLVs*/
         default :

            PDebugTrace("static_PNDEFICodeReadNDEFFileCompleted: parse TLV Length");

            if ((nIndex + 1) >= pNDEFConnection->nReceivedDataLength)
            {
               PDebugError("static_PNDEFICodeReadNDEFFileCompleted: end of tag reached");
               goto end;
            }

            /* Check the length */
            if ( pNDEFConnection->pReceivedBuffer[nIndex + 1] < 0xFF )
            {
               nLength = pNDEFConnection->pReceivedBuffer[nIndex + 1];
               nLengthFieldSize = 1;
            }
            else
            {
               if ((nIndex + 3) >= pNDEFConnection->nReceivedDataLength)
               {
                  PDebugError("static_PNDEFICodeReadNDEFFileCompleted: end of tag reached");
                  goto end;
               }

               nLength = (pNDEFConnection->pReceivedBuffer[nIndex + 2] << 8) + (pNDEFConnection->pReceivedBuffer[nIndex + 3]);

               if (nLength < 0xFF)
               {
                  PDebugWarning("static_PNDEFICodeReadNDEFFileCompleted: wrong TLV length");
               }

               nLengthFieldSize = 3;
            }

            if (nIndex + 1 + nLengthFieldSize + nLength > pNDEFConnection->nReceivedDataLength)
            {
               PDebugError("static_PNDEFICodeReadNDEFFileCompleted : end of tag reached");
               goto end;
            }

            if (pNDEFConnection->pReceivedBuffer[nIndex] == P_NDEF_6_TLV_NDEF_MESSAGE)
            {
               PDebugTrace("static_PNDEFICodeReadNDEFFileCompleted: NDEF Message");

               pNDEFConnection->nNDEFId = (uint16_t)(P_NDEF_6_CC_ICODE_LENGTH + nIndex + 1 + nLengthFieldSize);

               pNDEFConnection->nByteLength = nLengthFieldSize;
               pNDEFConnection->nNDEFFileLength = nLength;

               pNDEFConnection->nMaximumSpaceSize -= 1 + pNDEFConnection->nByteLength;

               if ((pNDEFConnection->nByteLength == 1) && (pNDEFConnection->nMaximumSpaceSize >= 255))
               {
                  /* Length can grow on 3 bytes */
                  pNDEFConnection->nMaximumSpaceSize -= 2;
               }

               pNDEFConnection->nFreeSpaceSize  = pNDEFConnection->nMaximumSpaceSize - pNDEFConnection->nNDEFFileLength;

               /* Ok, the operation is now completed */
               PDFCPostContext2(&pNDEFConnection->sCallbackContext, W_SUCCESS);
               return;
            }
            else
            {
               PDebugTrace("static_PNDEFICodeReadNDEFFileCompleted: jump over others TLV");
               pNDEFConnection->nMaximumSpaceSize -= 1 + nLengthFieldSize + nLength;
            }

            nIndex += 1 + nLengthFieldSize + nLength;

            break;
      }
   }

   PDebugTrace("static_PNDEFICodeReadNDEFFileCompleted : end of tag reached");

end:

   PDebugTrace("static_PNDEFICodeReadNDEFFileCompleted : no NDEF TLV found");
   PDFCPostContext2(&pNDEFConnection->sCallbackContext, W_ERROR_CONNECTION_COMPATIBILITY);
}


/* ------------------------------------------------------- */
/*                COMMAND PROCESSING                       */
/* ------------------------------------------------------- */

static void static_PNDEFType6ReadNDEFCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void static_PNDEFType6WriteNDEFCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void static_PNDEFType6WriteNDEFLengthCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void static_PNDEFICodeReadForMoveCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void static_PNDEFType6LockAutomaton(tContext* pContext, void* pCallbackParameter, W_ERROR nError );

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
  * @return W_SUCCESS on success
  */
static W_ERROR static_PNDEFType6SendCommand(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nOffset,
            uint32_t nLength )
{
   /* Send the corresponding command */
   switch ( pNDEFConnection->nCommandType )
   {

      case P_NDEF_COMMAND_READ_NDEF:

         PDebugTrace("static_PNDEFType6SendCommand: P_NDEF_COMMAND_READ_NDEF");

         /* Get the file information */
         pNDEFConnection->nReceivedDataLength = nLength;

         P15ReadInternal(
            pContext,
            pNDEFConnection->hConnection,
            static_PNDEFType6ReadNDEFCompleted,
            pNDEFConnection,
            pNDEFConnection->pReceivedBuffer,
            pNDEFConnection->nNDEFId + nOffset,
            nLength);

         break;

      case P_NDEF_COMMAND_WRITE_NDEF:

         PDebugTrace("static_PNDEFType6SendCommand : P_NDEF_COMMAND_WRITE_NDEF");

         if (pNDEFConnection->sType.t6.bICodeFormat == W_FALSE)
         {
            /* INSIDE TYPE 6 format */
            P15WriteInternal(
               pContext,
               pNDEFConnection->hConnection,
               static_PNDEFType6WriteNDEFCompleted,
               pNDEFConnection,
               pNDEFConnection->pMessageBuffer,
               pNDEFConnection->nNDEFId + nOffset,
               nLength,
               W_FALSE);
         }
         else
         {
            /* ICODE format */
            uint8_t nRequestedByteLength = (pNDEFConnection->nUpdatedNDEFFileLength <= 254) ? 1 : 3;

            /* specific case when writing a nOffset = 0 (e.g we are replacing the content of the NDEF file)
               we directly set the nByteLength to the correct length since there's no need to keep
               previous tag content */
            if (nOffset == 0)
            {
               pNDEFConnection->nNDEFId = pNDEFConnection->nNDEFId - pNDEFConnection->nByteLength + nRequestedByteLength;
               pNDEFConnection->nByteLength = nRequestedByteLength;
            }

            if (nRequestedByteLength == pNDEFConnection->nByteLength)
            {
               /* the NDEF file length format is not changing */
               P15WriteInternal(
                  pContext,
                  pNDEFConnection->hConnection,
                  static_PNDEFType6WriteNDEFCompleted,
                  pNDEFConnection,
                  pNDEFConnection->pMessageBuffer,
                  pNDEFConnection->nNDEFId + nOffset,
                  nLength,
                  W_FALSE);
            }
            else
            {
               /* Need to change the format of the length of the NDEF file */
               PDebugTrace("static_PNDEFType2SendCommand : the NDEF size is changing...");

               /* store the write parameters for later processing */
               pNDEFConnection->sType.t6.nPendingWriteOffset = nOffset;
               pNDEFConnection->sType.t6.nPendingWriteLength = nLength;

               /* Read data to move */
               pNDEFConnection->nReceivedDataLength = pNDEFConnection->nNDEFFileLength;

               P15ReadInternal(
                  pContext,
                  pNDEFConnection->hConnection,
                  static_PNDEFICodeReadForMoveCompleted,
                  pNDEFConnection,
                  &pNDEFConnection->pReceivedBuffer[nRequestedByteLength],
                  pNDEFConnection->nNDEFId,
                  pNDEFConnection->nNDEFFileLength);
            }
         }
         break;


      case P_NDEF_COMMAND_WRITE_NDEF_LENGTH:
         PDebugTrace("static_PNDEFType6SendCommand: P_NDEF_COMMAND_WRITE_NDEF_LENGTH");

         pNDEFConnection->nNDEFFileLength = pNDEFConnection->nUpdatedNDEFFileLength;
         pNDEFConnection->nFreeSpaceSize  = pNDEFConnection->nMaximumSpaceSize - pNDEFConnection->nNDEFFileLength;

         if (pNDEFConnection->sType.t6.bICodeFormat == W_FALSE)
         {
            /* INSIDE TYPE 6 format */
            pNDEFConnection->pSendBuffer[0] = (uint8_t) (pNDEFConnection->nNDEFFileLength >> 8);
            pNDEFConnection->pSendBuffer[1] = (uint8_t) pNDEFConnection->nNDEFFileLength;

            P15WriteInternal(
               pContext,
               pNDEFConnection->hConnection,
               static_PNDEFType6WriteNDEFLengthCompleted,
               pNDEFConnection,
               pNDEFConnection->pSendBuffer,
               P_NDEF_6_CC_TYPE6_LENGTH,
               2,
               W_FALSE);
         }
         else
         {
            /* ICODE format */
            if (pNDEFConnection->nByteLength == 1)
            {
               CDebugAssert(pNDEFConnection->nNDEFFileLength <= 254);

               pNDEFConnection->pSendBuffer[0] = (uint8_t) pNDEFConnection->nNDEFFileLength;
            }
            else
            {
               pNDEFConnection->pSendBuffer[0] = 0xFF;
               pNDEFConnection->pSendBuffer[1] = (uint8_t) (pNDEFConnection->nNDEFFileLength >> 8);
               pNDEFConnection->pSendBuffer[2] = (uint8_t) (pNDEFConnection->nNDEFFileLength);
            }

            P15WriteInternal(
               pContext,
               pNDEFConnection->hConnection,
               static_PNDEFType6WriteNDEFLengthCompleted,
               pNDEFConnection,
               pNDEFConnection->pSendBuffer,
               pNDEFConnection->nNDEFId - pNDEFConnection->nByteLength,
               pNDEFConnection->nByteLength,
               W_FALSE);
         }
         break;

      case P_NDEF_COMMAND_LOCK_TAG:

         PDebugTrace("static_PNDEFType6SendCommand: P_NDEF_COMMAND_LOCK_TAG");

         pNDEFConnection->sType.t6.nLockState = P_NDEF_6_LOCK_START;
         static_PNDEFType6LockAutomaton(pContext, pNDEFConnection, W_SUCCESS);
         break;

      default:
         PDebugError("static_PNDEFType6SendCommand: command 0x%02X not supported", pNDEFConnection->nCommandType);
         return W_ERROR_BAD_PARAMETER;
   }

   return W_SUCCESS;
}

/* @brief   Receives the answer to a Type 6 Tag read NDEF command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The value provided to the function P15Read() when the operation was initiated.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFType6ReadNDEFCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;
   W_ERROR nError2;

   if (nError == W_SUCCESS)
   {
      PDebugTrace("static_PNDEFType6ReadNDEFCompleted : nError %s", PUtilTraceError(nError));
   }
   else
   {
      PDebugError("static_PNDEFType6ReadNDEFCompleted : nError %s", PUtilTraceError(nError));
   }

   /* Call the generic callback function */
   nError2 = PNDEFSendCommandCompleted(
                     pContext,
                     pNDEFConnection,
                     pNDEFConnection->pReceivedBuffer,
                     pNDEFConnection->nReceivedDataLength,
                     nError );

   if ( (nError2 != W_SUCCESS))
   {
      /* Send the error */
      PNDEFSendError( pContext, pNDEFConnection, nError2 );
   }
}

/* @brief   Receives the answer to a Type 6 Tag write NDEF command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The value provided to the function P15WriteInternal() when the operation was initiated.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFType6WriteNDEFCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;
   W_ERROR nError2;

   if (nError == W_SUCCESS)
   {
      PDebugTrace("static_PNDEFType6WriteNDEFCompleted : nError %s", PUtilTraceError(nError));
   }
   else
   {
      PDebugError("static_PNDEFType6WriteNDEFCompleted : nError %s", PUtilTraceError(nError));
   }

   /* Call the generic callback function */
   nError2 = PNDEFSendCommandCompleted(
                     pContext,
                     pNDEFConnection,
                     null,
                     0,
                     nError );

   if ( (nError2 != W_SUCCESS))
   {
      /* Send the error */
      PNDEFSendError( pContext, pNDEFConnection, nError2 );
   }
}

/* @brief   Receives the answer to a Type 6 Tag write NDEF length command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The value provided to the function P15WriteInternal() when the operation was initiated.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFType6WriteNDEFLengthCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;
   W_ERROR nError2;

   if (nError == W_SUCCESS)
   {
      PDebugTrace("static_PNDEFType6WriteNDEFLengthCompleted : nError %s", PUtilTraceError(nError));
   }
   else
   {
      PDebugError("static_PNDEFType6WriteNDEFLengthCompleted : nError %s", PUtilTraceError(nError));
   }

   /* Call the generic callback function */
   nError2 = PNDEFSendCommandCompleted(
                     pContext,
                     pNDEFConnection,
                     null,
                     0,
                     nError );

   if ( (nError2 != W_SUCCESS))
   {
      /* Send the error */
      PNDEFSendError( pContext, pNDEFConnection, nError2 );
   }
}

/* @brief   Receives the answer to a Type 6 Tag read NDEF command.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The value provided to the function P15Read() when the operation was initiated.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFICodeReadForMoveCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;

   if (nError == W_SUCCESS)
   {
      PDebugTrace("static_PNDEFICodeReadForMoveCompleted : nError %s", PUtilTraceError(nError));
   }
   else
   {
      PDebugError("static_PNDEFICodeReadForMoveCompleted : nError %s", PUtilTraceError(nError));
      /* Send the error */
      PNDEFSendError( pContext, pNDEFConnection, nError );
   }

   /* Switch size of length */
   pNDEFConnection->nNDEFId -= pNDEFConnection->nByteLength;
   pNDEFConnection->nByteLength = (pNDEFConnection->nByteLength == 1) ? 3 : 1;
   pNDEFConnection->nNDEFId += pNDEFConnection->nByteLength;

   /* Temporary set length to 0 */
   CMemoryFill(pNDEFConnection->pReceivedBuffer, 0, pNDEFConnection->nByteLength);
   /* Append new data to write */
   CMemoryCopy(&pNDEFConnection->pReceivedBuffer[pNDEFConnection->sType.t6.nPendingWriteOffset], pNDEFConnection->pMessageBuffer, pNDEFConnection->sType.t6.nPendingWriteLength);

   /* Write the whole NDEF message */
   P15WriteInternal(
      pContext,
      pNDEFConnection->hConnection,
      static_PNDEFType6WriteNDEFCompleted,
      pNDEFConnection,
      pNDEFConnection->pReceivedBuffer,
      pNDEFConnection->nNDEFId - pNDEFConnection->nByteLength,
      pNDEFConnection->nByteLength + pNDEFConnection->nNDEFFileLength + pNDEFConnection->sType.t6.nPendingWriteLength,
      W_FALSE);
}

/**
  * @brief TAG locking automaton
  *
  * @param[in]  pContext  The context.
  *
  * @param[in]  pCallbackParameter  The value provided to the function PMifareRead() when the operation was initiated.
  *
  * @param[in]  nError  The error code of the operation.
  */
static void static_PNDEFType6LockAutomaton(
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

      if ( (nError2 != W_SUCCESS))
      {
         /* Send the error */
         PNDEFSendError( pContext, pNDEFConnection, nError2 );
         return;
      }
   }

   switch (pNDEFConnection->sType.t6.nLockState)
   {
      case P_NDEF_6_LOCK_START :
         {
         uint32_t nCCLength;

         if (pNDEFConnection->sType.t6.bICodeFormat == W_FALSE)
         {
            /* INSIDE TYPE 6 format */
            pNDEFConnection->aCCFile[0x06] = 0xFF;
            nCCLength = P_NDEF_6_CC_MAX_LENGTH;
         }
         else
         {
            /* ICODE format */
            pNDEFConnection->aCCFile[0x01] |= 0x03;
            nCCLength = pNDEFConnection->sType.t6.nSectorSize;
         }

         P15WriteInternal(
            pContext,
            pNDEFConnection->hConnection,
            static_PNDEFType6LockAutomaton,
            pNDEFConnection,
            pNDEFConnection->aCCFile,
            P_NDEF_6_CC_OFFSET,
            nCCLength,
            W_FALSE);

         pNDEFConnection->sType.t6.nLockState = P_NDEF_6_WRITE_CC;
         }
         break;

      case P_NDEF_6_WRITE_CC :

         P15WriteInternal(
            pContext,
            pNDEFConnection->hConnection,
            static_PNDEFType6LockAutomaton,
            pNDEFConnection,
            null,
            0,
            0,
            W_TRUE);

         pNDEFConnection->sType.t6.nLockState = P_NDEF_6_LOCK_TAG;
         break;


      case P_NDEF_6_LOCK_TAG :

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

static W_ERROR static_PNDEFType6InvalidateCache(
   tContext* pContext,
   tNDEFConnection* pNDEFConnection,
   uint32_t nOffset,
   uint32_t nLength)
{
   return P15InvalidateCache(pContext, pNDEFConnection->hConnection, pNDEFConnection->nNDEFId + nOffset, nLength);
}



/* The NDEF type information structure */
tNDEFTypeEntry g_sPNDEFType6Info = {
   W_PROP_NFC_TAG_TYPE_6,
   static_PNDEFType6CreateConnection,
   static_PNDEFType6SendCommand,
   static_PNDEFType6InvalidateCache};

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

