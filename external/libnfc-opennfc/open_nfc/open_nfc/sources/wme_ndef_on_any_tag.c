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

/*******************************************************************************

   Common function

*******************************************************************************/

/* Array listing every types of tags */
static const uint8_t g_aTagTypeArray[] = {
      W_PROP_NFC_TAG_TYPE_1,
      W_PROP_NFC_TAG_TYPE_2,
      W_PROP_NFC_TAG_TYPE_3,
      W_PROP_NFC_TAG_TYPE_4_A, W_PROP_NFC_TAG_TYPE_4_B,
#ifdef P_INCLUDE_PICOPASS
      W_PROP_NFC_TAG_TYPE_5,
#endif /* P_INCLUDE_PICOPASS */
      W_PROP_NFC_TAG_TYPE_6,

#ifdef P_INCLUDE_MIFARE_CLASSIC
      W_PROP_NFC_TAG_TYPE_7
#endif /* P_INCLUDE_MIFARE_CLASSIC */
};


static const uint8_t g_aTagAndChipTypeArray[] = {

      W_PROP_NFC_TAG_TYPE_1,
      W_PROP_TYPE1_CHIP,         /* we are able to format typer 1 chips */

      W_PROP_NFC_TAG_TYPE_2,
      W_PROP_MIFARE_UL,          /* we are able to format mifare UL chip */
      W_PROP_MY_D_MOVE,          /* we are able to format My-d move chip */
      W_PROP_MY_D_NFC,           /* we are able to format My-d NFC chip */

      W_PROP_NFC_TAG_TYPE_3,     /* no format possible for type 3 */


      W_PROP_NFC_TAG_TYPE_4_A,   /* no format possible for type 4 */
      W_PROP_NFC_TAG_TYPE_4_B,

#ifdef P_INCLUDE_PICOPASS
      W_PROP_NFC_TAG_TYPE_5,
      W_PROP_PICOPASS_2K,        /* we are able to format PicoPass chips */
      W_PROP_PICOPASS_32K,
#endif /* P_INCLUDE_PICOPASS */

      W_PROP_NFC_TAG_TYPE_6,
      W_PROP_ISO_15693_3,         /* we are able to format some ISO 15693 chips */

#ifdef P_INCLUDE_MIFARE_CLASSIC
      W_PROP_NFC_TAG_TYPE_7,
      W_PROP_MIFARE_1K,
      W_PROP_MIFARE_4K
#endif /* P_INCLUDE_MIFARE_CLASSIC */
};

/*******************************************************************************

   PNDEFWriteMessageOnAnyTag()

*******************************************************************************/

/* Declare a write NDEF on any tag structure */
typedef struct __tNDEFWriteOnAnyTagOperation
{
   /* Registry handle */
   W_HANDLE  hRegistryHandle;

   /* Callback context */
   tDFCCallbackContext  sCallbackContext;

   /* Write operation handle */
   W_HANDLE  hWriteOnAnyTagOperation;

   /* NDEF connection */
   W_HANDLE hNDEFConnection;

   /* Command state */
   uint8_t   nCommandState;

   /* NDEF message to write */
   W_HANDLE   hMessage;

   /* write parameters */
   uint32_t nActionMask;


} tNDEFWriteOnAnyTagOperation;

/* Write on any tag status */
#define P_NDEF_WRITE_ANY_STATE_NOT_INIT            0
#define P_NDEF_WRITE_ANY_STATE_DETECTION_PENDING   1
#define P_NDEF_WRITE_ANY_STATE_WRITE_PENDING       2

/** See WReaderListenToCardDetection */
static void static_PNDEFWriteOnAnyTagWriteCompleted(
            tContext* pContext,
            void * pHandlerParameter,
            W_ERROR nError )
{
   tNDEFWriteOnAnyTagOperation* pWriteOnAnyTagOperation =
      (tNDEFWriteOnAnyTagOperation*)pHandlerParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFWriteOnAnyTagWriteCompleted: receive %s",
         PUtilTraceError(nError));
   }

   if(pWriteOnAnyTagOperation->hRegistryHandle != W_NULL_HANDLE)
   {
      PHandleClose(pContext, pWriteOnAnyTagOperation->hRegistryHandle);
   }

   if(pWriteOnAnyTagOperation->hNDEFConnection != W_NULL_HANDLE)
   {
      /* notify the completion of the operation */
      PReaderHandlerWorkPerformed(
         pContext,
         pWriteOnAnyTagOperation->hNDEFConnection,
         W_FALSE, ( nError == W_SUCCESS )?W_TRUE:W_FALSE );
   }

   if(pWriteOnAnyTagOperation->hWriteOnAnyTagOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, pWriteOnAnyTagOperation->hWriteOnAnyTagOperation);
      PHandleClose(pContext, pWriteOnAnyTagOperation->hWriteOnAnyTagOperation);
   }

   if(pWriteOnAnyTagOperation->hMessage != W_NULL_HANDLE)
   {
      PHandleClose(pContext, pWriteOnAnyTagOperation->hMessage);
   }

   PDFCPostContext2(
      &pWriteOnAnyTagOperation->sCallbackContext,
      nError );

   CMemoryFree(pWriteOnAnyTagOperation);
}

/**
 *
 */

static void static_PNDEFWriteOnAnyTagRightsRetrieved(
   tContext * pContext,
   void     * pCallbackParameteter,
   W_ERROR    nError)
{
   tNDEFWriteOnAnyTagOperation* pWriteOnAnyTagOperation = pCallbackParameteter;
   W_HANDLE hNDEFOperation = W_NULL_HANDLE;

   if (nError == W_SUCCESS)
   {
      PNDEFWriteMessage(
         pContext,
         pWriteOnAnyTagOperation->hNDEFConnection,
         static_PNDEFWriteOnAnyTagWriteCompleted,
         pWriteOnAnyTagOperation,
         pWriteOnAnyTagOperation->hMessage,
         pWriteOnAnyTagOperation->nActionMask,
         &hNDEFOperation );

      if (hNDEFOperation != W_NULL_HANDLE)
      {
         if((nError = PBasicAddSubOperationAndClose(
            pContext, pWriteOnAnyTagOperation->hWriteOnAnyTagOperation, hNDEFOperation)) != W_SUCCESS)
         {
            PDebugError(
            "static_PNDEFWriteOnAnyTagDetectionHandler: error returned by PBasicAddSubOperationAndClose()");
            goto return_error;
         }
      }

      pWriteOnAnyTagOperation->nCommandState = P_NDEF_WRITE_ANY_STATE_WRITE_PENDING;
   }

   return;

return_error:

   PHandleClose(pContext, hNDEFOperation);

   static_PNDEFWriteOnAnyTagWriteCompleted(pContext, pWriteOnAnyTagOperation, nError);

}

/** See WReaderListenToCardDetection */
static void static_PNDEFWriteOnAnyTagDetectionHandler(
            tContext* pContext,
            void * pHandlerParameter,
            W_HANDLE hConnection,
            W_ERROR nError )
{
   tNDEFWriteOnAnyTagOperation* pWriteOnAnyTagOperation =
      (tNDEFWriteOnAnyTagOperation*)pHandlerParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFWriteOnAnyTagDetectionHandler: receive %s",
         PUtilTraceError(nError));
      goto return_error;
   }

   if(PBasicGetOperationState(pContext, pWriteOnAnyTagOperation->hWriteOnAnyTagOperation)
      == P_OPERATION_STATE_CANCELLED)
   {
      PDebugError("static_PNDEFWriteOnAnyTagDetectionHandler: operation cancelled");
      nError = W_ERROR_CANCEL;
      goto return_error;
   }

   /* Stop the detection */
   CDebugAssert(pWriteOnAnyTagOperation->hRegistryHandle != W_NULL_HANDLE);
   PHandleClose(pContext, pWriteOnAnyTagOperation->hRegistryHandle);
   pWriteOnAnyTagOperation->hRegistryHandle = W_NULL_HANDLE;

   /* Store the connection */
   pWriteOnAnyTagOperation->hNDEFConnection = hConnection;

   static_PNDEFWriteOnAnyTagRightsRetrieved(pContext, pWriteOnAnyTagOperation, W_SUCCESS);

   return;

