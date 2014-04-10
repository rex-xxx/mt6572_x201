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
   Contains the NDEF Type 5 API implementation.
*******************************************************************************/
#define P_MODULE P_MODULE_DEC( NDEFA5 )

#include "wme_context.h"

#if defined(P_INCLUDE_PICOPASS) && ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC))

/* Inside Type 5 Tag defines */
#define P_NDEF_5_MAPPING_VERSION             0x10       /* 1.0 */
#define P_NDEF_5_CC_BLOCK                    0x03
#define P_NDEF_5_NDEF_BLOCK                  0x04


static void static_PNDEFType5PicoRead( tContext* pContext, tNDEFConnection* pNDEFConnection,
                     tPBasicGenericCallbackFunction* pCallback, uint32_t nOffset, uint32_t nLength);

/* ------------------------------------------------------- */
/*                CONNECTION CREATION                      */
/* ------------------------------------------------------- */

static void static_PNDEFType5ReadCCCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static W_ERROR static_PNDEFType5ReadCapabilityContainer(tContext* pContext, tNDEFConnection* pNDEFConnection );

/**
 * @brief   Creates a NDEF TAG Type 5 connection
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The PNDEF connection
 *
 * @return W_SUCCESS if the current connection is NDEF type 5 capable
 *
 * The NDEF connection is completed when the PNDEFSendError() is called
 **/
static W_ERROR static_PNDEFType5CreateConnection(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   uint32_t nMaxSize;
   W_ERROR nError;
   bool_t bIsFormattable;

   /* Check the type */
   if ( ( nError = PPicoCheckType5(
                        pContext,
                        pNDEFConnection->hConnection,
                        &nMaxSize,
                        &pNDEFConnection->bIsLocked,
                        &pNDEFConnection->bIsLockable,
                        &bIsFormattable ) ) != W_SUCCESS )
   {
      PDebugError("static_PNDEFType5CreateConnection: not correct type 5");
      return nError;
   }

   /* Set the maximum space size */
   pNDEFConnection->nMaximumSpaceSize = nMaxSize;

   /* check that pNDEFConnection->pReceivedBuffer can store the nMaxSize */
   if (PNDEFUpdateBufferSize(pNDEFConnection, nMaxSize) != W_SUCCESS )
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Read the Capability Container and the first 3 bytes of the NDEF file */
   static_PNDEFType5PicoRead( pContext, pNDEFConnection,
            static_PNDEFType5ReadCCCompleted,
            P_NDEF_5_CC_BLOCK * P_PICO_BLOCK_SIZE,
            P_PICO_BLOCK_SIZE + 0x03);

   return W_SUCCESS;
}

/**
 * @brief Capability Container Read callback
 *
 */
static void static_PNDEFType5ReadCCCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;

   PDebugTrace("static_PNDEFType5ReadCCCompleted : Error %s", PUtilTraceError(nError));

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFType5ReadCCCompleted : Error %s", PUtilTraceError(nError));
      /* Send the error */
      goto end;
   }

   /* copy the contents of the received buffer in the CC file */
   CMemoryCopy(pNDEFConnection->aCCFile, pNDEFConnection->pReceivedBuffer, P_PICO_BLOCK_SIZE + 0x03);

   /* parse the contents of the capability container */
   nError = static_PNDEFType5ReadCapabilityContainer(pContext, pNDEFConnection);

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
 * @brief   Parses the content of the capability container
 *
 * @return W_SUCCESS if the capability container content is valid
 */
