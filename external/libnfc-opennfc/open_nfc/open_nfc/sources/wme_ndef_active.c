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

#define P_MODULE P_MODULE_DEC( NDEFA )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Queued operation type */
#define P_NDEF_QUEUED_NONE                   0
#define P_NDEF_QUEUED_READ                   1
#define P_NDEF_QUEUED_WRITE                  2
#define P_NDEF_QUEUED_LOCK                   3
#define P_NDEF_QUEUED_FORMAT                 4

/* The NDEF Information types */
extern tNDEFTypeEntry g_sPNDEFType1Info;
extern tNDEFTypeEntry g_sPNDEFType2Info;
extern tNDEFTypeEntry g_sPNDEFType3Info;
extern tNDEFTypeEntry g_sPNDEFType4AInfo;
extern tNDEFTypeEntry g_sPNDEFType4BInfo;

#ifdef P_INCLUDE_PICOPASS
extern tNDEFTypeEntry g_sPNDEFType5Info;
#endif /* P_INCLUDE_PICOPASS */

extern tNDEFTypeEntry g_sPNDEFType6Info;

#ifdef P_INCLUDE_MIFARE_CLASSIC
extern tNDEFTypeEntry g_sPNDEFType7Info;
#endif /* P_INCLUDE_MIFARE_CLASSIC */

/* NDEF Information array */
static tNDEFTypeEntry* const g_aNDEFTypeArray[] =
{
   &g_sPNDEFType1Info,
   &g_sPNDEFType2Info,
   &g_sPNDEFType3Info,
   &g_sPNDEFType4AInfo,
   &g_sPNDEFType4BInfo,

#ifdef P_INCLUDE_PICOPASS
   &g_sPNDEFType5Info,
#endif /* P_INCLUDE_PICOPASS */
   &g_sPNDEFType6Info,

#ifdef P_INCLUDE_MIFARE_CLASSIC
   &g_sPNDEFType7Info
#endif /* P_INCLUDE_MIFARE_CLASSIC */
};

/* Destroy connection callback */
static uint32_t static_PNDEFDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static uint32_t static_PNDEFGetPropertyNumber(
            tContext* pContext,
            void* pObject);

/* Get properties connection callback */
static bool_t static_PNDEFGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_PNDEFCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* Execute the queued operation (after polling) */
static void static_PNDEFExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult);

/* Execute the queued format (after polling) */
static void static_PNDEFExecuteQueuedFormat(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult);

/* Handle NDEF connection type */
tHandleType g_sNDEFConnection = {   static_PNDEFDestroyConnection,
                                    null,
                                    static_PNDEFGetPropertyNumber,
                                    static_PNDEFGetProperties,
                                    static_PNDEFCheckProperties,
                                    null, null, null, null };

#define P_HANDLE_TYPE_NDEF_CONNECTION (&g_sNDEFConnection)

/**
 * @brief   Destroyes a NDEF connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PNDEFDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pObject;

   PDebugTrace("static_PNDEFDestroyConnection");

   PDFCFlushCall(&pNDEFConnection->sCallbackContext);

   /* Free the NDEF buffers */
   PDebugTrace("static_PNDEFDestroyConnection - CMemoryFree:\nnBufferLength = %u\npBuffer = %p\npReceivedBuffer = %p\npSendBuffer = %p\n",
                pNDEFConnection->nBufferLength, pNDEFConnection->pBuffer, pNDEFConnection->pReceivedBuffer, pNDEFConnection->pSendBuffer);
   CMemoryFree( pNDEFConnection->pBuffer );
   CMemoryFree( pNDEFConnection->pReceivedBuffer );
   CMemoryFree( pNDEFConnection->pSendBuffer );

   /* Free the NDEF connection structure */
   CMemoryFree( pNDEFConnection );

   return P_HANDLE_DESTROY_DONE;
}


/**
 * @brief   Gets the NDEF connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 **/
static uint32_t static_PNDEFGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/**
 * @brief   Gets the NDEF connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PNDEFGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pObject;

   PDebugTrace("static_PNDEFGetProperties");

   if ( pNDEFConnection->pTagType == null )
   {
      PDebugError("static_PNDEFGetProperties: no property");
      return W_FALSE;
   }

   pPropertyArray[0] = pNDEFConnection->pTagType->nProperty;
   return W_TRUE;
}

/**
 * @brief   Checkes the NDEF connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PNDEFCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*)pObject;

   PDebugTrace(
      "static_PNDEFCheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if ( pNDEFConnection->pTagType != null )
   {
      if ( nPropertyValue == pNDEFConnection->pTagType->nProperty )
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
      PDebugError("static_PNDEFCheckProperties: no property");
      return W_FALSE;
   }
}

/**
 * @brief   Checks the NDEF file data.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The NDEF connection structure.
 *
 * @param[in]  pReceivedBuffer  The NDEF file content from the specified offset.
 *
 * @param[in]  nReceivedLength  The NDEF file size from the specified offset.
 **/
static W_ERROR static_PNDEFCheckFile(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t* pReceivedBuffer,
            uint32_t nReceivedLength )
{
   /* Check the length */
   if ( pNDEFConnection->nMessageLength == nReceivedLength )
   {
      if ( CMemoryCompare( pNDEFConnection->pMessageBuffer, pReceivedBuffer, nReceivedLength ) == 0 )
      {
         return W_SUCCESS;
      }
   }

   /* Send the error */
   return W_ERROR_TAG_WRITE;
}

/* See Header file */
void PNDEFCreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nNDEFType )
{
   tNDEFConnection* pNDEFConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint32_t nPos;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the NDEF buffer */
   pNDEFConnection = (tNDEFConnection*)CMemoryAlloc( sizeof(tNDEFConnection) );
   if (pNDEFConnection == null)
   {
      PDebugError("PNDEFCreateConnection: pNDEFConnection == null");
      /* Send the error */
      PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
      return;
   }
   CMemoryFill(pNDEFConnection, 0, sizeof(tNDEFConnection));

   /* Initialize NDEF message buffers */
   pNDEFConnection->nBufferLength   = P_NDEF_MAX_LENGTH;
   PDebugTrace("PNDEFCreateConnection - before CMemoryAlloc:\nnBufferLength = %u\npBuffer = %p\npReceivedBuffer = %p\npSendBuffer = %p\n",
                pNDEFConnection->nBufferLength, pNDEFConnection->pBuffer, pNDEFConnection->pReceivedBuffer, pNDEFConnection->pSendBuffer);
   pNDEFConnection->pBuffer         = CMemoryAlloc(pNDEFConnection->nBufferLength);
   if (pNDEFConnection->pBuffer == null)
   {
      goto return_error;
   }
   pNDEFConnection->pReceivedBuffer = CMemoryAlloc(pNDEFConnection->nBufferLength);
   if (pNDEFConnection->pReceivedBuffer == null)
   {
      goto return_error;
   }
   pNDEFConnection->pSendBuffer     = CMemoryAlloc(pNDEFConnection->nBufferLength);
   if (pNDEFConnection->pSendBuffer == null)
   {
      goto return_error;
   }
   PDebugTrace("PNDEFCreateConnection - after CMemoryAlloc:\nnBufferLength = %u\npBuffer = %p\npReceivedBuffer = %p\npSendBuffer = %p\n",
                pNDEFConnection->nBufferLength, pNDEFConnection->pBuffer, pNDEFConnection->pReceivedBuffer, pNDEFConnection->pSendBuffer);

   /* Store the callback context */
   pNDEFConnection->sCallbackContext = sCallbackContext;

   /* Store the connection information */
   pNDEFConnection->hConnection      = hConnection;
   pNDEFConnection->nCommandState    = P_NDEF_ACTION_RESET;
   pNDEFConnection->pTagType         = null;

   for(nPos = 0; nPos < (sizeof(g_aNDEFTypeArray)/sizeof(tNDEFTypeEntry*)); nPos++)
   {
      if (g_aNDEFTypeArray[nPos]->nProperty == nNDEFType)
      {
         pNDEFConnection->pTagType = g_aNDEFTypeArray[nPos];
         break;
      }
   }

   if (pNDEFConnection->pTagType == null)
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   CDebugAssert(pNDEFConnection->pTagType->pCreateConnection != null);
   if ( ( nError = pNDEFConnection->pTagType->pCreateConnection(
               pContext,
               pNDEFConnection ) ) != W_SUCCESS )
   {
      goto return_error;
   }

   /* Add the NDEF connection structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hConnection,
                     pNDEFConnection,
                     P_HANDLE_TYPE_NDEF_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("PNDEFCreateConnection: could not add the NDEF buffer");
      goto return_error;
   }

   return;

return_error:
   /* Send the error */
   PDFCPostContext2(&sCallbackContext, W_ERROR_CONNECTION_COMPATIBILITY);

   /* Free the NDEF buffers */
   PDebugTrace("PNDEFCreateConnection - return_error - CMemoryFree:\nnBufferLength = %u\npBuffer = %p\npReceivedBuffer = %p\npSendBuffer = %p\n",
                pNDEFConnection->nBufferLength, pNDEFConnection->pBuffer, pNDEFConnection->pReceivedBuffer, pNDEFConnection->pSendBuffer);
   CMemoryFree( pNDEFConnection->pBuffer );
   CMemoryFree( pNDEFConnection->pReceivedBuffer );
   CMemoryFree( pNDEFConnection->pSendBuffer );

   /* Free the buffer */
   CMemoryFree(pNDEFConnection);

   return;
}