return_error:

   static_PNDEFWriteOnAnyTagWriteCompleted(pContext, pWriteOnAnyTagOperation, nError);
}

/** See tHandleCancelOperation */
static void static_PNDEFWriteOnAnyTagCancelOperation(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tNDEFWriteOnAnyTagOperation* pWriteOnAnyTagOperation =
      (tNDEFWriteOnAnyTagOperation*)pCancelParameter;

   if(pWriteOnAnyTagOperation->nCommandState == P_NDEF_WRITE_ANY_STATE_DETECTION_PENDING)
   {
      tDFCCallbackContext sCallbackContext;

      PHandleClose(pContext, pWriteOnAnyTagOperation->hRegistryHandle);
      pWriteOnAnyTagOperation->hRegistryHandle = W_NULL_HANDLE;

      /* Post the call to avoid concurrency issues */
      PDFCFillCallbackContext(pContext, (tDFCCallback*) static_PNDEFWriteOnAnyTagWriteCompleted, pWriteOnAnyTagOperation, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, W_ERROR_CANCEL);
   }
}

/* See Client API Specifications */
void PNDEFWriteMessageOnAnyTag(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nPriority,
            W_HANDLE hMessage,
            uint32_t nActionMask,
            W_HANDLE* phOperation )
{
   tNDEFWriteOnAnyTagOperation* pWriteOnAnyTagOperation = null;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDebugTrace("PNDEFWriteMessageOnAnyTag()");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the NDEF structure */
   pWriteOnAnyTagOperation = (tNDEFWriteOnAnyTagOperation*)CMemoryAlloc( sizeof(tNDEFWriteOnAnyTagOperation) );

   if ( pWriteOnAnyTagOperation == null )
   {
      PDebugError("PNDEFWriteMessageOnAnyTag: cannot allocate the operation");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pWriteOnAnyTagOperation, 0, sizeof(tNDEFWriteOnAnyTagOperation));

   hOperation = PBasicCreateOperation(
         pContext,
         static_PNDEFWriteOnAnyTagCancelOperation, pWriteOnAnyTagOperation);

   if(hOperation == W_NULL_HANDLE)
   {
      PDebugError("PNDEFWriteMessageOnAnyTag: Cannot allocate the operation");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }

   /* Store the callback information */
   pWriteOnAnyTagOperation->sCallbackContext = sCallbackContext;

   /* Set the command state */
   pWriteOnAnyTagOperation->nCommandState = P_NDEF_WRITE_ANY_STATE_NOT_INIT;

   /* Check the parameters */
   if(( nPriority < W_PRIORITY_MINIMUM )
   || ( nPriority > W_PRIORITY_MAXIMUM ) )
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ((nActionMask & W_NDEF_ACTION_BITMASK) != nActionMask)
   {
      PDebugError("PNDEFWriteMessageOnAnyTag : invalid nActionMask value");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* save the operation parameters */
   pWriteOnAnyTagOperation->nActionMask = nActionMask;

   /* Duplicate the message handle */
   if(hMessage != W_NULL_HANDLE)
   {
      if ( ( nError = PHandleDuplicate(
                        pContext,
                        hMessage,
                        &pWriteOnAnyTagOperation->hMessage ) ) != W_SUCCESS )
      {
         goto return_error;
      }
   }

   if ((nActionMask & W_NDEF_ACTION_FORMAT_BITMASK) == 0)
   {
      /* no format is requested, only listen for tags */

      if ( ( nError = PReaderListenToCardDetection(
                     pContext,
                     static_PNDEFWriteOnAnyTagDetectionHandler, pWriteOnAnyTagOperation,
                     nPriority,
                     g_aTagTypeArray, sizeof(g_aTagTypeArray),
                     &pWriteOnAnyTagOperation->hRegistryHandle ) ) != W_SUCCESS )
      {
         goto return_error;
      }
   }
   else
   {
      /* a format acation is requested, listen on both tags and chips */

      if ( ( nError = PReaderListenToCardDetection(
                     pContext,
                     static_PNDEFWriteOnAnyTagDetectionHandler, pWriteOnAnyTagOperation,
                     nPriority,
                     g_aTagAndChipTypeArray, sizeof(g_aTagAndChipTypeArray),
                     &pWriteOnAnyTagOperation->hRegistryHandle ) ) != W_SUCCESS )
      {
         goto return_error;
      }
   }

   /* Get an operation handle if needed */
   if(phOperation != null)
   {
      *phOperation = hOperation;

      if((nError = PHandleDuplicate(
         pContext, hOperation, &pWriteOnAnyTagOperation->hWriteOnAnyTagOperation))
         != W_SUCCESS)
      {
         goto return_error;
      }
   }
   else
   {
      pWriteOnAnyTagOperation->hWriteOnAnyTagOperation = hOperation;
   }

   pWriteOnAnyTagOperation->nCommandState = P_NDEF_WRITE_ANY_STATE_DETECTION_PENDING;

   return;


return_error:

   PDebugError("PNDEFWriteMessageOnAnyTag: return %s", PUtilTraceError(nError));

   if(phOperation != null)
   {
      *phOperation = hOperation;
   }

   if(hOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, hOperation);
   }

   if(pWriteOnAnyTagOperation != null)
   {
      PHandleClose(pContext, pWriteOnAnyTagOperation->hMessage);
      PHandleClose(pContext, pWriteOnAnyTagOperation->hRegistryHandle);
      PHandleClose(pContext, pWriteOnAnyTagOperation->hWriteOnAnyTagOperation);
      CMemoryFree(pWriteOnAnyTagOperation);
   }

   PDFCPostContext2(
      &sCallbackContext,
      nError );

}

/*******************************************************************************

   PNDEFReadMessageOnAnyTag()

*******************************************************************************/

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS

/* Declare a read NDEF on any tag structure */
typedef struct __tNDEFReadOnAnyTagRegistry
{
   /* Connection object registry */
   tHandleObjectHeader        sObjectHeader;

   /* NDEF operation handle */
   W_HANDLE                   hNDEFOperation;
   /* Connection handle */
   W_HANDLE                   hNDEFConnection;

   /* NDEF read information */
   uint8_t                    nTNF;
   char16_t                   aTypeString[P_NDEF_MAX_STRING_LENGTH];

   /* Registry handle */
   W_HANDLE                   hRegistryHandle;

   /* Callback context */
   tDFCCallbackContext        sCallbackContext;

} tNDEFReadOnAnyTagRegistry;