static W_ERROR static_PNDEFType5ReadCapabilityContainer(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{
   uint32_t nIndex = 0;
   uint32_t nLength;
   uint16_t nLen;

   static const uint8_t pType5String[] = { 0x4E, 0x44, 0x45, 0x46 }; /* "NDEF" */

   /* Check the NDEF identification string */
   if ( CMemoryCompare(
            pType5String,
            pNDEFConnection->aCCFile,
            4 ) != 0 )
   {
      PDebugLog("static_PNDEFType5ReadCapabilityContainer: wrong identification string");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   nIndex = 4;

   /* Mapping version */
   PDebugTrace(
      "static_PNDEFType5ReadCapabilityContainer: version %d.%d",
      (pNDEFConnection->aCCFile[nIndex] >> 4),
      (pNDEFConnection->aCCFile[nIndex] & 0x0F) );
   if ( ( P_NDEF_5_MAPPING_VERSION & 0xF0 ) < ( pNDEFConnection->aCCFile[nIndex] & 0xF0 ) )
   {
      PDebugError("static_PNDEFType5ReadCapabilityContainer: higher version");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Calculate the maximum message length */
   nLength  = (pNDEFConnection->aCCFile[nIndex + 1] << 8) + pNDEFConnection->aCCFile[nIndex + 2];

   if ( nLength + 2 > pNDEFConnection->nMaximumSpaceSize )
   {
      PDebugWarning(
         "static_PNDEFType5ReadCapabilityContainer: wrong length 0x%02X",
         nLength );

      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Store the maximum file size */
   pNDEFConnection->nMaximumSpaceSize = nLength;
   pNDEFConnection->nFreeSpaceSize = nLength;

   PDebugTrace("static_PNDEFType5ReadCapabilityContainer: nMaximumSpaceSize 0x%04X", pNDEFConnection->nMaximumSpaceSize );

   /* Set the default file id */
   pNDEFConnection->nNDEFId = P_NDEF_5_NDEF_BLOCK;

   /* Check the RFU value */
   if ( pNDEFConnection->aCCFile[nIndex + 3] != 0x00 )
   {
      PDebugError("static_PNDEFType5ReadCapabilityContainer: wrong RFU value");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* retrieve the actual NDEF length (two first bytes of the NDEF area) */

   nLen = (((uint16_t)(pNDEFConnection->aCCFile[nIndex + 4])) << 8) + (uint16_t)(pNDEFConnection->aCCFile[nIndex + 5]);

   if ((nLen == 0xFFFF) || (nLen > pNDEFConnection->nMaximumSpaceSize))
   {
      PDebugError( "static_PNDEFType5ReadCapabilityContainer, NDEF File length not valid");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   pNDEFConnection->nNDEFFileLength = nLen;
   pNDEFConnection->nFreeSpaceSize = pNDEFConnection->nMaximumSpaceSize - pNDEFConnection->nNDEFFileLength;

  return W_SUCCESS;
}

/* ------------------------------------------------------- */
/*                COMMAND PROCESSING                       */
/* ------------------------------------------------------- */

static void static_PNDEFType5ReadCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void static_PNDEFType5WriteCompleted(tContext* pContext, void* pCallbackParameter, W_ERROR nError );

/**
 * Calls the tag layer to perform a read operation.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The connection.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  nOffset  The offset in bytes.
 *
 * @param[in]  nLength  The length in bytes.
 **/
static void static_PNDEFType5PicoRead(
                     tContext* pContext,
                     tNDEFConnection* pNDEFConnection,
                     tPBasicGenericCallbackFunction* pCallback,
                     uint32_t nOffset,
                     uint32_t nLength)
{
   W_HANDLE hSubOperation = W_NULL_HANDLE;
   W_HANDLE* phSubOperation;
   if(pNDEFConnection->hCurrentOperation != W_NULL_HANDLE)
   {
      phSubOperation = &hSubOperation;
   }
   else
   {
      phSubOperation = null;
   }

   /* Store the parameter to be used in the callback */
   pNDEFConnection->nReceivedDataLength = nLength;

   PPicoReadInternal(
      pContext,
      pNDEFConnection->hConnection,
      pCallback,
      pNDEFConnection,
      pNDEFConnection->pReceivedBuffer,
      nOffset,
      nLength,
      phSubOperation );

   if((pNDEFConnection->hCurrentOperation != W_NULL_HANDLE)
   && (hSubOperation != W_NULL_HANDLE))
   {
      if(PBasicAddSubOperationAndClose(pContext,
         pNDEFConnection->hCurrentOperation, hSubOperation) != W_SUCCESS)
      {
         PDebugError(
         "static_PNDEFType5PicoRead: error returned by PBasicAddSubOperationAndClose(), ignored");
      }
   }
}

/**
 * Calls the tag layer to perform a write operation.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The connection.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pBuffer  The pointer to data.
 *
 * @param[in]  nOffset  The offset in bytes.
 *
 * @param[in]  nLength  The length in bytes.
 *
 * @param[in]  bLockTag  The lock tag flag.
 **/
static void static_PNDEFType5PicoWrite(
                     tContext* pContext,
                     tNDEFConnection* pNDEFConnection,
                     tPBasicGenericCallbackFunction* pCallback,
                     uint8_t * pBuffer,
                     uint32_t nOffset,
                     uint32_t nLength,
                     bool_t bLockTag)
{
   W_HANDLE hSubOperation = W_NULL_HANDLE;
   W_HANDLE* phSubOperation;

   phSubOperation = (pNDEFConnection->hCurrentOperation != W_NULL_HANDLE) ? &hSubOperation : null;

   PPicoWriteInternal(pContext, pNDEFConnection->hConnection, pCallback, pNDEFConnection,
                      pBuffer, nOffset, nLength, bLockTag, phSubOperation);

   if ((pNDEFConnection->hCurrentOperation != W_NULL_HANDLE) && (hSubOperation != W_NULL_HANDLE))
   {
      if(PBasicAddSubOperationAndClose(pContext,
         pNDEFConnection->hCurrentOperation, hSubOperation) != W_SUCCESS)
      {
         PDebugError(
         "static_PNDEFType5PicoWrite: error returned by PBasicAddSubOperationAndClose(), ignored");
      }
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
  * @return W_SUCCESS on success
  */
static W_ERROR static_PNDEFType5SendCommand(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nOffset,
            uint32_t nLength )
{
   W_ERROR ErrCde = W_SUCCESS;

   /* Send the corresponding command */
   switch ( pNDEFConnection->nCommandType )
   {
      case P_NDEF_COMMAND_READ_NDEF:
         PDebugTrace("static_PNDEFType5SendCommand: P_NDEF_COMMAND_READ_NDEF");

         static_PNDEFType5PicoRead( pContext, pNDEFConnection, static_PNDEFType5ReadCompleted,
                                    P_NDEF_5_NDEF_BLOCK * P_PICO_BLOCK_SIZE + 2 + nOffset,   /* The first two bytes of P_NDEF_5_NDEF_BLOCK is the length of NDEF Messages */
                                    nLength);
         break;


      case P_NDEF_COMMAND_WRITE_NDEF:

         PDebugTrace("static_PNDEFType5SendCommand: P_NDEF_COMMAND_WRITE_NDEF");

         static_PNDEFType5PicoWrite( pContext, pNDEFConnection, static_PNDEFType5WriteCompleted,
                  pNDEFConnection->pMessageBuffer,
                  (pNDEFConnection->nNDEFId * P_PICO_BLOCK_SIZE) + nOffset + 2,
                  nLength,
                  W_FALSE);

         break;


      case P_NDEF_COMMAND_WRITE_NDEF_LENGTH:

         PDebugTrace("static_PNDEFType5SendCommand: P_NDEF_COMMAND_WRITE_NDEF_LENGTH");

         pNDEFConnection->nNDEFFileLength = pNDEFConnection->nUpdatedNDEFFileLength;

         pNDEFConnection->pSendBuffer[0] = (uint8_t) (pNDEFConnection->nNDEFFileLength >> 8);
         pNDEFConnection->pSendBuffer[1] = (uint8_t) pNDEFConnection->nNDEFFileLength;

         static_PNDEFType5PicoWrite( pContext, pNDEFConnection,
                  static_PNDEFType5WriteCompleted,
                  pNDEFConnection->pSendBuffer,
                  pNDEFConnection->nNDEFId * P_PICO_BLOCK_SIZE,
                  2,
                  W_FALSE);

         break;

      case P_NDEF_COMMAND_LOCK_TAG:

         PDebugTrace("static_PNDEFType5SendCommand: P_NDEF_COMMAND_LOCK_TAG");

         /* Send the command */
         static_PNDEFType5PicoWrite( pContext, pNDEFConnection,
                     static_PNDEFType5WriteCompleted,
                     null,
                     0,
                     0,
                     W_TRUE);
         break;

      default:
         PDebugError("static_PNDEFType5SendCommand: command 0x%02X not supported", pNDEFConnection->nCommandType);
         return W_ERROR_BAD_PARAMETER;
   }

   return ErrCde;
}


/* @brief   Read command callback function
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The value provided to the function PMifareRead() when the operation was initiated.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFType5ReadCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;
   W_ERROR nError2;

   /* Call the generic callback function */
   nError2 = PNDEFSendCommandCompleted(
                     pContext,
                     pNDEFConnection,
                     pNDEFConnection->pReceivedBuffer,
                     pNDEFConnection->nReceivedDataLength,
                     nError );

   if (nError2 != W_SUCCESS )
   {
      /* Send the error */
      PNDEFSendError(
         pContext,
         pNDEFConnection,
         nError2 );
   }
}

/* @brief   Write command callback function
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The value provided to the function PMifareRead() when the operation was initiated.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_PNDEFType5WriteCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pCallbackParameter;
   W_ERROR nError2;

   /* Call the generic callback function */
   nError2 = PNDEFSendCommandCompleted(
                     pContext,
                     pNDEFConnection,
                     null,
                     0,
                     nError );

   if (nError2 != W_SUCCESS )
   {
      /* Send the error */
      PNDEFSendError(
         pContext,
         pNDEFConnection,
         nError2 );
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

static W_ERROR static_PNDEFType5InvalidateCache(
   tContext* pContext,
   tNDEFConnection* pNDEFConnection,
   uint32_t nOffset,
   uint32_t nLength)
{
   return PPicoInvalidateCache(pContext, pNDEFConnection->hConnection, pNDEFConnection->nNDEFId * P_PICO_BLOCK_SIZE + 2 + nOffset, nLength);
}


/* The NDEF type information structure */
tNDEFTypeEntry g_sPNDEFType5Info = {
   W_PROP_NFC_TAG_TYPE_5,
   static_PNDEFType5CreateConnection,
   static_PNDEFType5SendCommand,
   static_PNDEFType5InvalidateCache
};

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