/* See Header file */
W_ERROR PNDEFUpdateBufferSize(
            tNDEFConnection* pNDEFConnection,
            uint32_t nLength)
{
   /* Temporary buffers */
   uint8_t* pBuffer         = null;
   uint8_t* pReceivedBuffer = null;
   uint8_t* pSendBuffer     = null;

   /* check if the NDEF message can be stored in the buffers */
   if (nLength > pNDEFConnection->nBufferLength)
   {
      /* Allocate buffers with the new length */
      pBuffer         = CMemoryAlloc(nLength);
      if (pBuffer == null)
      {
         goto return_error;
      }
      pReceivedBuffer = CMemoryAlloc(nLength);
      if (pReceivedBuffer == null)
      {
         goto return_error;
      }
      pSendBuffer     = CMemoryAlloc(nLength);
      if (pSendBuffer == null)
      {
         goto return_error;
      }

      /* copy old buffers content */
      CMemoryCopy(pBuffer,         pNDEFConnection->pBuffer,         pNDEFConnection->nBufferLength);
      CMemoryCopy(pReceivedBuffer, pNDEFConnection->pReceivedBuffer, pNDEFConnection->nBufferLength);
      CMemoryCopy(pSendBuffer,     pNDEFConnection->pSendBuffer,     pNDEFConnection->nBufferLength);

      /* free old buffers */
      PDebugTrace("PNDEFUpdateBufferSize - CMemoryFree:\nnBufferLength = %u\npBuffer = %p\npReceivedBuffer = %p\npSendBuffer = %p\n",
                  pNDEFConnection->nBufferLength, pNDEFConnection->pBuffer, pNDEFConnection->pReceivedBuffer, pNDEFConnection->pSendBuffer);
      CMemoryFree(pNDEFConnection->pBuffer);
      CMemoryFree(pNDEFConnection->pReceivedBuffer);
      CMemoryFree(pNDEFConnection->pSendBuffer);

      /* update buffers length */
      pNDEFConnection->nBufferLength   = nLength;

      /* buffers initialization */
      pNDEFConnection->pBuffer         = pBuffer;
      pNDEFConnection->pReceivedBuffer = pReceivedBuffer;
      pNDEFConnection->pSendBuffer     = pSendBuffer;
      PDebugTrace("PNDEFUpdateBufferSize - after CMemoryAlloc:\nnBufferLength = %u\npBuffer = %p\npReceivedBuffer = %p\npSendBuffer = %p\n",
                  pNDEFConnection->nBufferLength, pNDEFConnection->pBuffer, pNDEFConnection->pReceivedBuffer, pNDEFConnection->pSendBuffer);
   }
   return W_SUCCESS;

return_error:
   PDebugError("PNDEFUpdateBufferSize: could not re-allocate the NDEF buffers : W_ERROR_OUT_OF_RESOURCE");
   /* Free temporary buffers */
   CMemoryFree( pBuffer );
   CMemoryFree( pReceivedBuffer );
   CMemoryFree( pSendBuffer );
   return W_ERROR_OUT_OF_RESOURCE;
}

/* See Header file */
void PNDEFRemoveConnection(
            tContext* pContext,
            W_HANDLE hConnection )
{
   tNDEFConnection* pNDEFConnection;

   pNDEFConnection = PHandleRemoveLastHeir(pContext, hConnection, P_HANDLE_TYPE_NDEF_CONNECTION);

   if (pNDEFConnection != null)
   {

      PDebugTrace("PNDEFRemoveConnection %p ", pNDEFConnection);

      PDFCFlushCall(&pNDEFConnection->sCallbackContext);

      /* Free the NDEF buffers */
      PDebugTrace("PNDEFRemoveConnection - CMemoryFree:\nnBufferLength = %u\npBuffer = %p\npReceivedBuffer = %p\npSendBuffer = %p\n",
                   pNDEFConnection->nBufferLength, pNDEFConnection->pBuffer, pNDEFConnection->pReceivedBuffer, pNDEFConnection->pSendBuffer);
      CMemoryFree( pNDEFConnection->pBuffer );
      CMemoryFree( pNDEFConnection->pReceivedBuffer );
      CMemoryFree( pNDEFConnection->pSendBuffer );

      /* Free the NDEF connection structure */
      CMemoryFree( pNDEFConnection );
   }

}
/* See Header file */
W_ERROR PNDEFCheckReadParameters(
            tContext* pContext,
            uint8_t nTNF,
            const char16_t* pTypeString )
{
   uint32_t nLength = 0;

   PDebugTrace("PNDEFCheckReadParameters: nTNF 0x%02x", nTNF);

   /* Check the parameters */
   switch (nTNF)
   {
      case W_NDEF_TNF_EMPTY:
      case W_NDEF_TNF_UNKNOWN:
      case W_NDEF_TNF_ANY_TYPE:

         if ( pTypeString != null )
         {
            PDebugError("PNDEFCheckReadParameters: type string must be null");
            return W_ERROR_BAD_PARAMETER;
         }

         break;

      case W_NDEF_TNF_WELL_KNOWN:
      case W_NDEF_TNF_MEDIA:
      case W_NDEF_TNF_ABSOLUTE_URI:
      case W_NDEF_TNF_EXTERNAL:

         if (pTypeString == null)
         {
            PDebugError("PNDEFCheckReadParameters: type string must be not null");
            return W_ERROR_BAD_PARAMETER;
         }

         PDebugTrace("PNDEFCheckReadParameters: W_NDEF_TNF_WELL_KNOWN");

         nLength = PUtilStringLength(pTypeString);

         if ( (nLength == 0) || (nLength > P_NDEF_MAX_STRING_LENGTH))
         {
            PDebugError( "PNDEFCheckReadParameters: invalid type string length");
            return W_ERROR_BAD_PARAMETER;

         }
         break;

      default:
         PDebugError("PNDEFCheckReadParameters: Wrong TNF 0x%02x", nTNF );
         return W_ERROR_BAD_PARAMETER;
   }

   return W_SUCCESS;
}

/* See Header file */
void PNDEFSendError(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            W_ERROR nError )
{
   /* Store the error code */
   pNDEFConnection->nError = nError;

   if (pNDEFConnection->hCurrentOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pNDEFConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("PNDEFSendError: operation cancelled");
         nError = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pNDEFConnection->hCurrentOperation);
      PHandleClose(pContext, pNDEFConnection->hCurrentOperation);
      pNDEFConnection->hCurrentOperation = W_NULL_HANDLE;
   }

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, pNDEFConnection->hConnection);

   /* Check the command state */
   switch ( pNDEFConnection->nCommandState )
   {
      case P_NDEF_ACTION_READ:
         /* Send the error */
         PDFCPostContext3(
            &pNDEFConnection->sCallbackContext,
            ((nError != W_SUCCESS) ? W_NULL_HANDLE : pNDEFConnection->hMessage),
            nError );

         /* In case of error */
         if ( nError != W_SUCCESS )
         {
            PHandleClose(pContext, pNDEFConnection->hMessage);
            pNDEFConnection->hMessage = W_NULL_HANDLE;
         }
         break;

      case P_NDEF_ACTION_RESET:
         /* Send the error */
         if (nError != W_SUCCESS)
         {
            PDFCPostContext2(
               &pNDEFConnection->sCallbackContext,
               W_ERROR_CONNECTION_COMPATIBILITY );
         }
         else
         {
            PDFCPostContext2(
               &pNDEFConnection->sCallbackContext,
               nError);
         }
         break;

      case P_NDEF_ACTION_WRITE:
         /* Send the error */
         PDFCPostContext2(
            &pNDEFConnection->sCallbackContext,
            nError );

         /* Close the message */
         PHandleClose(pContext, pNDEFConnection->hMessage);
         pNDEFConnection->hMessage = W_NULL_HANDLE;
         break;
   }

   if ( pNDEFConnection->pMessageBuffer != null )
   {
      /* Free the message buffer */
      CMemoryFree( pNDEFConnection->pMessageBuffer );
      pNDEFConnection->pMessageBuffer = null;
   }

   /* Reset the message handle */
   pNDEFConnection->hMessage = W_NULL_HANDLE;

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pNDEFConnection);
}