/**
 * @brief   Assynchronous object destruction.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static void static_PNDEFReadOnAnyTagFreeRegistry(
            tContext* pContext,
            void* pObject )
{
   CMemoryFree(pObject);
}

/**
 * @brief   Destroyes a read on any tag registry object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PNDEFReadOnAnyTagDestroyRegistry(
            tContext* pContext,
            void* pObject )
{
   tDFCCallbackContext sCallbackContext;
   tNDEFReadOnAnyTagRegistry* pReadOnAnyTagRegistry
      = (tNDEFReadOnAnyTagRegistry*)pObject;

   PDebugTrace("static_PNDEFReadOnAnyTagDestroyRegistry");

   PHandleClose(pContext, pReadOnAnyTagRegistry->hRegistryHandle);
   pReadOnAnyTagRegistry->hRegistryHandle = W_NULL_HANDLE;

   CDebugAssert(pReadOnAnyTagRegistry->hNDEFOperation == W_NULL_HANDLE);
   CDebugAssert(pReadOnAnyTagRegistry->hNDEFConnection == W_NULL_HANDLE);

   /* Use a DFC to avoid a problem of concurency with static_PNDEFReadOnAnyTagDetectionHandler */
   PDFCFillCallbackContext(pContext, (tDFCCallback*) static_PNDEFReadOnAnyTagFreeRegistry, pReadOnAnyTagRegistry, &sCallbackContext);
   PDFCPostContext1(&sCallbackContext);

   return P_HANDLE_DESTROY_DONE;
}

/* Read message on any tag registry type */
tHandleType g_sNDEFReadOnAnyTag = { static_PNDEFReadOnAnyTagDestroyRegistry,
                                    null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_NDEF_READ_ON_ANY_TAG_REGISTRY (&g_sNDEFReadOnAnyTag)

/* See WNDEFReadMessage() */
static void static_PNDEFReadOnAnyTagReadMessageCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_HANDLE hMessage,
            W_ERROR nError )
{
   tNDEFReadOnAnyTagRegistry* pReadOnAnyTagRegistry
      = (tNDEFReadOnAnyTagRegistry*)pCallbackParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFReadOnAnyTagReadMessageCompleted: receive %s",
         PUtilTraceError(nError));

      CDebugAssert(hMessage == W_NULL_HANDLE);
   }

   if( pReadOnAnyTagRegistry->hNDEFOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, pReadOnAnyTagRegistry->hNDEFOperation);
      pReadOnAnyTagRegistry->hNDEFOperation = W_NULL_HANDLE;
   }

   /* Notify the reader registry */
   CDebugAssert(pReadOnAnyTagRegistry->hNDEFConnection != W_NULL_HANDLE);
   PReaderHandlerWorkPerformed(
      pContext,
      pReadOnAnyTagRegistry->hNDEFConnection,
      W_TRUE, ( nError == W_SUCCESS )?W_TRUE:W_FALSE );
   pReadOnAnyTagRegistry->hNDEFConnection = W_NULL_HANDLE;

   if(nError != W_ERROR_CANCEL)
   {
      PDFCPostContext3(
         &pReadOnAnyTagRegistry->sCallbackContext, hMessage, nError );
   }

   /* Release the protection: may destroy the registry object */
   PHandleDecrementReferenceCount( pContext, pReadOnAnyTagRegistry );
}

/** See WReaderListenToCardDetection */
static void static_PNDEFReadOnAnyTagDetectionHandler(
            tContext* pContext,
            void* pHandlerParameter,
            W_HANDLE hConnection,
            W_ERROR nError )
{
   tNDEFReadOnAnyTagRegistry* pReadOnAnyTagRegistry;

   if ((PHandleGetObject(pContext, PUtilConvertPointerToHandle(pHandlerParameter),
                        P_HANDLE_TYPE_NDEF_READ_ON_ANY_TAG_REGISTRY,
                        (void **) &pReadOnAnyTagRegistry) != W_SUCCESS) || (pReadOnAnyTagRegistry == null))
   {
      PDebugWarning("static_PNDEFReadOnAnyTagDetectionHandler : hRegistry is no longer valid. Collision ?");
      PHandleClose(pContext, hConnection);
      return;
   }

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNDEFReadOnAnyTagDetectionHandler: receive %s",
         PUtilTraceError(nError));
   }

   if(pReadOnAnyTagRegistry->hRegistryHandle == W_NULL_HANDLE)
   {
      PDebugTrace("static_PNDEFReadOnAnyTagDetectionHandler: operation cancelled");
      PHandleClose(pContext, hConnection);
      return;
   }

   if(nError == W_SUCCESS)
   {
      CDebugAssert(pReadOnAnyTagRegistry->hNDEFConnection == W_NULL_HANDLE);
      CDebugAssert(pReadOnAnyTagRegistry->hNDEFOperation == W_NULL_HANDLE);

      pReadOnAnyTagRegistry->hNDEFConnection = hConnection;

      /* Read the message */
      PNDEFReadMessage(
         pContext,
         hConnection,
         static_PNDEFReadOnAnyTagReadMessageCompleted,
         pReadOnAnyTagRegistry,
         pReadOnAnyTagRegistry->nTNF,
         ( pReadOnAnyTagRegistry->nTNF != W_NDEF_TNF_ANY_TYPE &&
           pReadOnAnyTagRegistry->nTNF != W_NDEF_TNF_EMPTY &&
           pReadOnAnyTagRegistry->nTNF != W_NDEF_TNF_UNKNOWN &&
           pReadOnAnyTagRegistry->nTNF != W_NDEF_TNF_UNCHANGED) ? pReadOnAnyTagRegistry->aTypeString : null,
         &pReadOnAnyTagRegistry->hNDEFOperation );

      /* Protect the registry against the destruction */
      PHandleIncrementReferenceCount(pReadOnAnyTagRegistry);
   }
}