/** @brief READ NDEF operation automaton
  *
  * @param[in] pContext The context
  *
  * @param[in] pNDEFConnection The current NDEF connection
  *
  * @param[in] pBuffer the buffer that contains the operation result
  *
  * @param[in] nLength the length of the buffer
*/

W_ERROR static_PNDEFReadAutomaton(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t* pBuffer,
            uint32_t nLength)
{
   W_ERROR  nError;
   W_HANDLE hMessage;

   switch (pNDEFConnection->nCommandType)
   {
      case P_NDEF_COMMAND_READ_NDEF :

         PDebugTrace("static_PNDEFReadAutomaton: P_NDEF_COMMAND_READ_NDEF");

         /* The contents of the read NDEF message is stored in pNDEFConnection->pReceivedBuffer,
            copy it in pNDEFConnection->pBuffer */

         CMemoryCopy(pNDEFConnection->pBuffer + pNDEFConnection->nIndex, pBuffer, nLength);
         pNDEFConnection->nIndex += nLength;

         break;

      default :

         return W_ERROR_BAD_STATE;
   }

   /* we've reached the end of the NDEF message, parse it */

   if (pNDEFConnection->nIndex == 0)
   {
      return W_ERROR_ITEM_NOT_FOUND;
   }

   /* Parse the information */
   nError = PNDEFParseFile(
               pContext,
               pNDEFConnection->nTNF,
               pNDEFConnection->aTypeString,
               pNDEFConnection->aIdentifierString,
               pNDEFConnection->pBuffer,
               pNDEFConnection->nIndex,
               &hMessage );

   if (nError == W_SUCCESS)
   {
      /* Set message */
      pNDEFConnection->hMessage = hMessage;

      /* Send result */
      PNDEFSendError(pContext, pNDEFConnection, W_SUCCESS);
   }

   return nError;
}

/** @brief WRITE NDEF operation automaton
  *
  * @param[in] pContext The context
  *
  * @param[in] pNDEFConnection The current NDEF connection
  *
  * @param[in] pBuffer the buffer that contains the operation result
  *
  * @param[in] nLength the length of the buffer
*/

W_ERROR static_PNDEFWriteAutomaton(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t* pBuffer,
            uint32_t nLength)
{
   W_ERROR nError;

   switch (pNDEFConnection->nCommandType)
   {
      case P_NDEF_COMMAND_WRITE_NDEF :

         PDebugTrace("static_PNDEFWriteAutomaton : P_NDEF_COMMAND_WRITE_NDEF");

         /* The write NDEF operation succeeded, write the NDEF length */
         PNDEFSendCommand( pContext, pNDEFConnection, P_NDEF_COMMAND_WRITE_NDEF_LENGTH, 0, 0);
         return W_SUCCESS;


      case P_NDEF_COMMAND_WRITE_NDEF_LENGTH :

         PDebugTrace("static_PNDEFWriteAutomaton : P_NDEF_COMMAND_WRITE_NDEF_LENGTH");

         /* The Write NDEF length operation succeeded */

         if ((pNDEFConnection->nActionMask & W_NDEF_ACTION_BIT_CHECK_WRITE) != 0)
         {
            if (pNDEFConnection->pTagType->pInvalidateCache)
            {
               /* here, need to flush the cache (if any) to ensure data are read from the chip */
               pNDEFConnection->pTagType->pInvalidateCache(pContext, pNDEFConnection, pNDEFConnection->nOffset, pNDEFConnection->nMessageLength);
            }

            /* if a check has been requested, read the TAG contents to check it */
            PNDEFSendCommand( pContext, pNDEFConnection, P_NDEF_COMMAND_READ_NDEF, pNDEFConnection->nOffset, pNDEFConnection->nMessageLength);
            return W_SUCCESS;
         }

         if ((pNDEFConnection->nActionMask & W_NDEF_ACTION_BIT_LOCK) != 0)
         {
            /* If lock has been requested, lock the TAG contents */
            PNDEFSendCommand(pContext, pNDEFConnection, P_NDEF_COMMAND_LOCK_TAG, 0, 0);
            return W_SUCCESS;
         }

         /* ok, the write operation is completed */
         pNDEFConnection->nError = W_SUCCESS;
         PNDEFSendError(pContext, pNDEFConnection, W_SUCCESS );

         break;

      case P_NDEF_COMMAND_READ_NDEF :

         PDebugTrace("static_PNDEFWriteAutomaton : P_NDEF_COMMAND_READ_NDEF");

         /* the read NDEF operation succeeded */

         /* compare the contents of the tag with the expected contents */
         if ((nError = static_PNDEFCheckFile( pContext, pNDEFConnection, pBuffer, nLength)) != W_SUCCESS)
         {
            /* difference found between the tag contents and the expected NDEF message */
            return (nError);
         }

         /* ok, the check passed */
         if ((pNDEFConnection->nActionMask & W_NDEF_ACTION_BIT_LOCK) != 0)
         {
            /* If lock has been requested, lock the TAG contents */
            PNDEFSendCommand(pContext, pNDEFConnection, P_NDEF_COMMAND_LOCK_TAG, 0, 0);
            return W_SUCCESS;
         }

         /* ok, the write operation is completed */
         pNDEFConnection->nError = W_SUCCESS;
         PNDEFSendError(pContext, pNDEFConnection, W_SUCCESS );

         break;

      case P_NDEF_COMMAND_LOCK_TAG :

         PDebugTrace("static_PNDEFWriteAutomaton : P_NDEF_COMMAND_LOCK_TAG");

         /* the lock operation succeeded */

         /* ok, the write operation is completed */
         pNDEFConnection->nError = W_SUCCESS;
         PNDEFSendError(pContext, pNDEFConnection, W_SUCCESS );

         break;

      default :

         PDebugError("static_PNDEFWriteAutomaton : unexpected nCommandType %d", pNDEFConnection->nCommandType);
         return W_ERROR_BAD_STATE;
   }

   return W_SUCCESS;
}

/* See Header file */
W_ERROR PNDEFSendCommandCompleted(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t* pBuffer,
            uint32_t nLength,
            uint32_t nError )
{

   /* Check if the operation has been cancelled by the user */
   if ( pNDEFConnection->nError == W_ERROR_CANCEL )
   {
      PDebugError("PNDEFSendCommandCompleted: W_ERROR_CANCEL");
      return W_ERROR_CANCEL;
   }

   /* Check the parameters */
   if ( pNDEFConnection->hConnection == W_NULL_HANDLE )
   {
      PDebugError("PNDEFSendCommandCompleted: W_ERROR_BAD_HANDLE");
      return W_ERROR_BAD_HANDLE;
   }

   /* Check the error code return */
   if ( nError != W_SUCCESS )
   {
      PDebugError("PNDEFSendCommandCompleted: Error %s", PUtilTraceError(nError) );

      if ( nError == W_ERROR_ITEM_LOCKED )
      {
         nError = W_ERROR_LOCKED_TAG;
      }
      return nError;
   }

   if(pNDEFConnection->nCommandType == P_NDEF_COMMAND_LOCK_TAG)
   {
      /* Update the lock and lockable boolean indicator */
      pNDEFConnection->bIsLockable = W_FALSE;
      pNDEFConnection->bIsLocked = W_TRUE;
   }

   switch (pNDEFConnection->nCommandState)
   {
      case P_NDEF_ACTION_WRITE :
         nError = static_PNDEFWriteAutomaton(pContext, pNDEFConnection, pBuffer, nLength);
         break;

      case P_NDEF_ACTION_READ :
         nError = static_PNDEFReadAutomaton(pContext, pNDEFConnection, pBuffer, nLength);
         break;

      default :
         CDebugAssert(0);
         nError = W_ERROR_BAD_STATE;
         break;
   }

   return nError;
}

/* See Header file */
void PNDEFSendCommand(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t nCommandType,
            uint32_t nOffset,
            uint32_t nLength )
{
   /* uint8_t nCommandLength = 0; */
   W_ERROR nError;
/*    static const uint8_t g_aNDEFEmpty[] = { 0x00, 0x03, 0xD0, 0x00, 0x00 }; */

   /* Store the command type */
   pNDEFConnection->nCommandType = nCommandType;

   CDebugAssert(pNDEFConnection->pTagType->pSendCommand != null);

   /* Send the corresponding command */
   switch ( nCommandType )
   {
      case P_NDEF_COMMAND_READ_NDEF:
      case P_NDEF_COMMAND_WRITE_NDEF :

         pNDEFConnection->nOffset = nOffset;

         CDebugAssert(pNDEFConnection->pTagType->pSendCommand != null);

         nError = pNDEFConnection->pTagType->pSendCommand( pContext, pNDEFConnection, nOffset, nLength );

         if(nError != W_SUCCESS)
         {
            PDebugError("PNDEFSendCommand: error %s", PUtilTraceError(nError) );
            /* Send the error */
            PNDEFSendError( pContext, pNDEFConnection, nError );
         }

         break;

      case P_NDEF_COMMAND_LOCK_TAG :
      case P_NDEF_COMMAND_WRITE_NDEF_LENGTH :

         CDebugAssert(pNDEFConnection->pTagType->pSendCommand != null);

         nError = pNDEFConnection->pTagType->pSendCommand(pContext, pNDEFConnection, 0, 0 );

         if(nError != W_SUCCESS)
         {
            PDebugError("PNDEFSendCommand: error %s", PUtilTraceError(nError) );
            /* Send the error */
            PNDEFSendError( pContext, pNDEFConnection, nError );
         }

         break;
   }
}

static void static_PNDEFReadMessage(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint8_t nTNF,
            const char16_t* pTypeString)
{
   /* Store the connection information */
   pNDEFConnection->nCommandState   = P_NDEF_ACTION_READ;

   /* Reset the buffer addresses */
   pNDEFConnection->pMessageBuffer     = null;

   /* Store the command information */
   pNDEFConnection->nTNF            = nTNF;
   /* Reset the values */
   pNDEFConnection->hMessage        = W_NULL_HANDLE;
   pNDEFConnection->nIndex          = 0;
   pNDEFConnection->nOffset         = 0;
   pNDEFConnection->nMessageLength  = 0;
   /* Get the values */
   switch (nTNF)
   {
      case W_NDEF_TNF_EMPTY:
      case W_NDEF_TNF_UNKNOWN:
      case W_NDEF_TNF_ANY_TYPE:
         pNDEFConnection->aTypeString[0] = '\0';
         break;

      case W_NDEF_TNF_WELL_KNOWN:
      case W_NDEF_TNF_MEDIA:
      case W_NDEF_TNF_ABSOLUTE_URI:
      case W_NDEF_TNF_EXTERNAL:
         CMemoryCopy(
            pNDEFConnection->aTypeString,
            pTypeString,
            PUtilStringLength(pTypeString) * 2);

         pNDEFConnection->aTypeString[PUtilStringLength(pTypeString)] = '\0';
         pNDEFConnection->aIdentifierString[0] = '\0';
         break;
   }

   /* Read the NDEF file */
   PNDEFSendCommand(
      pContext,
      pNDEFConnection,
      P_NDEF_COMMAND_READ_NDEF,
      0x00,
      pNDEFConnection->nNDEFFileLength);
}