/* See Client API Specifications */
void PNDEFReadMessageOnAnyTag(
            tContext* pContext,
            tPBasicGenericHandleCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nPriority,
            uint8_t nTNF,
            const char16_t* pTypeString,
            W_HANDLE* phRegistry )
{
   tNDEFReadOnAnyTagRegistry* pReadOnAnyTagRegistry;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError = W_SUCCESS;
   W_HANDLE hRegistry = W_NULL_HANDLE;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the NDEF structure */
   pReadOnAnyTagRegistry = (tNDEFReadOnAnyTagRegistry*)CMemoryAlloc( sizeof(tNDEFReadOnAnyTagRegistry) );
   if ( pReadOnAnyTagRegistry == null )
   {
      PDebugError("PNDEFReadMessageOnAnyTag: pNDEFOnAnyTagRegistry == null");
      /* Send the error */
      PDFCPostContext3(
         &sCallbackContext, W_NULL_HANDLE, W_ERROR_OUT_OF_RESOURCE );
      return;
   }
   CMemoryFill(pReadOnAnyTagRegistry, 0, sizeof(tNDEFReadOnAnyTagRegistry));

   if(phRegistry == null)
   {
      PDebugError("PNDEFReadMessageOnAnyTag: phRegistry is null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Store the callback information */
   pReadOnAnyTagRegistry->sCallbackContext = sCallbackContext;

   /* Check the parameters */
   if(( nPriority < W_PRIORITY_MINIMUM )
   || ( nPriority > W_PRIORITY_MAXIMUM ) )
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the read parameters */
   if ((nError = PNDEFCheckReadParameters(pContext, nTNF, pTypeString)) != W_SUCCESS)
   {
      PDebugError("PNDEFReadMessageOnAnyTag: PNDEFCheckReadParameters error");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Copy the registry information */
   pReadOnAnyTagRegistry->nTNF = nTNF;
   if ( pTypeString != null )
   {
      CMemoryCopy(
         pReadOnAnyTagRegistry->aTypeString,
         pTypeString,
         PUtilStringLength(pTypeString) * 2 );
   }
   else
   {
      pReadOnAnyTagRegistry->aTypeString[0] = 0;
   }

   /* Create the registry handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     pReadOnAnyTagRegistry,
                     P_HANDLE_TYPE_NDEF_READ_ON_ANY_TAG_REGISTRY,
                     &hRegistry ) ) != W_SUCCESS )
   {
      PDebugError("PNDEFReadMessageOnAnyTag: could not create the registry handle");
      goto return_error;
   }

   /* Register the structure */
   if ( ( nError = PReaderListenToCardDetection(
               pContext,
               static_PNDEFReadOnAnyTagDetectionHandler,
               PUtilConvertHandleToPointer(hRegistry),
               nPriority,
               g_aTagTypeArray, sizeof(g_aTagTypeArray),
               &pReadOnAnyTagRegistry->hRegistryHandle ) ) != W_SUCCESS )
   {
      PDebugError("PNDEFReadMessageOnAnyTag: PReaderListenToCardDetection() in error");
      goto return_error;
   }

   * phRegistry = hRegistry;

   return;

return_error:

   PDebugError("PNDEFReadMessageOnAnyTag: returning %s", PUtilTraceError(nError));

   if (hRegistry != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hRegistry);
   }
   else
   {
      if(pReadOnAnyTagRegistry != null)
      {
         PHandleClose(pContext, pReadOnAnyTagRegistry->hRegistryHandle);
         CMemoryFree(pReadOnAnyTagRegistry);
      }
   }

   PDFCPostContext3(
      &sCallbackContext, W_NULL_HANDLE, nError );
}

#endif /* P_INCLUDE_DEPRECATED_FUNCTIONS */


/*******************************************************************************

   PNDEFRegisterMessageHandler

*******************************************************************************/

#ifdef P_INCLUDE_SNEP_NPP
static void static_NDEFSNEPMessageHandler(tContext* pContext,  void * pCallbackParameter,  uint32_t nDataLength,  W_ERROR nResult );
static void static_NDEFNPPMessageHandler(tContext* pContext,  void * pCallbackParameter,  uint32_t nDataLength,  W_ERROR nError );
#endif

static void static_NDEFTagDetected(tContext* pContext, void * pCallbackParameter, W_HANDLE hCardDetected, W_ERROR nError);
static void static_NDEFTagMessageRead(tContext* pContext, void * pCallbackParameter, W_HANDLE hMessage, W_ERROR nError);

static uint32_t static_PNDEFMessageHandlerDestroyAsync(tContext* pContext, tPBasicGenericCallbackFunction* pCallback, void* pCallbackParameter, void* pObject );
static bool_t static_NDEFMessageIsMatching(tContext * pContext, tNDEFMessageHandler * pNDEFMessageHandler, W_HANDLE hMessage);

static void static_NDEFPostCallback(tContext * pContext, tNDEFMessageHandler * pMessageHandler, W_HANDLE hMessage, uint8_t nProperty);
static bool_t static_NDEFProcessMessageFromTag(tContext * pContext, tNDEFMessageHandler * pCurrentMessageHandler, W_HANDLE hMessage);

tHandleType g_sNDEFMessageHandler = { null, static_PNDEFMessageHandlerDestroyAsync, null, null, null, null, null, null, null };
static const uint8_t static_NDEFPropertiesAll[] = {W_PROP_NFC_TAGS, W_PROP_SNEP, W_PROP_NPP};

#define P_HANDLE_TYPE_NDEF_MESSAGE_HANDLER (&g_sNDEFMessageHandler)


/** See WNDEFRegisterMessageHandler */

W_ERROR PNDEFRegisterMessageHandler  (
   tContext * pContext,
   tPBasicGenericHandleCallbackFunction *  pHandler,
   void *  pHandlerParameter,
   const uint8_t *  pPropertyArray,
   uint32_t  nPropertyNumber,
   uint8_t  nPriority,
   uint8_t  nTNF,
   const char16_t *  pTypeString,
   W_HANDLE *  phRegistry
 )
{
   W_ERROR nError;
   tNDEFMessageHandlerManager * pNDEFMessageHandlerManager = null;
   tNDEFMessageHandler * pNDEFMessageHandler = null;
   tNDEFMessageHandler * pNDEFHeadOfList     = null;
   tNDEFMessageHandler * pNDEFCurrentItem    = null;

#ifdef P_INCLUDE_SNEP_NPP
   bool_t bIsSNEPRequested    = W_FALSE;
   bool_t bIsNPPRequested     = W_FALSE;
#endif /* P_INCLUDE_SNEP_NPP */

   bool_t bIsReaderRequested  = W_FALSE;
   uint32_t nPos = 0;
   uint32_t i;

   PDebugTrace("PNDEFRegisterMessageHandler");
   if( ((pPropertyArray == null) && (nPropertyNumber != 0)) ||
       ((pPropertyArray != null) && (nPropertyNumber == 0)))
   {
      PDebugError("PNDEFRegisterMessageHandler : bad properties");
      nError = W_ERROR_BAD_PARAMETER;
      goto end;
   }
   else if((pPropertyArray == null) && (nPropertyNumber == 0))
   {
      pPropertyArray = static_NDEFPropertiesAll;
      nPropertyNumber = sizeof(static_NDEFPropertiesAll);
   }

   if( (nPriority < W_PRIORITY_MINIMUM) ||
       (nPriority > W_PRIORITY_MAXIMUM) )
   {
      PDebugError("PNDEFRegisterMessageHandler : bad priority");
      nError = W_ERROR_BAD_PARAMETER;
      goto end;
   }

   if(nTNF > W_NDEF_TNF_ANY_TYPE)
   {
      PDebugError("PNDEFRegisterMessageHandler : bad TNF");
      nError = W_ERROR_BAD_PARAMETER;
      goto end;
   }

   if (phRegistry != null)
   {
      * phRegistry = W_NULL_HANDLE;
   }
   else
   {
      PDebugError("PNDEFRegisterMessageHandler : phRegistry == null");
      nError = W_ERROR_BAD_PARAMETER;
      goto end;
   }
   for (i=0; i<nPropertyNumber; i++)
   {
      switch (pPropertyArray[i])
      {
         case W_PROP_SNEP :
#ifdef P_INCLUDE_SNEP_NPP
            bIsSNEPRequested = W_TRUE;
#endif /* P_INCLUDE_SNEP_NPP */
            break;

         case W_PROP_NPP :
#ifdef P_INCLUDE_SNEP_NPP
            bIsNPPRequested = W_TRUE;
#endif /* P_INCLUDE_SNEP_NPP */
            break;

         case W_PROP_NFC_TAGS:
            bIsReaderRequested = W_TRUE;
            break;

         default:
            PDebugError("PNDEFRegisterMessageHandler : invalid property %d", pPropertyArray[i]);
            nError = W_ERROR_BAD_PARAMETER;
            goto end;
      }
   }

   pNDEFMessageHandlerManager = PContextGetNDEFMessageHandlerManager(pContext);
   pNDEFHeadOfList = &pNDEFMessageHandlerManager->aNDEFMessageHandlerByPriority[nPriority - 1];

   /* Allocate a data structure that will contain all stuff specific to this handler registration
      the common stuff (socket, P2P link are stored in the context itself...  */
   pNDEFMessageHandler = (tNDEFMessageHandler *) CMemoryAlloc(sizeof(tNDEFMessageHandler));

   if (pNDEFMessageHandler == null)
   {
      PDebugError("PNDEFRegisterMessageHandler : pNDEFMessageHandler == null");

      nError = W_ERROR_OUT_OF_RESOURCE;
      goto end;
   }

   CMemoryFill(pNDEFMessageHandler, 0, sizeof(tNDEFMessageHandler));

   nError = PHandleRegister(pContext, pNDEFMessageHandler, P_HANDLE_TYPE_NDEF_MESSAGE_HANDLER, phRegistry);
   if (nError != W_SUCCESS)
   {
      PDebugError("PNDEFRegisterMessageHandler : PHandleRegister failed %d", nError);
      goto end;
   }

   pNDEFMessageHandler->nTNF = nTNF;

   if (pTypeString != null)
   {
      pNDEFMessageHandler->pTypeString = (char16_t* ) CMemoryAlloc( (PUtilStringLength(pTypeString) + 1) * sizeof (char16_t));

      if (pNDEFMessageHandler->pTypeString == null)
      {
         PDebugError("PNDEFRegisterMessageHandler : pNDEFMessageHandler->pTypeString == null");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto end;
      }

      PUtilStringCopy(pNDEFMessageHandler->pTypeString, &nPos, pTypeString);
   }
   else
   {
      pNDEFMessageHandler->pTypeString = null;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pHandler, pHandlerParameter, & pNDEFMessageHandler->sCallbackContext);

#ifdef P_INCLUDE_SNEP_NPP
   if (bIsSNEPRequested != W_FALSE)
   {
      nError = PNDEFRegisterSNEPMessageHandlerDriver  (pContext, static_NDEFSNEPMessageHandler, pNDEFMessageHandler, nPriority, & pNDEFMessageHandler->hSNEPRegistry);

      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFRegisterMessageHandler : PNDEFRegisterSNEPMessageHandlerDriver failed %d", nError);
         goto end;
      }
   }

   if (bIsNPPRequested != W_FALSE)
   {
      nError = PNDEFRegisterNPPMessageHandlerDriver  (pContext, static_NDEFNPPMessageHandler, pNDEFMessageHandler, nPriority, & pNDEFMessageHandler->hNPPRegistry);

      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFRegisterMessageHandler : PNDEFRegisterNPPMessageHandlerDriver failed %d", nError);
         goto end;
      }
   }
#endif /* P_INCLUDE_SNEP_NPP */

   /* Reader */
   if(bIsReaderRequested != W_FALSE)
   {
      /* If a listenToCardDetection must be done before */
      if(pNDEFHeadOfList->hReaderRegistry == W_NULL_HANDLE)
      {
         /* Register the structure */
         if ( ( nError = PReaderListenToCardDetection(
                     pContext,
                     static_NDEFTagDetected,
                     pNDEFHeadOfList,
                     nPriority,
                     g_aTagTypeArray, sizeof(g_aTagTypeArray),
                     & pNDEFHeadOfList->hReaderRegistry ) ) != W_SUCCESS )
         {
            PDebugError("PNDEFRegisterMessageHandler: PReaderListenToCardDetection() in error");
            goto end;
         }
      }

      pNDEFMessageHandler->hReaderRegistry = pNDEFHeadOfList->hReaderRegistry;
   }

   pNDEFCurrentItem = pNDEFHeadOfList;

   /* Go to the end of the list */
   while(pNDEFCurrentItem->pNextMessage != null)
      pNDEFCurrentItem = pNDEFCurrentItem->pNextMessage;

   /* Add the newest element at end */
   pNDEFCurrentItem->pNextMessage = pNDEFMessageHandler;
   pNDEFMessageHandler->pPreviousMessage = pNDEFCurrentItem;

end:

   if (nError != W_SUCCESS)
   {
      if ((phRegistry != null) && (* phRegistry != W_NULL_HANDLE))
      {
         PBasicCloseHandle(pContext, * phRegistry);
         * phRegistry = W_NULL_HANDLE;
      }
      else
      {
         if (pNDEFMessageHandler != null)
         {
            CMemoryFree(pNDEFMessageHandler);
         }
      }
   }

   return nError;
}

/* See C API Documentation */
void PNDEFHandlerWorkPerformed(tContext * pContext,
                               bool_t bGiveToNextListener,
                               bool_t bMessageMatch)
{
   tNDEFMessageHandlerManager * pManager = PContextGetNDEFMessageHandlerManager(pContext);
   tNDEFMessageHandler * pCurrentMessageHandler = pManager->pLastMessageHandlerCalled;

   if(pCurrentMessageHandler == null)
   {
      /* no work perform shall be done */
      return;
   }

   pManager->pLastMessageHandlerCalled = null;

#ifdef P_INCLUDE_SNEP_NPP
   if(pManager->nLastPropertyCalled == W_PROP_SNEP)
   {
      PNDEFSetWorkPerformedSNEPDriver(pContext, bGiveToNextListener);
   }
   else if (pManager->nLastPropertyCalled == W_PROP_NPP)
   {
      PNDEFSetWorkPerformedNPPDriver(pContext, bGiveToNextListener);
   }
   else
#endif /* P_INCLUDE_SNEP_NPP */

   if (pManager->nLastPropertyCalled == W_PROP_NFC_TAGS)
   {
      /* Only for W_PROP_NFC_TAGS */
      if(bMessageMatch == W_TRUE)
      {
         pManager->bTmpMessageMatchAtLeastOne = W_TRUE;
      }

      pCurrentMessageHandler = pCurrentMessageHandler->pNextMessage;

      if( (bGiveToNextListener == W_FALSE) ||
          (static_NDEFProcessMessageFromTag(pContext, pCurrentMessageHandler, pManager->hMessage) == W_FALSE) )
      {
         /* The Reader workperformed must be done*/
         PBasicCloseHandle(pContext, pManager->hMessage);
         pManager->hMessage = W_NULL_HANDLE;
         PReaderHandlerWorkPerformed(pContext, pManager->hTmpConnection, bGiveToNextListener, pManager->bTmpMessageMatchAtLeastOne);
         pManager->hTmpConnection = W_NULL_HANDLE;
      }
      else
      {
         /* a Handler in the list of handler has matched and the callback has been posted by the static_NDEFProcessMessageFromTag */
         return;
      }
   }
   else
   {
      CDebugAssert(0);
      PDebugError("PNDEFHandlerWorkPerformed : Invalid Property saved");
   }
}


static void static_PNDEFMessageHandlerDestroyAsyncAutomaton(tContext* pContext, void* pCallbackParameter, W_ERROR nError);

/**
 * This function is called when the handle is closed by the user
 *
 * The function will start the close of all regisry handles
 */

static uint32_t static_PNDEFMessageHandlerDestroyAsync(tContext* pContext, tPBasicGenericCallbackFunction* pCallback, void* pCallbackParameter, void* pObject )
{
   tNDEFMessageHandler * pNDEFMessageHandler = (tNDEFMessageHandler *) pObject;
   tNDEFMessageHandlerManager * pManager     = PContextGetNDEFMessageHandlerManager(pContext);

   PDebugTrace("static_PNDEFMessageHandlerDestroyAsync");

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pNDEFMessageHandler->sCloseCallbackContext);

   /* If this handler is the last called handler, we must do a work performed to notify other handler */
   if(pManager->pLastMessageHandlerCalled == pNDEFMessageHandler)
   {
      PNDEFHandlerWorkPerformed(pContext, W_TRUE, W_TRUE);
   }

   /* Remove the handler from the list */
   CDebugAssert(pNDEFMessageHandler->pPreviousMessage != null);
   (pNDEFMessageHandler->pPreviousMessage)->pNextMessage = pNDEFMessageHandler->pNextMessage;

   if(pNDEFMessageHandler->pNextMessage != null)
   {
      pNDEFMessageHandler->pNextMessage->pPreviousMessage = pNDEFMessageHandler->pPreviousMessage;
   }

   static_PNDEFMessageHandlerDestroyAsyncAutomaton(pContext, (void *) pNDEFMessageHandler, W_SUCCESS);

   return P_HANDLE_DESTROY_PENDING;
}

/**
 * Close automaton
 *
 * Registries are close one after each other, the callback is called once all registry have been closed
 */

static void static_PNDEFMessageHandlerDestroyAsyncAutomaton(tContext* pContext, void* pCallbackParameter, W_ERROR nError)
{
   tNDEFMessageHandler * pNDEFMessageHandler = (tNDEFMessageHandler *) pCallbackParameter;

   if (pNDEFMessageHandler->hSNEPRegistry != W_NULL_HANDLE)
   {
      PDebugTrace("static_PNDEFMessageHandlerDestroyAsyncAutomaton : closing SNEP server registry");
      PBasicCloseHandleSafe(pContext, pNDEFMessageHandler->hSNEPRegistry, static_PNDEFMessageHandlerDestroyAsyncAutomaton, pNDEFMessageHandler);
      pNDEFMessageHandler->hSNEPRegistry = W_NULL_HANDLE;
      return;
   }

   if (pNDEFMessageHandler->hNPPRegistry != W_NULL_HANDLE)
   {
      PDebugTrace("static_PNDEFMessageHandlerDestroyAsyncAutomaton : closing NPP server registry");
      PBasicCloseHandleSafe(pContext, pNDEFMessageHandler->hNPPRegistry, static_PNDEFMessageHandlerDestroyAsyncAutomaton, pNDEFMessageHandler);
      pNDEFMessageHandler->hNPPRegistry = W_NULL_HANDLE;
      return;
   }

   if (pNDEFMessageHandler->hReaderRegistry != W_NULL_HANDLE)
   {
      tNDEFMessageHandler * pNDEFHead           = null;
      tNDEFMessageHandler * pNDEFCurrentHandler = null;
      bool_t                bContinueReader     = W_FALSE;

      /* Get the Head's List */
      pNDEFHead = pNDEFMessageHandler->pPreviousMessage;
      while(pNDEFHead->pPreviousMessage != null)
      {
         pNDEFHead = pNDEFHead->pPreviousMessage;
      }

      /* Search if another item of the list used the reader */
      pNDEFCurrentHandler = pNDEFHead->pNextMessage;
      while(pNDEFCurrentHandler != null)
      {
         if(pNDEFCurrentHandler->hReaderRegistry != W_NULL_HANDLE)
         {
            bContinueReader = W_TRUE;
            break;
         }
         pNDEFCurrentHandler = pNDEFCurrentHandler->pNextMessage;
      }

      pNDEFMessageHandler->hReaderRegistry = W_NULL_HANDLE;
      if(bContinueReader == W_FALSE)
      {
         PDebugTrace("static_PNDEFMessageHandlerDestroyAsyncAutomaton : closing reader registry");
         PBasicCloseHandleSafe(pContext, pNDEFHead->hReaderRegistry, static_PNDEFMessageHandlerDestroyAsyncAutomaton, pNDEFMessageHandler);
         pNDEFHead->hReaderRegistry = W_NULL_HANDLE;
         return;
      }
   }

   PDebugTrace("static_PNDEFMessageHandlerDestroyAsyncAutomaton : complete");
   PDFCPostContext2(&pNDEFMessageHandler->sCloseCallbackContext, W_SUCCESS);

   CMemoryFree(pNDEFMessageHandler->pTypeString);
   CMemoryFree(pNDEFMessageHandler);
}


#ifdef P_INCLUDE_SNEP_NPP
/**
  * This function is called when a NDEF message has been received through SNEP protocol
  *
  * it will retreive the binary content of the message from the driver's part of the stack
  * and build a NDEF message handle from it.
  *
  * Then, the resulting message will be given to the dispatching function that deals with the filtering of
  * the message and the call of the user callback
  */
static void static_NDEFSNEPMessageHandler(tContext* pContext,  void * pCallbackParameter,  uint32_t nDataLength,  W_ERROR nError )
{
   tNDEFMessageHandler * pNDEFMessageHandler = (tNDEFMessageHandler *) pCallbackParameter;
   W_HANDLE  hMessage;
   uint8_t * pBuffer;

   CDebugAssert(pNDEFMessageHandler->hSNEPRegistry != W_NULL_HANDLE);

   PDebugTrace("static_NDEFSNEPMessageHandler");

   if (nError != W_SUCCESS)
   {
      PDebugError("static_NDEFSNEPMessageHandler : nError %d", nError);
      return;
   }

   /* Allocates a buffer and retreive the message */
   if ((pBuffer = (uint8_t *) CMemoryAlloc(nDataLength)) != null)
   {
      /* retreive the NDEF content */
      PNDEFRetrieveSNEPMessageDriver(pContext, pBuffer, nDataLength);

      /* build the corresponding message */
      PNDEFBuildMessage(pContext, pBuffer, nDataLength, & hMessage);
      CMemoryFree(pBuffer);

      /* Call the dispatcher */
      if(static_NDEFMessageIsMatching(pContext, pNDEFMessageHandler, hMessage) == W_TRUE)
      {
         static_NDEFPostCallback(pContext, pNDEFMessageHandler, hMessage, W_PROP_SNEP);
         return;
      }

      PBasicCloseHandle(pContext, hMessage);

      PNDEFSetWorkPerformedSNEPDriver(pContext, W_TRUE);
   }
   else
   {
      PDebugError("static_NDEFSNEPMessageHandler : pBuffer == null");

      PDFCPostContext3(&pNDEFMessageHandler->sCallbackContext, W_NULL_HANDLE, W_ERROR_OUT_OF_RESOURCE);

      PNDEFSetWorkPerformedSNEPDriver(pContext, W_FALSE);
   }
}
#endif /* P_INCLUDE_SNEP_NPP */

#ifdef P_INCLUDE_SNEP_NPP
/**
  * This function is called when a NDEF message has been received through NPP protocol
  *
  * it will retreive the binary content of the message from the driver's part of the stack
  * and build a NDEF message handle from it.
  *
  * Then, the resulting message will be given to the dispatching function that deals with the filtering of
  * the message and the call of the user callback
  */
static void static_NDEFNPPMessageHandler(tContext* pContext,  void * pCallbackParameter,  uint32_t nDataLength,  W_ERROR nError )
{
   tNDEFMessageHandler * pNDEFMessageHandler = (tNDEFMessageHandler *) pCallbackParameter;
   W_HANDLE  hMessage;
   uint8_t * pBuffer;


   PDebugTrace("static_NDEFNPPMessageHandler : nError %d", nError);

   if (nError != W_SUCCESS)
   {
      PDebugError("static_NDEFNPPMessageHandler : nError %d", nError);
      return;
   }

   /* Allocates a buffer and retreive the message */
   if ((pBuffer = (uint8_t *) CMemoryAlloc(nDataLength)) != null)
   {
      /* retreive the NDEF content */
      PNDEFRetrieveNPPMessageDriver(pContext, pBuffer, nDataLength);

      /* build the corresponding message */
      PNDEFBuildMessage(pContext, pBuffer, nDataLength, & hMessage);
      CMemoryFree(pBuffer);

      /* Call the dispatcher */
      if(static_NDEFMessageIsMatching(pContext, pNDEFMessageHandler, hMessage) == W_TRUE)
      {

         static_NDEFPostCallback(pContext, pNDEFMessageHandler, hMessage, W_PROP_NPP);
         return;
      }

      PBasicCloseHandle(pContext, hMessage);

      PNDEFSetWorkPerformedNPPDriver(pContext, W_TRUE);
   }
   else
   {
      PDebugError("static_NDEFNPPMessageHandler : pBuffer == null");

      PDFCPostContext3(&pNDEFMessageHandler->sCallbackContext, W_NULL_HANDLE, W_ERROR_OUT_OF_RESOURCE);

      PNDEFSetWorkPerformedNPPDriver(pContext, W_FALSE);
   }
}
#endif /* P_INCLUDE_SNEP_NPP */

static void static_NDEFTagMessageRead(tContext* pContext, void * pCallbackParameter, W_HANDLE hMessage, W_ERROR nError)
{
   tNDEFMessageHandlerManager * pManager = PContextGetNDEFMessageHandlerManager(pContext);
   tNDEFMessageHandler * pNDEFHead = (tNDEFMessageHandler * )pCallbackParameter;
   tNDEFMessageHandler * pCurrentMessageHandler = pNDEFHead->pNextMessage;

   PDebugTrace("static_NDEFTagMessageRead Error");

   if(nError != W_SUCCESS)
   {
      PDebugTrace("static_NDEFTagMessageRead Error %d", nError);
      goto end;
   }
   CDebugAssert(pManager->hTmpConnection != W_NULL_HANDLE);
   pManager->bTmpMessageMatchAtLeastOne = W_FALSE;

   if(static_NDEFProcessMessageFromTag(pContext, pCurrentMessageHandler, hMessage) == W_TRUE)
   {
      /* end of traitement, the callback has been posted in static_NDEFProcessMessageFromTag */
      return;
   }

   PBasicCloseHandle(pContext, hMessage);
   PReaderHandlerWorkPerformed(pContext, pManager->hTmpConnection, W_TRUE, W_TRUE);
   pManager->hTmpConnection = W_NULL_HANDLE;
   return;

end:
   /* Close the Message Handle */
   PBasicCloseHandle(pContext, hMessage);

   /* Close the Connection */
   PBasicCloseHandle(pContext, pManager->hTmpConnection);
   pManager->hTmpConnection = W_NULL_HANDLE;
}

static bool_t static_NDEFProcessMessageFromTag(tContext * pContext,
                                               tNDEFMessageHandler * pCurrentMessageHandler,
                                               W_HANDLE hMessage)
{
   tNDEFMessageHandlerManager * pManager = PContextGetNDEFMessageHandlerManager(pContext);

   /* Find the First Message to call */
   while(pCurrentMessageHandler != null)
   {
      /* if this handler has been registred to be notified for Tag reading
         And if the received NDEF MEssage matchs the expected TNF and the Type */
      if( (pCurrentMessageHandler->hReaderRegistry != W_NULL_HANDLE) &&
          (static_NDEFMessageIsMatching(pContext, pCurrentMessageHandler, hMessage) == W_TRUE))
      {
         W_HANDLE hMessageCopied;
         uint32_t nNDEFMessageLength = PNDEFGetMessageLength(pContext, hMessage);
         uint32_t nActualMessageLength;
         uint8_t  * pBuffer = (uint8_t *) CMemoryAlloc(nNDEFMessageLength);
         W_ERROR nError;

         if(pBuffer == null)
         {
            PDebugError("Not enough resource for allocation Memory");
            break;
         }

         nError = PNDEFGetMessageContent( pContext,
                                          hMessage,
                                          pBuffer,
                                          nNDEFMessageLength,
                                          &nActualMessageLength );

         if(nError != W_SUCCESS)
         {
            PDebugError("Error during retrieving the Message");
            CMemoryFree(pBuffer);
            break;
         }

         /* Copy data into the ndef Message */
         nError = PNDEFBuildMessage(pContext, pBuffer, nActualMessageLength, &hMessageCopied);
         if(nError != W_SUCCESS)
         {
            PDebugError("Error during the build of the Message");
            PBasicCloseHandle(pContext, hMessageCopied);
            CMemoryFree(pBuffer);
            break;
         }

         /* Free the buffer */
         CMemoryFree(pBuffer);

         /* Save the original NDEF Message handle*/
         pManager->hMessage = hMessage;

         /* Send to Copied Message through the callback */
         static_NDEFPostCallback(pContext, pCurrentMessageHandler, hMessageCopied, W_PROP_NFC_TAGS);
         return W_TRUE;
      }

      pCurrentMessageHandler = pCurrentMessageHandler->pNextMessage;
   }

   return W_FALSE;
}

/**
 * Callback call when a tag is found
 **/
static void static_NDEFTagDetected(tContext* pContext, void * pCallbackParameter, W_HANDLE hTagDetected, W_ERROR nError)
{
   tNDEFMessageHandlerManager * pManager = PContextGetNDEFMessageHandlerManager(pContext);

   PDebugTrace("static_NDEFTagDetected Error %d", nError);

   if(nError != W_SUCCESS || hTagDetected == W_NULL_HANDLE)
   {
      /* Should not arrive */
      return;
   }
   CDebugAssert(pManager->hTmpConnection == W_NULL_HANDLE);

   pManager->hTmpConnection = hTagDetected;

   PNDEFReadMessage(pContext, hTagDetected, static_NDEFTagMessageRead, pCallbackParameter, W_NDEF_TNF_ANY_TYPE, null, null);
}


/**
  * This function returned a boolean indicating if the received message is matching the TNF and the type of the expected NDEF Message
  *
  * @param[in] pContext    The current context
  * @param[in] pNDEFMessageHandler  the Structure which contains the TNF and the type of NDEF Message expected by the user
  * @param[in] hMessage    A handle referencing the received NDEF Message
  *
  * @return    W_TRUE   if the Message referenced by the hMessage is matching
  *                     the expected type of NDEFMessage defined in the tNDEFMessageHandler Structure.
  *            W_FALSE  otherwise
  */
static bool_t static_NDEFMessageIsMatching(tContext * pContext, tNDEFMessageHandler * pNDEFMessageHandler, W_HANDLE hMessage)
{
   W_HANDLE hRecord;
   bool_t bIsMatching = W_FALSE;

   PDebugTrace("static_NDEFMessageIsMatching");

   hRecord = PNDEFGetRecord(pContext, hMessage, 0);

   if ((pNDEFMessageHandler->nTNF == W_NDEF_TNF_ANY_TYPE) || (PNDEFCheckType(pContext, hRecord, pNDEFMessageHandler->nTNF, pNDEFMessageHandler->pTypeString) != W_FALSE))
   {
      bIsMatching = W_TRUE;
   }

   /* The message does not match */
   PBasicCloseHandle(pContext, hRecord);

   return bIsMatching;
}

/**
 * @brief Post the callback contained in the pMessageHandler, save the current NDEF Message Handler and the property of the connection to do the workPerformed
 *
 * @param[in] pContext        the current context
 * @param[in] pMessageHandler the Handler to call
 * @param[in] hMessage        the found NDEF Message
 * @param[in] nProperty       the property of the connection used to retrieve the NDEF Message
 **/
static void static_NDEFPostCallback(tContext * pContext,
                                    tNDEFMessageHandler * pMessageHandler,
                                    W_HANDLE hMessage,
                                    uint8_t nProperty)
{
   tNDEFMessageHandlerManager * pManager = PContextGetNDEFMessageHandlerManager(pContext);

   /* Must be null*/
   CDebugAssert(pManager->pLastMessageHandlerCalled == null);

   PDebugTrace("static_NDEFPostCallback nProperty : %d", nProperty);

   /* Save the last handler called (for the workperformed) and the property of the connection*/
   pManager->pLastMessageHandlerCalled = pMessageHandler;
   pManager->nLastPropertyCalled       = nProperty;

   PDFCPostContext3(&pMessageHandler->sCallbackContext, hMessage, W_SUCCESS);
}




/*******************************************************************************

   PNDEFSendMessage

*******************************************************************************/
#ifdef P_INCLUDE_SNEP_NPP

static void static_PNDEFSendMessageCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError);
static void static_PNDEFSendMessageCancelCurrentOperation(tContext * pContext, void* pCallbackParameter, bool_t bIsClosing);

void PNDEFSendMessage  (
   tContext * pContext,
   tPBasicGenericCallbackFunction *  pCallback,
   void *  pCallbackParameter,
   const uint8_t *  pPropertyArray,
   uint32_t  nPropertyNumber,
   W_HANDLE  hMessage,
   W_HANDLE * phOperation
 )
{
   tNDEFSendMessageInstance * pSendMessageInstance = PContextGetNDEFSendMessageInstance(pContext);
   uint8_t * pBuffer = null;
   uint32_t nLength = 0;
   W_ERROR nError;
   uint32_t i;

   tDFCCallbackContext sCallbackContext;

   PDebugTrace("PNDEFSendMessage");

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);

   for (i=0; i<nPropertyNumber; i++)
   {
      switch (pPropertyArray[i])
      {
         case W_PROP_SNEP :
         case W_PROP_NPP :
            break;

         default:
            PDebugError("PNDEFSendMessage : invalid property %d", pPropertyArray[i]);
            nError = W_ERROR_BAD_PARAMETER;
            goto end;
      }
   }

   if (hMessage != W_NULL_HANDLE)
   {
      nLength = PNDEFGetMessageLength(pContext, hMessage);

      if (nLength == 0)
      {
         PDebugError("PNDEFSendMessage : PNDEFGetMessageLength failed");
         nError = W_ERROR_BAD_HANDLE;
         goto end;
      }

      pBuffer = (uint8_t *) CMemoryAlloc(nLength);

      if (pBuffer == null)
      {
         PDebugError("PNDEFSendMessage : pBuffer == null");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto end;
      }

      nError = PNDEFGetMessageContent(pContext, hMessage, pBuffer, nLength, &nLength);
      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFSendMessage : PNDEFGetMessageContent failed  %d", nError);
         goto end;
      }
   }

   if (pSendMessageInstance->bOperationInProgress != W_FALSE)
   {
      /* We allow only one SNEP operation */
      PDebugError("PNDEFSendMessage : another exchange already in progress");
      nError = W_ERROR_BAD_STATE;
      goto end;
   }

   /* Save order */
   CDebugAssert(nPropertyNumber <= sizeof(pSendMessageInstance->aPropertyArray));
   CMemoryCopy(pSendMessageInstance->aPropertyArray,
               pPropertyArray,
               nPropertyNumber);

   pSendMessageInstance->nPropertyLength = nPropertyNumber;
   pSendMessageInstance->nPropertyOffset = 0;

   pSendMessageInstance->bOperationInProgress = W_TRUE;
   pSendMessageInstance->sCallbackContext = sCallbackContext;

   pSendMessageInstance->pBuffer = pBuffer;
   pSendMessageInstance->nLength = nLength;


   if( pSendMessageInstance->aPropertyArray[pSendMessageInstance->nPropertyOffset] == W_PROP_SNEP )
   {
      pSendMessageInstance->hDriverOperation = PNDEFSendSNEPMessageDriver(pContext, pBuffer, nLength, static_PNDEFSendMessageCompleted, null);
   }
   else if( pSendMessageInstance->aPropertyArray[pSendMessageInstance->nPropertyOffset] == W_PROP_NPP )
   {
      pSendMessageInstance->hDriverOperation = PNDEFSendNPPMessageDriver(pContext, pBuffer, nLength, static_PNDEFSendMessageCompleted, null);
   }
   else
   {
      /* cannot arrive */
      CDebugAssert(0);
      nError = W_ERROR_PROGRAMMING;
      goto end;
   }


   pSendMessageInstance->hUserOperation = PBasicCreateOperation(pContext,
                                                                static_PNDEFSendMessageCancelCurrentOperation,
                                                                null);

   if (phOperation != null)
   {
      *phOperation = pSendMessageInstance->hUserOperation;
   }

   /* no need to perform error management here, the callback will be posted in any case */
   nError = W_SUCCESS;

end:

   if (nError != W_SUCCESS)
   {
      if (pBuffer != null)
      {
         CMemoryFree(pBuffer);
      }

      PDFCPostContext2(&sCallbackContext, nError);
   }

   return;
}


static void static_PNDEFSendMessageCompleted(tContext * pContext, void * pCallbackParameter, W_ERROR nError)
{
   tNDEFSendMessageInstance * pSendMessageInstance = PContextGetNDEFSendMessageInstance(pContext);

   PBasicCloseHandle(pContext, pSendMessageInstance->hDriverOperation);
   pSendMessageInstance->hDriverOperation = W_NULL_HANDLE;

   PDebugTrace("static_PNDEFSendMessageCompleted");

   /* try to next protocol specified */
   if( nError == W_ERROR_EXCLUSIVE_REJECTED )
   {
      /* try next protocol IF ANY */
      if( ++ (pSendMessageInstance->nPropertyOffset) < pSendMessageInstance->nPropertyLength)
      {
         if( pSendMessageInstance->aPropertyArray[pSendMessageInstance->nPropertyOffset] == W_PROP_SNEP )
         {
            pSendMessageInstance->hDriverOperation = PNDEFSendSNEPMessageDriver(pContext,
                                                                                pSendMessageInstance->pBuffer,
                                                                                pSendMessageInstance->nLength,
                                                                                static_PNDEFSendMessageCompleted,
                                                                                null);
            return;
         }
         else if( pSendMessageInstance->aPropertyArray[pSendMessageInstance->nPropertyOffset] == W_PROP_NPP )
         {
            pSendMessageInstance->hDriverOperation = PNDEFSendNPPMessageDriver(pContext,
                                                                               pSendMessageInstance->pBuffer,
                                                                               pSendMessageInstance->nLength,
                                                                               static_PNDEFSendMessageCompleted,
                                                                               null);
            return;
         }
         else
         {
            /* cannot arrive */
            CDebugAssert(0);
            nError = W_ERROR_PROGRAMMING;
         }
      }
   }

   if (pSendMessageInstance->pBuffer != null)
   {
      CMemoryFree(pSendMessageInstance->pBuffer);
      pSendMessageInstance->pBuffer = null;
   }

   PBasicSetOperationCompleted(pContext, pSendMessageInstance->hUserOperation);

   PDFCPostContext2(&pSendMessageInstance->sCallbackContext, nError);

   pSendMessageInstance->bOperationInProgress = W_FALSE;
}


static void static_PNDEFSendMessageCancelCurrentOperation(tContext * pContext,
                                                          void* pCallbackParameter,
                                                          bool_t bIsClosing)
{
   tNDEFSendMessageInstance * pSendMessageInstance = PContextGetNDEFSendMessageInstance(pContext);

   if( ( pSendMessageInstance->bOperationInProgress == W_TRUE ) &&
         ( pSendMessageInstance->hDriverOperation != W_NULL_HANDLE) )
   {
      PBasicCancelOperation(pContext, pSendMessageInstance->hDriverOperation);
   }
}

#else  /* #ifdef P_INCLUDE_SNEP_NPP */

/* See API */
void PNDEFSendMessage  (
   tContext * pContext,
   tPBasicGenericCallbackFunction *  pCallback,
   void *  pCallbackParameter,
   const uint8_t *  pPropertyArray,
   uint32_t  nPropertyNumber,
   W_HANDLE  hMessage,
   W_HANDLE * phOperation
 )
{
   tDFCCallbackContext sCallbackContext;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

#endif /* #ifdef P_INCLUDE_SNEP_NPP */

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