/* See Client API Specifications */
void PNDEFReadMessage(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericHandleCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nTNF,
            const char16_t* pTypeString,
            W_HANDLE* phOperation )
{
   tNDEFConnection* pNDEFConnection;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;
   W_ERROR nError;

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Check if the connection handle is valid */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF_CONNECTION, (void**)&pNDEFConnection);
   if (pNDEFConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      W_HANDLE hMessage = W_NULL_HANDLE;

      /* Check if it is a Tag Simulation */
      nError = PVirtualTagReadMessage(
               pContext,
               hConnection,
               &hMessage,
               nTNF,
               pTypeString);

      PDFCPostContext3(
         &sCallbackContext,
         hMessage,
         nError );

      return;
   }

   /* Check the read parameters */
   if ( ( nError = PNDEFCheckReadParameters(
                     pContext,
                     nTNF,
                     pTypeString ) ) != W_SUCCESS )
   {
      PDebugError("PNDEFReadMessage: PNDEFCheckReadParameters error");
      goto return_error;
   }

   /* Check the NDEF file size */
   if (pNDEFConnection->nNDEFFileLength < 3)
   {
      PDebugError("PNDEFReadMessage: W_ERROR_ITEM_NOT_FOUND");
      nError = W_ERROR_ITEM_NOT_FOUND;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PNDEFReadMessage: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PNDEFReadMessage: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nError = PReaderNotifyExchange(pContext, hConnection, static_PNDEFExecuteQueuedExchange, pNDEFConnection);

   switch (nError)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the NDEFSendError when the operation is completed */
      PHandleIncrementReferenceCount(pNDEFConnection);

      /* Store the operation handle */
      CDebugAssert(pNDEFConnection->hCurrentOperation == W_NULL_HANDLE);
      pNDEFConnection->hCurrentOperation = hCurrentOperation;

      /* Store the callback context */
      pNDEFConnection->sCallbackContext = sCallbackContext;

      /* Read */
      static_PNDEFReadMessage(pContext, pNDEFConnection, nTNF, pTypeString);

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the NDEFSendError when the operation is completed */
      PHandleIncrementReferenceCount(pNDEFConnection);

      /* Save the operation handle */
      CDebugAssert(pNDEFConnection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
      pNDEFConnection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

      /* Save callback context */
      pNDEFConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

      /* Save type of operation */
      pNDEFConnection->sQueuedOperation.nType = P_NDEF_QUEUED_READ;

      /* Save data */
      pNDEFConnection->sQueuedOperation.nTNF = nTNF;
      CMemoryCopy(
         pNDEFConnection->sQueuedOperation.aTypeString,
         pTypeString,
         PUtilStringLength(pTypeString) * 2);

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
   PDebugError("PNDEFReadMessage: return %s", PUtilTraceError(nError));

   PDFCPostContext3(&sCallbackContext, W_NULL_HANDLE, nError);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
W_ERROR PNDEFGetTagInfo(
            tContext* pContext,
            W_HANDLE hConnection,
            tNDEFTagInfo* pTagInfo )
{
   tNDEFConnection* pNDEFConnection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF_CONNECTION, (void**)&pNDEFConnection);

   /* Check the parameters */
   if ( pTagInfo == null )
   {
      PDebugError("PNDEFGetTagInfo: pTagInfo == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Fill in the buffer with zeros */
   CMemoryFill(pTagInfo, 0, sizeof(tNDEFTagInfo));

   if ( nError == W_SUCCESS )
   {
      uint32_t nSerialNumberLength;

      /* Store the information on the provided connection */
      pTagInfo->nTagType            = pNDEFConnection->pTagType->nProperty;
      pTagInfo->bIsLocked           = pNDEFConnection->bIsLocked;
      pTagInfo->bIsLockable         = pNDEFConnection->bIsLockable;
      pTagInfo->nFreeSpaceSize      = pNDEFConnection->nFreeSpaceSize;

      nError = PHandleGetIdentifier(
         pContext, hConnection,
         pTagInfo->aSerialNumber,
         sizeof(pTagInfo->aSerialNumber),
         &nSerialNumberLength);

      pTagInfo->nSerialNumberLength = (uint8_t)nSerialNumberLength;
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR WNDEFReadMessageSync(
            W_HANDLE hConnection,
            uint8_t nTNF,
            const char16_t* pTypeString,
            W_HANDLE* phMessage)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WNDEFReadMessage(
         hConnection,
         PBasicGenericSyncCompletionHandle, &param,
         nTNF,
         pTypeString,
         null );
   }

   return PBasicGenericSyncWaitForResultHandle(&param, phMessage);
}

const uint8_t g_SupportedNonFormattedTags[] =
{
   /* TAG type 1  */
   W_PROP_TYPE1_CHIP,      /* Innovision topaz */

   /* TAG type 2 */
   W_PROP_MIFARE_UL_C,     /* NXP MIFARE Ultra Light C */
   W_PROP_MIFARE_UL,       /* NXP MIFARE Ultra Light */
   W_PROP_MY_D_MOVE,       /* Infineon My-D Move */
   W_PROP_MY_D_NFC,        /* Infineon My-D NFC */
   W_PROP_KOVIO_RFID,

#ifdef P_INCLUDE_PICOPASS
   /* TAG type 5 */
   W_PROP_PICOPASS_2K,     /* Inside Secure PicoPass */
   W_PROP_PICOPASS_32K,
#endif /* P_INCLUDE_PICOPASS */

   /* TAG type 6 */
   W_PROP_TI_TAGIT,
   W_PROP_NXP_ICODE,
   W_PROP_ST_LRI_512,

   /* TAG TYpe 7 */
   W_PROP_MIFARE_1K,       /* NXP mifare 1K */
   W_PROP_MIFARE_4K,       /* NXP mifare 4K */

};


/* NDEF Write message completed callback */
static void static_PNDEFFormatWriteMessageCompleted(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tNDEFOperation * pNDEFOperation = (tNDEFOperation *) pCallbackParameter;

   PDFCPostContext2(& pNDEFOperation->sCallbackContext, nError);

   /* Free the no longer needed NDEF operation */
   CMemoryFree(pNDEFOperation);
}

/* NDEF creation completed */
static void static_PNDEFFormatConnectionCreated(
            tContext * pContext,
            void     * pCallbackParameter,
            W_ERROR    nError)
{
   tNDEFOperation * pNDEFOperation = (tNDEFOperation *) pCallbackParameter;

   PDebugTrace("static_PNDEFFormatConnectionCreated");

   if (nError == W_SUCCESS)
   {
      /* the connection has been upgraded to NDEF, we can now perform the NDEF operation pending */

      PNDEFWriteMessage(pContext, pNDEFOperation->hConnection, static_PNDEFFormatWriteMessageCompleted, pNDEFOperation, pNDEFOperation->hMessage, pNDEFOperation->nActionMask, null);
   }
   else
   {
      PDebugError("static_PNDEFFormatConnectionCreated : nError %d", nError);

      PDFCPostContext2(& pNDEFOperation->sCallbackContext, nError);

      /* Free the no longer needed NDEF operation */
      CMemoryFree(pNDEFOperation);
   }
}

/* TAG format completed */
static void static_PNDEFFormatCompleted(
            tContext * pContext,
            void     * pCallbackParameter,
            W_ERROR    nError)
{
   tNDEFOperation* pNDEFOperation = (tNDEFOperation*) pCallbackParameter;
   tHandleObjectHeader* pObjectHeader = null;

   PDebugTrace("static_PNDEFFormatCompleted %d", nError);

   if (pNDEFOperation->hOperation != W_NULL_HANDLE)
   {
      /* RFU: Check operation status */
      /*if ( (nError == W_SUCCESS) && (PBasicGetOperationState(pContext, pNDEFOperation->hOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PNDEFFormatCompleted: operation cancelled");
         nError = W_ERROR_CANCEL;
      }*/

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pNDEFOperation->hOperation);
      PHandleClose(pContext, pNDEFOperation->hOperation);
      pNDEFOperation->hOperation = W_NULL_HANDLE;
   }

   /* Manage user connection status and polling */
   PReaderNotifyExchangeCompletion(pContext, pNDEFOperation->hConnection);

   /* Retrieve the user connection object */
   nError = PHandleGetObject(pContext, pNDEFOperation->hConnection, P_HANDLE_TYPE_ANY, (void**)&pObjectHeader);
   if (pObjectHeader == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }

   if (nError == W_SUCCESS)
   {
      /* If the NDEF connection was already existing, we need to destroy it and to recreate it to allow proper update of the connection infos */
      if (PBasicCheckConnectionProperty(pContext, pNDEFOperation->hConnection, pNDEFOperation->nNDEFType) == W_SUCCESS)
      {
         PNDEFRemoveConnection(pContext, pNDEFOperation->hConnection);
      }

      /* create the NDEF connection */
      PNDEFCreateConnection(pContext, pNDEFOperation->hConnection, static_PNDEFFormatConnectionCreated, pNDEFOperation, pNDEFOperation->nNDEFType);

      /* update pObjectHeader because it is no longer valid but its reference count must be decremented */
      nError = PHandleGetObject(pContext, pNDEFOperation->hConnection, P_HANDLE_TYPE_ANY, (void**)&pObjectHeader);

      if (pObjectHeader == null)
      {
         PDebugError("static_PNDEFFormatCompleted : unexpected error. Trouble ahead");
      }

   }
   else
   {
      PDebugError("static_PNDEFFormatCompleted : nlError %d", nError);

      PDFCPostContext2(& pNDEFOperation->sCallbackContext, nError);

      /* Free the no longer needed NDEF operation */
      CMemoryFree(pNDEFOperation);
   }

   if (pObjectHeader != null)
   {
      /* Decrement the reference count of the connection. This may destroy the object */
      PHandleDecrementReferenceCount(pContext, pObjectHeader);
   }
}

static void static_PNDEFWrite(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nActionMask,
            W_HANDLE hMessage,
            uint8_t* pMessageBuffer,
            uint32_t nOffset,
            uint32_t nLength,
            uint32_t nActualLength,
            uint32_t nUpdatedNDEFFileLength)
{
   /* store the updated message length for later processing */
   pNDEFConnection->nUpdatedNDEFFileLength = nUpdatedNDEFFileLength;

   /* Store the connection information */
   pNDEFConnection->nCommandState   = P_NDEF_ACTION_WRITE;
   pNDEFConnection->hMessage        = hMessage;
   pNDEFConnection->nMessageLength  = nActualLength;
   pNDEFConnection->nActionMask     = nActionMask & ~0x03;
   pNDEFConnection->pMessageBuffer  = pMessageBuffer;

   /* Perform the write operation */
   PNDEFSendCommand(pContext, pNDEFConnection, P_NDEF_COMMAND_WRITE_NDEF, nOffset, nLength);
}

static void static_PNDEFLock(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection)
{
   /* Store the connection information */
   pNDEFConnection->nCommandState   = P_NDEF_ACTION_WRITE;

   /* Reset the buffer addresses */
   pNDEFConnection->hMessage        = W_NULL_HANDLE;
   pNDEFConnection->pMessageBuffer  = null;
   pNDEFConnection->nMessageLength  = 0;

   /* Perform the lock operation */
   PNDEFSendCommand(pContext, pNDEFConnection, P_NDEF_COMMAND_LOCK_TAG, 0, 0);
}

static void static_PNDEFFormat(
            tContext* pContext,
            W_HANDLE hConnection,
            tNDEFOperation* pNDEFOperation)
{
   /* start format operation */
   switch (pNDEFOperation->nTagType)
   {
      /* Type 1 TAG */
      case W_PROP_TYPE1_CHIP :

         pNDEFOperation->nNDEFType = W_PROP_NFC_TAG_TYPE_1;
         PNDEFFormatNDEFType1(pContext, hConnection, static_PNDEFFormatCompleted, pNDEFOperation, pNDEFOperation->nTagType);
         break;

      /* Type 2 TAG */
      case W_PROP_MIFARE_UL :
      case W_PROP_MIFARE_UL_C :
      case W_PROP_MY_D_MOVE :
      case W_PROP_MY_D_NFC :
      case W_PROP_KOVIO_RFID :

         pNDEFOperation->nNDEFType = W_PROP_NFC_TAG_TYPE_2;
         PNDEFFormatNDEFType2(pContext, hConnection, static_PNDEFFormatCompleted, pNDEFOperation, pNDEFOperation->nTagType);
         break;

#ifdef P_INCLUDE_PICOPASS
      /* Type 5 TAG */
      case W_PROP_PICOPASS_2K :
      case W_PROP_PICOPASS_32K :

         pNDEFOperation->nNDEFType = W_PROP_NFC_TAG_TYPE_5;
         PNDEFFormatNDEFType5(pContext, hConnection, static_PNDEFFormatCompleted, pNDEFOperation, pNDEFOperation->nTagType);
         break;
#endif /* P_INCLUDE_PICOPASS */

      /* Type 6 TAG */
      case W_PROP_NXP_ICODE :
      case W_PROP_ST_LRI_512 :
      case W_PROP_TI_TAGIT :

         pNDEFOperation->nNDEFType = W_PROP_NFC_TAG_TYPE_6;
         PNDEFFormatNDEFType6(pContext, hConnection, static_PNDEFFormatCompleted, pNDEFOperation, pNDEFOperation->nTagType, pNDEFOperation->nTagSize);
         break;

#ifdef P_INCLUDE_MIFARE_CLASSIC
      /* Type 7 TAG */
      case W_PROP_MIFARE_1K:
      case W_PROP_MIFARE_4K:
         pNDEFOperation->nNDEFType = W_PROP_NFC_TAG_TYPE_7;
         PNDEFFormatNDEFType7(pContext, hConnection, static_PNDEFFormatCompleted, pNDEFOperation, pNDEFOperation->nTagType);
         break;
#endif /* P_INCLUDE_MIFARE_CLASSIC */

      default :
         /* should not occur */
         CDebugAssert(0);
         break;
   }
}

#define P_NDEF_WRITE_COMMAND_NONE   0
#define P_NDEF_WRITE_COMMAND_WRITE  1
#define P_NDEF_WRITE_COMMAND_LOCK   2
#define P_NDEF_WRITE_COMMAND_FORMAT 3

/* See Client API Specifications */
void PNDEFWriteMessage(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            W_HANDLE hMessage,
            uint32_t nActionMask,
            W_HANDLE* phOperation )
{
   tDFCCallbackContext sCallbackContext;
   tNDEFConnection* pNDEFConnection;
   W_HANDLE hCurrentOperation = W_NULL_HANDLE;
   W_ERROR nError;

   uint8_t nTagType = 0;
   uint32_t nTagSize = 0;
   tNDEFOperation* pNDEFOperation = null;

   W_HANDLE hLocalMessage = W_NULL_HANDLE;
   uint8_t* pMessageBuffer = null;
   uint32_t nOffset = 0;
   uint32_t nMessageSize = 0;
   uint32_t nTrailerSize = 0;
   uint32_t nRequestedSize = 0;
   uint32_t nActualLength = 0;

   uint32_t nCommandType = P_NDEF_WRITE_COMMAND_NONE;

   PDebugTrace("PNDEFWriteMessage");

   /* prepare the callback context */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* check the nActionMask value */
   if ((nActionMask & W_NDEF_ACTION_BITMASK) != nActionMask)
   {
      PDebugError("PNDEFWriteMessage : invalid nActionMask value");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* specific case for Virtual tags */
   if (PBasicCheckConnectionProperty(pContext, hConnection, W_PROP_VIRTUAL_TAG) == W_SUCCESS)
   {
      nError = PVirtualTagWriteMessage(pContext, hConnection, hMessage, nActionMask);

      PDFCPostContext2(&sCallbackContext, nError);
      return;
   }

   /* Generic case */
   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_NDEF_CONNECTION, (void**)&pNDEFConnection);
   /* check the connection handle. W_ERROR_CONNECTION_COMPATIBILITY is managed latter when formatting */
   if (nError == W_ERROR_BAD_HANDLE)
   {
      PDebugError("PNDEFWriteMessage: bad handle");
      goto return_error;
   }

   if ( ((nError == W_SUCCESS) && ((nActionMask & W_NDEF_ACTION_FORMAT_BITMASK) == W_NDEF_ACTION_BIT_FORMAT_ALL)) ||
        ((nError != W_SUCCESS) && ((nActionMask & W_NDEF_ACTION_FORMAT_BITMASK) != 0)))
   {
      /* A format operation has been requested...
       *
       * Check if we are able to format this kind of tag
       */
      uint32_t i;
      for (i = 0; i < sizeof(g_SupportedNonFormattedTags); i++)
      {
         nError = PBasicCheckConnectionProperty(pContext, hConnection, g_SupportedNonFormattedTags[i]);

         if (nError == W_ERROR_BAD_HANDLE)
         {
            PDebugError("PNDEFWriteMessage : PBasicCheckConnectionProperty returned W_ERROR_BAD_HANDLE");
            goto return_error;
         }
         else if (nError == W_SUCCESS)
         {
            nTagType = g_SupportedNonFormattedTags[i];
            break;
         }
      }

#ifdef P_INCLUDE_MIFARE_CLASSIC
      /* Special case for already NDEF Mifare Type 7 */
      /* We don't support the formatting of this tag */
      if(PBasicCheckConnectionProperty(pContext, hConnection, W_PROP_NFC_TAG_TYPE_7) == W_SUCCESS)
      {
         nActionMask &= ~W_NDEF_ACTION_FORMAT_BITMASK;
         nActionMask |= W_NDEF_ACTION_BIT_ERASE;
      }
#endif /* P_INCLUDE_MIFARE_CLASSIC */

      if (nError != W_SUCCESS)
      {
         /* We don't support formatting of this tag...
          * If it is already formatted, simply ignore the format request and replace it by a erase request
          */
         if (pNDEFConnection != null)
         {
            nActionMask &= ~W_NDEF_ACTION_FORMAT_BITMASK;
            nActionMask |= W_NDEF_ACTION_BIT_ERASE;
         }
         else
         {
            PDebugError("PNDEFWriteMessage : formatting of this tag is not supported and this tag is not already formatted.");
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
            goto return_error;
         }
      }
   }

   /*
    *  Depending of the type of the connection and the request :
    *  - directly write into the tag
    *  - or format it before...
    */
   if ((pNDEFConnection != null) && ((nActionMask & W_NDEF_ACTION_FORMAT_BITMASK) != W_NDEF_ACTION_BIT_FORMAT_ALL))
   {
      /* We already have a NDEF connection, and unconditionnal format has not been resquested,
       * perform the write operation...
       */

      /* Check the connection locked flag */
      if (pNDEFConnection->bIsLocked != W_FALSE)
      {
         PDebugError("PNDEFWriteMessage: W_ERROR_LOCKED_TAG");
         nError = W_ERROR_LOCKED_TAG;
         goto return_error;
      }

      /* Check the command lock flag and the connection lockable flag */
      if ( ((nActionMask & W_NDEF_ACTION_BIT_LOCK) != W_FALSE) && (pNDEFConnection->bIsLockable == W_FALSE) )
      {
         PDebugError("PNDEFWriteMessage: W_ERROR_TAG_NOT_LOCKABLE");
         nError = W_ERROR_TAG_NOT_LOCKABLE;
         goto return_error;
      }

      if (((nActionMask & W_NDEF_ACTION_BIT_ERASE) != 0) || (hMessage != W_NULL_HANDLE))
      {
         /* A write action is needed */

         /* if ERASE action has been requested,
            the requested size does not include the current NDEF message length,
            otherwise, the requested size include the current NDEF message length */
         nOffset = ((nActionMask & W_NDEF_ACTION_BIT_ERASE) == 0) ? pNDEFConnection->nNDEFFileLength : 0;

         /* get the message size */
         if (hMessage != W_NULL_HANDLE)
         {
            nMessageSize = PNDEFGetMessageLength( pContext, hMessage);
            if (nMessageSize == 0)
            {
               PDebugError("PNDEFWriteMessage: unknown hMessage");
               nError = W_ERROR_BAD_HANDLE;
               goto return_error;
            }
         }
         else
         {
            nMessageSize = 3;     /* empty NDEF message: D0 00 00 */
         }

         /* Compute the requested tag's size */
         nRequestedSize = nOffset + nMessageSize;

         if (nRequestedSize > pNDEFConnection->nMaximumSpaceSize)
         {
            PDebugError("PNDEFWriteMessage : no room to store the message");
            nError = W_ERROR_TAG_FULL;
            goto return_error;
         }

         /* Compute the trailer size */
         switch (pNDEFConnection->pTagType->nProperty)
         {
            case W_PROP_NFC_TAG_TYPE_1 :
            case W_PROP_NFC_TAG_TYPE_2 :
               /* For theses tags, if the NDEF area is not full, we must append a terminator
                  TLV at the end of the message */
               if (nRequestedSize < pNDEFConnection->nMaximumSpaceSize)
               {
                  nTrailerSize = 1;
               }
               else
               {
                  nTrailerSize = 0;
               }

               break;

            case W_PROP_NFC_TAG_TYPE_3 :

               nTrailerSize = 15;
               break;

            case W_PROP_NFC_TAG_TYPE_4_A :
            case W_PROP_NFC_TAG_TYPE_4_B :
            case W_PROP_NFC_TAG_TYPE_5 :

               nTrailerSize = 0;
               break;

            case W_PROP_NFC_TAG_TYPE_6 :
               /* For ICODE formatted tags, if the NDEF area is not full, we must append a terminator
                  TLV at the end of the message */
               if (pNDEFConnection->sType.t6.bICodeFormat != W_FALSE && nRequestedSize < pNDEFConnection->nMaximumSpaceSize)
               {
                  nTrailerSize = 1;
               }
               else
               {
                  nTrailerSize = 0;
               }
               break;

#ifdef P_INCLUDE_MIFARE_CLASSIC
            case W_PROP_NFC_TAG_TYPE_7 :
               nTrailerSize = 0;
               break;
#endif /* P_INCLUDE_MIFARE_CLASSIC */

            default:
               CDebugAssert(0);
               break;
         }

         /* Allocate message buffer */
         pMessageBuffer = (uint8_t *) CMemoryAlloc(nMessageSize + nTrailerSize);
         if (pMessageBuffer == null)
         {
            PDebugError("PNDEFWriteMessage : CMemoryAlloc() failed");
            nError = W_ERROR_OUT_OF_RESOURCE;
            goto return_error;
         }

         /* Build message buffer */
         if (hMessage != W_NULL_HANDLE)
         {
            nError = PNDEFGetMessageContent(pContext, hMessage, pMessageBuffer, nMessageSize, &nActualLength );
            if (nError != W_SUCCESS )
            {
               PDebugError("PNDEFWriteMessage: could not retrieve message contents");
               goto return_error;
            }
         }
         else
         {
            /* empty NDEF message */
            pMessageBuffer[0] = 0xD0;
            pMessageBuffer[1] = 0x00;
            pMessageBuffer[2] = 0x00;
            nActualLength = 3;
         }

         if (nTrailerSize != 0)
         {
            /* Add trailer to message buffer */
            switch (pNDEFConnection->pTagType->nProperty)
            {
               case W_PROP_NFC_TAG_TYPE_1 :

                  pMessageBuffer[nActualLength] = P_NDEF_1_TLV_TERMINATOR;
                  break;

               case W_PROP_NFC_TAG_TYPE_2 :
                  pMessageBuffer[nActualLength] = P_NDEF_2_TLV_TERMINATOR;
                  break;

               case W_PROP_NFC_TAG_TYPE_3 :
                  CMemoryFill(&pMessageBuffer[nActualLength], 0xFF, nTrailerSize);
                  nTrailerSize = 0;
                  break;

               case W_PROP_NFC_TAG_TYPE_6 :
                  pMessageBuffer[nActualLength] = P_NDEF_6_TLV_TERMINATOR;
                  break;
            }
         }

         /* Duplicate the message handle */
         if ( ( nError = PHandleDuplicate(pContext, hMessage, &hLocalMessage ) ) != W_SUCCESS )
         {
            PDebugError("PNDEFWriteMessage : PHandleDuplicate failed");
            goto return_error;
         }

         nCommandType = P_NDEF_WRITE_COMMAND_WRITE;
      }
      else
      {
         /* no write operation */

         if ((nActionMask & W_NDEF_ACTION_BIT_LOCK) == 0)
         {
            PDebugError("PNDEFWriteMessage : No write / no lock operation");
            nError = W_SUCCESS;
            goto return_error;
         }

         nCommandType = P_NDEF_WRITE_COMMAND_LOCK;
      }
   }
   else
   {
      /*
       * We have to format the TAG....
       */
      uint8_t  nSectorSize;
      bool_t     bIsLocked = W_FALSE;
      bool_t     bIsLockable = W_FALSE;
      bool_t     bIsFormattable = W_FALSE;

      switch (nTagType)
      {
         /* Type 1 TAG */
         case W_PROP_TYPE1_CHIP :

            nError = PType1ChipUserCheckType1(pContext, hConnection, &nTagSize, &nSectorSize, &bIsLocked, &bIsLockable, &bIsFormattable);
            break;

         /* Type 2 TAG */
         case W_PROP_MIFARE_UL :
         case W_PROP_MIFARE_UL_C :

            nError = PMifareCheckType2(pContext, hConnection, &nTagSize, &nSectorSize, &bIsLocked, &bIsLockable, &bIsFormattable);
            break;

         case W_PROP_MY_D_MOVE :
         case W_PROP_MY_D_NFC :

            nError = PMyDCheckType2(pContext, hConnection, &nTagSize, &nSectorSize, &bIsLocked, &bIsLockable, &bIsFormattable);
            break;

         case W_PROP_KOVIO_RFID :
            nError = PKovioRFIDCheckType2(pContext, hConnection, &nTagSize, &nSectorSize, &bIsLocked, &bIsLockable, &bIsFormattable);
            break;

#ifdef P_INCLUDE_PICOPASS
         /* Type 5 TAG */
         case W_PROP_PICOPASS_2K :
         case W_PROP_PICOPASS_32K :

            nError = PPicoCheckType5(pContext, hConnection, &nTagSize, &bIsLocked, &bIsLockable, &bIsFormattable);
            break;
#endif /* P_INCLUDE_PICOPASS */

         /* Type 6 TAG */
         case W_PROP_NXP_ICODE :
         case W_PROP_ST_LRI_512 :
         case W_PROP_TI_TAGIT :

            nError = P15P3UserCheckType6(pContext, hConnection, &nTagSize, &bIsLocked, &bIsLockable, &bIsFormattable);
            break;

#ifdef P_INCLUDE_MIFARE_CLASSIC
         case W_PROP_MIFARE_1K:
         case W_PROP_MIFARE_4K:

            nError = PMifareClassicCheckType7(pContext, hConnection, &nTagSize, &bIsLocked, &bIsLockable, &bIsFormattable);
            break;
#endif /* P_INCLUDE_MIFARE_CLASSIC */
         default :
            /* should not occur */
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
            break;
      }

      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFWriteMessage : check type failed");
         goto return_error;
      }

      if (bIsFormattable == W_FALSE)
      {
         nError = (bIsLocked != W_FALSE) ? W_ERROR_LOCKED_TAG : W_ERROR_CONNECTION_COMPATIBILITY;
         PDebugError("PNDEFWriteMessage : Unable to format this tag : %d", nError);
         goto return_error;
      }

      /* check we can also perform the write operation if any */
      if (hMessage != W_NULL_HANDLE)
      {
         uint32_t nLength = PNDEFGetMessageLength( pContext, hMessage);

         if (nLength == 0)
         {
            PDebugError("PNDEFWriteMessage: unknown hMessage");
            nError = W_ERROR_BAD_HANDLE;
            goto return_error;
         }

         if (nLength > nTagSize)       /* @todo : compresssion ? */
         {
            PDebugError("PNDEFWriteMessage : NDEF message too long");
            nError = W_ERROR_TAG_FULL;
            goto return_error;
         }
      }

      /* check if we can perform the lock operation if requested */
      if ((nActionMask & W_NDEF_ACTION_BIT_LOCK) && (bIsLockable == W_FALSE))
      {
         PDebugError("PNDEFWriteMessage : unable to lock");
         nError = W_ERROR_TAG_NOT_LOCKABLE;
         goto return_error;
      }

      /* allocate a NDEF operation to store the current operation parameters */
      pNDEFOperation = (tNDEFOperation *) CMemoryAlloc(sizeof(tNDEFOperation));
      if (pNDEFOperation == null)
      {
         PDebugError("PNDEFWriteMessage : can not allocate operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Build the NDEF operation */
      pNDEFOperation->hConnection = hConnection;
      pNDEFOperation->hMessage = hMessage;
      pNDEFOperation->nActionMask = nActionMask & ~W_NDEF_ACTION_BIT_FORMAT_ALL;
      pNDEFOperation->nTagType = nTagType;
      pNDEFOperation->nTagSize = nTagSize;

      nCommandType = P_NDEF_WRITE_COMMAND_FORMAT;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PNDEFWriteMessage: Cannot allocate the operation");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nError = PHandleDuplicate(pContext, *phOperation, &hCurrentOperation);
      if(nError != W_SUCCESS)
      {
         PDebugError("PNDEFWriteMessage: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   if (nCommandType != P_NDEF_WRITE_COMMAND_FORMAT)
   {
      nError = PReaderNotifyExchange(pContext, hConnection, static_PNDEFExecuteQueuedExchange, pNDEFConnection);

      switch (nError)
      {
      case W_SUCCESS:
         /* Continue this operation */

         /* Increment the ref count to avoid prematurely freeing during the operation
            The ref count will be decremented in the NDEFSendError when the operation is completed */
         PHandleIncrementReferenceCount(pNDEFConnection);

         /* Store the operation handle */
         CDebugAssert(pNDEFConnection->hCurrentOperation == W_NULL_HANDLE);
         pNDEFConnection->hCurrentOperation = hCurrentOperation;

         /* Store the callback context */
         pNDEFConnection->sCallbackContext = sCallbackContext;

         /* Write or lock */
         switch (nCommandType)
         {
         case P_NDEF_WRITE_COMMAND_WRITE:
            /* Write */
            static_PNDEFWrite(pContext,
                              pNDEFConnection,
                              nActionMask,
                              hLocalMessage,
                              pMessageBuffer,
                              nOffset,
                              nMessageSize + nTrailerSize,
                              nActualLength,
                              nRequestedSize);
            break;

         case P_NDEF_WRITE_COMMAND_LOCK:
            /* Lock */
            static_PNDEFLock(pContext,
                             pNDEFConnection);
            break;
         }

         return;

      case W_ERROR_OPERATION_PENDING:
         /* A polling is pending. Save data to execute this operation after the polling completion. */

         /* Increment the ref count to avoid prematurely freeing during the operation
            The ref count will be decremented in the NDEFSendError when the operation is completed */
         PHandleIncrementReferenceCount(pNDEFConnection);

         /* Save the operation handle */
         CDebugAssert(pNDEFConnection->sQueuedOperation.hCurrentOperation == W_NULL_HANDLE);
         pNDEFConnection->sQueuedOperation.hCurrentOperation = hCurrentOperation;

         /* Save callback context */
         pNDEFConnection->sQueuedOperation.sCallbackContext = sCallbackContext;

         /* Save data */
         switch (nCommandType)
         {
         case P_NDEF_WRITE_COMMAND_WRITE:
            pNDEFConnection->sQueuedOperation.nType = P_NDEF_QUEUED_WRITE;

            pNDEFConnection->sQueuedOperation.nActionMask = nActionMask;
            pNDEFConnection->sQueuedOperation.hMessage = hLocalMessage;
            pNDEFConnection->sQueuedOperation.pMessageBuffer = pMessageBuffer;
            pNDEFConnection->sQueuedOperation.nOffset = nOffset;
            pNDEFConnection->sQueuedOperation.nLength = nMessageSize + nTrailerSize;
            pNDEFConnection->sQueuedOperation.nActualLength = nActualLength;
            pNDEFConnection->sQueuedOperation.nUpdatedNDEFFileLength = nRequestedSize;
            break;

         case P_NDEF_WRITE_COMMAND_LOCK:
            pNDEFConnection->sQueuedOperation.nType = P_NDEF_QUEUED_LOCK;

            break;
         }

         return;

      default:
         /* Return this error */
         if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
         {
            PHandleClose(pContext, *phOperation);
         }
         goto return_error;
      }
   }
   else
   {
      tHandleObjectHeader* pObjectHeader = null;

      /* In case of formatting, pNDEFConnection is null */
      /* Data is stored in pNDEFOperation */

      nError = PReaderNotifyExchange(pContext, hConnection, static_PNDEFExecuteQueuedFormat, pNDEFOperation);

      switch (nError)
      {
      case W_SUCCESS:
      case W_ERROR_OPERATION_PENDING:

         /* Retrieve the user connection object */
         if ((PHandleGetObject(pContext, hConnection, P_HANDLE_TYPE_ANY, (void**)&pObjectHeader) != W_SUCCESS) || (pObjectHeader == null))
         {
            PDebugError("PNDEFWriteMessage: could not get pObjectHeader");
            if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
            {
               PHandleClose(pContext, *phOperation);
            }
            /* Return this error */
            nError = W_ERROR_BAD_HANDLE;
            goto return_error;
         }

         /* Increment the ref count to avoid prematurely freeing during the operation
            The ref count will be decremented in the static_PNDEFFormatCompleted when the operation is completed */
         PHandleIncrementReferenceCount(pObjectHeader);

         /* Store the operation handle */
         pNDEFOperation->hOperation = hCurrentOperation;

         /* Store the callback context */
         pNDEFOperation->sCallbackContext = sCallbackContext;

         if (nError != W_SUCCESS)
         {
            /* A polling is pending. Data is saved in pNDEFOperation to execute this operation after the polling completion. */
            break;
         }

         /* Format */
         static_PNDEFFormat(pContext,
                            hConnection,
                            pNDEFOperation);

         return;

      default:
         /* Return this error */
         if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
         {
            PHandleClose(pContext, *phOperation);
         }
         goto return_error;
      }
   }

return_error:
   PDebugError("PNDEFWriteMessage: return %s", PUtilTraceError(nError));

   PDFCPostContext2(&sCallbackContext, nError);

   if (pNDEFOperation != null)
   {
      CMemoryFree(pNDEFOperation);
   }

   if(hLocalMessage != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hLocalMessage);
   }

   if(hCurrentOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hCurrentOperation);
   }

   CMemoryFree(pMessageBuffer);

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}


/* See Client API Specifications */
W_ERROR WNDEFWriteMessageSync(
            W_HANDLE hConnection,
            W_HANDLE hMessage,
            uint32_t nActionMask)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WNDEFWriteMessage(
         hConnection,
         PBasicGenericSyncCompletion, &param,
         hMessage,
         nActionMask,
         null );
   }

   return PBasicGenericSyncWaitForResult(&param);
}


/* Execute the queued operation (after polling) */
static void static_PNDEFExecuteQueuedExchange(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection*) pObject;

   PDebugTrace("static_PNDEFExecuteQueuedExchange");

   /* Restore operation handle */
   pNDEFConnection->hCurrentOperation = pNDEFConnection->sQueuedOperation.hCurrentOperation;
   /* Restore callback context */
   pNDEFConnection->sCallbackContext = pNDEFConnection->sQueuedOperation.sCallbackContext;

   /* Check operation status */
   if ( (pNDEFConnection->hCurrentOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pNDEFConnection->hCurrentOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PNDEFExecuteQueuedExchange: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* Initialize command State for calling the right callback in PNDEFSendError */
      switch (pNDEFConnection->sQueuedOperation.nType)
      {
      case P_NDEF_QUEUED_READ:
         /* Read */
         /* Store the connection information */
         pNDEFConnection->nCommandState   = P_NDEF_ACTION_READ;


         break;

      case P_NDEF_QUEUED_WRITE:
      case P_NDEF_QUEUED_LOCK:
         pNDEFConnection->nCommandState   = P_NDEF_ACTION_WRITE;
         break;

      default:
         /* Return an error */
         PDebugError("static_PNDEFExecuteQueuedExchange: unknown type of operation!");
         nResult = W_ERROR_BAD_STATE;
      }

      /* If an error has been detected during the polling, return directly */
      PNDEFSendError(pContext, pNDEFConnection, nResult);
   }
   else
   {
      switch (pNDEFConnection->sQueuedOperation.nType)
      {
      case P_NDEF_QUEUED_READ:
         /* Read */
         static_PNDEFReadMessage(pContext, pNDEFConnection, pNDEFConnection->sQueuedOperation.nTNF, pNDEFConnection->sQueuedOperation.aTypeString);

         break;

      case P_NDEF_QUEUED_WRITE:
         /* Write */
         static_PNDEFWrite(pContext,
                           pNDEFConnection,
                           pNDEFConnection->sQueuedOperation.nActionMask,
                           pNDEFConnection->sQueuedOperation.hMessage,
                           pNDEFConnection->sQueuedOperation.pMessageBuffer,
                           pNDEFConnection->sQueuedOperation.nOffset,
                           pNDEFConnection->sQueuedOperation.nLength,
                           pNDEFConnection->sQueuedOperation.nActualLength,
                           pNDEFConnection->sQueuedOperation.nUpdatedNDEFFileLength);

         break;

      case P_NDEF_QUEUED_LOCK:
         /* Lock */
         static_PNDEFLock(pContext,
                          pNDEFConnection);

         break;

      default:
         /* Return an error */
         PDebugError("static_PNDEFExecuteQueuedExchange: unknown type of operation!");
         PNDEFSendError(pContext, pNDEFConnection, W_ERROR_BAD_STATE);
      }
   }

   /* Reset data */
   CMemoryFill(&pNDEFConnection->sQueuedOperation, 0, sizeof(pNDEFConnection->sQueuedOperation));
}

/* Execute the queued format (after polling) */
static void static_PNDEFExecuteQueuedFormat(
      tContext * pContext,
      void * pObject,
      W_ERROR nResult)
{
   tNDEFOperation* pNDEFOperation = (tNDEFOperation*) pObject;

   PDebugTrace("static_PNDEFExecuteQueuedFormat");

   /* Check operation status */
   if ( (pNDEFOperation->hOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pNDEFOperation->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PNDEFExecuteQueuedFormat: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_PNDEFFormatCompleted(pContext, pNDEFOperation, nResult);
   }
   else
   {
      /* Format */
      static_PNDEFFormat(pContext,
                         pNDEFOperation->hConnection,
                         pNDEFOperation);
   }
}


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */


