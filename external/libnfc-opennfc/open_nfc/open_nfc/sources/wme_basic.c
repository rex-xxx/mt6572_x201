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

#define P_MODULE  P_MODULE_DEC( BASIC )

#include "wme_context.h"

/* -----------------------------------------------------------------------------

   PBasicInit() and PBasicTerminate()

----------------------------------------------------------------------------- */

static const char16_t g_aLibraryImplementation[] = OPEN_NFC_PRODUCT_VERSION_BUILD_S;
static const char16_t g_aLibraryVersion40[] = {'4', '.', '0', 0 };
static const char16_t g_aLibraryVersion41[] = {'4', '.', '1', 0 };
static const char16_t g_aLibraryVersion42[] = {'4', '.', '2', 0 };
static const char16_t g_aLibraryVersion43[] = {'4', '.', '3', 0 };
static const char16_t g_aLibraryVersion[] = {'4', '.', '4', 0 };


/* See header */
const char16_t* PBasicGetLibraryVersion(void)
{
   return g_aLibraryVersion;
}

/* See header */
const char16_t* PBasicGetLibraryImplementation(void)
{
   return g_aLibraryImplementation;
}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

W_ERROR PBasicDriverGetVersion(
            tContext* pContext,
            void* pBuffer,
            uint32_t nBufferSize)
{
   if((pBuffer == null) || (nBufferSize < sizeof(g_aLibraryImplementation)))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   CMemoryCopy(pBuffer, g_aLibraryImplementation, sizeof(g_aLibraryImplementation));

   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


/* See Client API Specifications */
W_ERROR PBasicInit(
         tContext * pContext,
         const char16_t* pVersionString )
{
   W_ERROR nError = W_SUCCESS;

   if(pVersionString == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if ((PUtilStringCompare(pVersionString, g_aLibraryVersion) != 0) &&
       (PUtilStringCompare(pVersionString, g_aLibraryVersion43) != 0) &&
       (PUtilStringCompare(pVersionString, g_aLibraryVersion42) != 0) &&
       (PUtilStringCompare(pVersionString, g_aLibraryVersion41) != 0) &&
       (PUtilStringCompare(pVersionString, g_aLibraryVersion40) != 0)
       )
   {
      return W_ERROR_VERSION_NOT_SUPPORTED;
   }

#if (P_BUILD_CONFIG == P_CONFIG_USER)

   if(pContext != null)
   {
      return W_ERROR_BAD_STATE;
   }

   if(nError == W_SUCCESS)
   {
      void* pUserInstance;

      pContext = PContextCreate();
      if(pContext == null)
      {
         nError = W_ERROR_OUT_OF_RESOURCE;;
      }

      if(nError == W_SUCCESS)
      {
         PContextLock(pContext);

         if((pUserInstance = PContextGetUserInstance(pContext)) != null)
         {
            nError = W_ERROR_BAD_STATE;
         }
         else if((pUserInstance = CUserOpen()) == null)
         {
            nError = W_ERROR_DRIVER;
         }
         else
         {
            char16_t aDriverImplementation[65];

            /* Set the user instance for the ioctl calls */
            PContextSetUserInstance(pContext, pUserInstance);

            PContextReleaseLock(pContext);
            nError = PBasicDriverGetVersion(pContext, aDriverImplementation, sizeof(aDriverImplementation));
            PContextLock(pContext);

            if(nError == W_SUCCESS)
            {
               if(PUtilStringCompare(aDriverImplementation, g_aLibraryImplementation) != 0)
               {
                  nError = W_ERROR_DRIVER;
               }
            }

            if(nError == W_SUCCESS)
            {
               nError = PNFCControllerUserReadInfo(pContext);
            }

            if(nError != W_SUCCESS)
            {
               CUserClose(pUserInstance);
            }
         }

         if(nError != W_SUCCESS)
         {
            PContextSetUserInstance(pContext, null);
         }

         PContextReleaseLock(pContext);
      }
   }

#endif /* P_CONFIG_USER */

   return nError;
}

#if (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See Client API Specifications */
void PBasicTerminate(
         tContext * pContext )
{
}

#endif /* P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

/* See Client API Specifications */
void PBasicTerminate(
         tContext * pContext )
{
   void* pUserInstance;

   if(pContext != null)
   {
      if((pUserInstance = PContextGetUserInstance(pContext)) != null)
      {
         CUserClose(pUserInstance);

         PContextSetUserInstance(pContext, null);
      }

      PContextDestroy(pContext);
   }
}

#endif /* P_CONFIG_USER */

/* See header file */
void PBasicGenericSyncCompletionSimple(
            void *pCallbackParameter )
{
   tPBasicGenericSyncParameters* pParam = (tPBasicGenericSyncParameters*)pCallbackParameter;

   CSyncSignalWaitObject(&pParam->hWaitObject);
}

/* See header file */
void PBasicGenericSyncCompletion(
            void *pCallbackParameter,
            W_ERROR nResult )
{
   tPBasicGenericSyncParameters* pParam = (tPBasicGenericSyncParameters*)pCallbackParameter;

   /* Store the error code */
   pParam->nResult = nResult;

   CSyncSignalWaitObject(&pParam->hWaitObject);
}

/* See header file */
void PBasicGenericSyncCompletionHandle(
            void *pCallbackParameter,
            W_HANDLE hHandle,
            W_ERROR nResult )
{
   tPBasicGenericSyncParameters* pParam = (tPBasicGenericSyncParameters*)pCallbackParameter;

   /* Store the handle */
   pParam->res.hHandle = hHandle;

   /* Store the error code */
   pParam->nResult = nResult;

   CSyncSignalWaitObject(&pParam->hWaitObject);
}

/* See header file */
void PBasicGenericSyncCompletionUint32(
            void *pCallbackParameter,
            uint32_t nValue,
            W_ERROR nResult )
{
   tPBasicGenericSyncParameters* pParam = (tPBasicGenericSyncParameters*)pCallbackParameter;

   /* Store the value */
   pParam->res.nValue = nValue;

   /* Store the error code */
   pParam->nResult = nResult;

   CSyncSignalWaitObject(&pParam->hWaitObject);
}

/* See header file */
void PBasicGenericSyncCompletionUint8Uint8(
            void *pCallbackParameter,
            W_ERROR nResult,
            uint8_t nByteValue1,
            uint8_t nByteValue2)
{
   tPBasicGenericSyncParameters* pParam = (tPBasicGenericSyncParameters*)pCallbackParameter;

   /* Store the values */
   pParam->res.aByteValues[0] = nByteValue1;
   pParam->res.aByteValues[1] = nByteValue2;

   /* Store the error code */
   pParam->nResult = nResult;

   CSyncSignalWaitObject(&pParam->hWaitObject);
}

/* See header file */
void PBasicGenericSyncCompletionUint8WError(
            void *pCallbackParameter,
            uint8_t nByteValue,
            W_ERROR nResult)
{
   tPBasicGenericSyncParameters* pParam = (tPBasicGenericSyncParameters*)pCallbackParameter;

   /* Store the values */
   pParam->res.aByteValues[0] = nByteValue;

   /* Store the error code */
   pParam->nResult = nResult;

   CSyncSignalWaitObject(&pParam->hWaitObject);
}


/* See header file */
W_ERROR PBasicGenericSyncWaitForResult(
            tPBasicGenericSyncParameters* pParam)
{
   if ( (pParam->nResult != W_ERROR_SYNC_OBJECT) &&
        (pParam->nResult != W_ERROR_PROGRAMMING))
   {
      CSyncWaitForObject(&pParam->hWaitObject);

      CSyncDestroyWaitObject(&pParam->hWaitObject);
   }

   return pParam->nResult;
}

/* See header file */
W_ERROR PBasicGenericSyncWaitForResultHandle(
            tPBasicGenericSyncParameters* pParam,
            W_HANDLE* phHandle)
{
   if ( (pParam->nResult != W_ERROR_SYNC_OBJECT) &&
        (pParam->nResult != W_ERROR_PROGRAMMING))
   {
      CSyncWaitForObject(&pParam->hWaitObject);

      CSyncDestroyWaitObject(&pParam->hWaitObject);
   }

   if(phHandle != null)
   {
      *phHandle = pParam->res.hHandle;
   }

   return pParam->nResult;
}

/* See header file */
W_ERROR PBasicGenericSyncWaitForResultUint32(
            tPBasicGenericSyncParameters* pParam,
            uint32_t* pnValue)
{
   if ( (pParam->nResult != W_ERROR_SYNC_OBJECT) &&
        (pParam->nResult != W_ERROR_PROGRAMMING))
   {
      CSyncWaitForObject(&pParam->hWaitObject);

      CSyncDestroyWaitObject(&pParam->hWaitObject);
   }

   if(pnValue != null)
   {
      *pnValue = pParam->res.nValue;
   }

   return pParam->nResult;
}

/* See header file */
W_ERROR PBasicGenericSyncWaitForResultUint8Uint8(
            tPBasicGenericSyncParameters* pParam,
            uint8_t* pnByteValue1,
            uint8_t* pnByteValue2)
{
   if ( (pParam->nResult != W_ERROR_SYNC_OBJECT) &&
        (pParam->nResult != W_ERROR_PROGRAMMING))
   {
      CSyncWaitForObject(&pParam->hWaitObject);

      CSyncDestroyWaitObject(&pParam->hWaitObject);
   }

   if(pnByteValue1 != null)
   {
      *pnByteValue1 = pParam->res.aByteValues[0];
   }

   if(pnByteValue2 != null)
   {
      *pnByteValue2 = pParam->res.aByteValues[1];
   }

   return pParam->nResult;
}

/* See header file */
W_ERROR PBasicGenericSyncWaitForResultUint8WError(
            tPBasicGenericSyncParameters* pParam,
            uint8_t* pnByteValue)
{
   if ( (pParam->nResult != W_ERROR_SYNC_OBJECT) &&
        (pParam->nResult != W_ERROR_PROGRAMMING))
   {
      CSyncWaitForObject(&pParam->hWaitObject);

      CSyncDestroyWaitObject(&pParam->hWaitObject);
   }

   if(pnByteValue != null)
   {
      *pnByteValue = pParam->res.aByteValues[0];
   }

   return pParam->nResult;
}


/* See header file */
bool_t PBasicGenericSyncPrepare(
            tContext * pContext,
            void * arg)
{
   tPBasicGenericSyncParameters* pParam = (tPBasicGenericSyncParameters*) arg;

   CMemoryFill(pParam, 0, sizeof(* pParam));

   pParam->nResult = W_SUCCESS;

#ifdef P_SYNCHRONOUS_FUNCTION_DEBUG
   if (PContextCheckCurrentThreadId(pContext) == W_FALSE)
   {
      PDebugError("PBasicGenericSyncPrepare: synchronous function called from a callback");
      pParam->nResult = W_ERROR_PROGRAMMING;
      return W_FALSE;
   }
#endif /* ifdef P_SYNCHRONOUS_FUNCTION_DEBUG */

   if(CSyncCreateWaitObject(&pParam->hWaitObject) == W_FALSE)
   {
      PDebugError("PBasicGenericSyncPrepare: cannot create the sync object");
      pParam->nResult = W_ERROR_SYNC_OBJECT;
      return W_FALSE;
   }

   return W_TRUE;
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */


/* -----------------------------------------------------------------------------

   Generic Operation Functions

----------------------------------------------------------------------------- */

/**
 * Cancels the operation
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pOperationInfo  The operation.
 *
 * @param[in]  bIsClosing  The operation is cancelled because of a close handle.
 **/
static void static_PBasicCancelOperation(
         tContext * pContext,
         tOperationInfo* pOperationInfo,
         bool_t bIsClosing )
{
   if ( pOperationInfo->nOperationState == P_OPERATION_STATE_STARTED )
   {
      uint32_t nPos;

      pOperationInfo->nOperationState = P_OPERATION_STATE_CANCELLED;

      if ( pOperationInfo->pCancelCallback )
      {
         pOperationInfo->pCancelCallback(
            pContext, pOperationInfo->pCancelParameter, bIsClosing);
      }

      /* Cancel the sub-operation */
      for(nPos = 0; nPos < P_OPERATION_SUB_NUMBER; nPos++)
      {
         if(pOperationInfo->aSubOperationArray[nPos] != null)
         {
            static_PBasicCancelOperation(
               pContext, pOperationInfo->aSubOperationArray[nPos], bIsClosing);
            PHandleDecrementReferenceCount(pContext, pOperationInfo->aSubOperationArray[nPos]);
            pOperationInfo->aSubOperationArray[nPos] = null;
         }
      }
   }
}

/**
 * @brief   Destroyes an operation object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PBasicDestroyOperation(
            tContext* pContext,
            void* pObject )
{
   tOperationInfo* pOperationInfo = (tOperationInfo*)pObject;
   uint32_t nPos;

   static_PBasicCancelOperation(pContext, pOperationInfo, W_TRUE);

   for(nPos = 0; nPos < P_OPERATION_SUB_NUMBER; nPos++)
   {
      if(pOperationInfo->aSubOperationArray[nPos] != null)
      {
         PHandleDecrementReferenceCount(pContext, pOperationInfo->aSubOperationArray[nPos]);
      }
   }

   if(pOperationInfo->pEnclosingObject == null)
   {
      /* Free the operation structure */
      CMemoryFree( pOperationInfo );
   }
   else
   {
      void* pEnclosingObject = pOperationInfo->pEnclosingObject;

      CMemoryFill(pOperationInfo, 0, sizeof(tOperationInfo));

      /* May release the enclosing object and the operation */
      PHandleDecrementReferenceCount(pContext, pEnclosingObject);
   }

   return P_HANDLE_DESTROY_DONE;
}

/* Generic operation information type */
static tHandleType g_sOperationInfo = { static_PBasicDestroyOperation,
                                 null, null, null, null, null, null, null, null };

/* Generic operation information type */
#define P_HANDLE_TYPE_OPERATION (&g_sOperationInfo)

/** See header file */
W_HANDLE PBasicCreateOperation(
         tContext* pContext,
         tHandleCancelOperation* pCancelCallback,
         void* pCancelParameter)
{
   return PBasicCreateEmbeddedOperation(
         pContext, null, null, pCancelCallback, pCancelParameter);
}

/** See header file */
W_HANDLE PBasicCreateEmbeddedOperation(
         tContext* pContext,
         void* pEnclosingObject,
         tOperationInfo* pOperationInfo,
         tHandleCancelOperation* pCancelCallback,
         void* pCancelParameter)
{
   W_ERROR nError;
   W_HANDLE hOperation;

   if(pEnclosingObject != null)
   {
      if((pOperationInfo == null) || (pOperationInfo->pEnclosingObject != null))
      {
         PDebugError("PBasicCreateOperation: Error in the allocation flag");
         return W_NULL_HANDLE;
      }
   }
   else
   {
      if(pOperationInfo != null)
      {
         PDebugError("PBasicCreateOperation: Error in the parameters");
         return W_NULL_HANDLE;
      }

      pOperationInfo = (tOperationInfo*)CMemoryAlloc(sizeof(tOperationInfo));
      if(pOperationInfo == null)
      {
         PDebugError("PBasicCreateOperation: Cannot allocate the operation");
         return W_NULL_HANDLE;
      }
   }

   CMemoryFill(pOperationInfo, 0, sizeof(tOperationInfo));

   if ( ( nError = PHandleRegister(
                     pContext,
                     pOperationInfo,
                     P_HANDLE_TYPE_OPERATION,
                     &hOperation) ) != W_SUCCESS )
   {
      PDebugError("PBasicCreateOperation: error %s on PHandleRegister()",
         PUtilTraceError(nError));
      if(pEnclosingObject == null)
      {
         CMemoryFree(pOperationInfo);
      }
      return W_NULL_HANDLE;
   }

   pOperationInfo->nOperationState = P_OPERATION_STATE_STARTED;
   pOperationInfo->pEnclosingObject = pEnclosingObject;
   pOperationInfo->pCancelCallback = pCancelCallback;
   pOperationInfo->pCancelParameter = pCancelParameter;
   pOperationInfo->pSuperOperation = null;

   if(pEnclosingObject != null)
   {
      PHandleIncrementReferenceCount(pEnclosingObject);
   }

   return hOperation;
}

static tOperationInfo* static_PBasicGetOperation(
         tContext* pContext,
         W_HANDLE hOperation)
{
   tOperationInfo* pOperationInfo;

   if ( ( PHandleGetObject(
      pContext, hOperation, P_HANDLE_TYPE_OPERATION, (void**)&pOperationInfo) != W_SUCCESS )
   || ( pOperationInfo == null ) )
   {
      PDebugError("static_PBasicGetOperation: Bad operation handle");
      return null;
   }

   return pOperationInfo;
}

/** See header file */
W_ERROR PBasicAddSubOperationAndClose(
         tContext* pContext,
         W_HANDLE hOperation,
         W_HANDLE hSubOperation)
{
   tOperationInfo* pOperationInfo;
   tOperationInfo* pSubOperationInfo;
   uint32_t nPos;

   if ( (pOperationInfo = static_PBasicGetOperation(pContext, hOperation)) == null )
   {
      return W_ERROR_BAD_HANDLE;
   }
   if ( (pSubOperationInfo = static_PBasicGetOperation(pContext, hSubOperation)) == null )
   {
      return W_ERROR_BAD_HANDLE;
   }

   if(pSubOperationInfo->pSuperOperation != null)
   {
      PDebugError("PBasicAddSubOperationAndClose: super-operation already set");
      PHandleClose(pContext, hSubOperation);
      return W_ERROR_BAD_STATE;
   }

   if(pOperationInfo->nOperationState == P_OPERATION_STATE_CANCELLED)
   {
      PDebugError("PBasicAddSubOperationAndClose: operation cancelled");
      static_PBasicCancelOperation(pContext, pSubOperationInfo, W_FALSE);
      PHandleClose(pContext, hSubOperation);
      return W_SUCCESS;
   }
   else if(pOperationInfo->nOperationState != P_OPERATION_STATE_STARTED)
   {
      PDebugError("PBasicAddSubOperationAndClose: bad state");
      PHandleClose(pContext, hSubOperation);
      return W_ERROR_BAD_STATE;
   }

   for(nPos = 0; nPos < P_OPERATION_SUB_NUMBER; nPos++)
   {
      if(pOperationInfo->aSubOperationArray[nPos] == null)
      {
         pOperationInfo->aSubOperationArray[nPos] = pSubOperationInfo;
         PHandleIncrementReferenceCount(pSubOperationInfo);
         pSubOperationInfo->pSuperOperation = pOperationInfo;
         PHandleClose(pContext, hSubOperation);
         return W_SUCCESS;
      }
   }

   PDebugError("PBasicAddSubOperationAndClose: too many sub-operations registered");
   PHandleClose(pContext, hSubOperation);
   return W_ERROR_OUT_OF_RESOURCE;
}

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)
void PBasicDriverCancelOperation(
         tContext * pContext,
         W_HANDLE hOperation )
{
   PBasicCancelOperation(pContext, hOperation);
}
#endif /* P_CONFIG_DRIVER */

/* See Client API Specifications */
void PBasicCancelOperation(
         tContext * pContext,
         W_HANDLE hOperation )
{
   tOperationInfo* pOperationInfo;
   W_ERROR nError;

#if (P_BUILD_CONFIG == P_CONFIG_USER)

   if (PHandleIsUser(pContext, hOperation) == W_FALSE)
   {
      /* Specific case for USER / KERNEL porting, when a driver operation is canceled from user */
      PBasicDriverCancelOperation(pContext, hOperation);

      /* @todo : what can we do here if the IOCTL failed ? */
      return;
   }
#endif /* P_BUILD_CONFIG == P_CONFIG_USER */

   /* Generic case */

   /* Get the generic operation information */
   nError = PHandleGetObject(pContext, hOperation, P_HANDLE_TYPE_OPERATION, (void**)&pOperationInfo);

   if ( ( nError == W_SUCCESS ) && ( pOperationInfo != null ) )
   {
      static_PBasicCancelOperation( pContext, pOperationInfo, W_FALSE );
      if(pOperationInfo->pSuperOperation != null)
      {
         uint32_t nPos;
         for(nPos = 0; nPos < P_OPERATION_SUB_NUMBER; nPos++)
         {
            if(pOperationInfo->pSuperOperation->aSubOperationArray[nPos] == pOperationInfo)
            {
               pOperationInfo->pSuperOperation->aSubOperationArray[nPos] = null;
               PHandleDecrementReferenceCount(pContext, pOperationInfo);
               break;
            }
         }
         pOperationInfo->pSuperOperation = null;
      }
   }
   else
   {
      PDebugWarning(
         "PBasicCancelOperation: could not get pOperationInfo buffer of hOperation 0x%08X",
         hOperation );
   }
}

/** See header file */
void PBasicSetOperationCompleted(
         tContext* pContext,
         W_HANDLE hOperation)
{
   tOperationInfo* pOperationInfo;
   uint32_t nPos;

   if ( (pOperationInfo = static_PBasicGetOperation(pContext, hOperation)) != null )
   {
      if(pOperationInfo->nOperationState == P_OPERATION_STATE_STARTED)
      {
         pOperationInfo->nOperationState = P_OPERATION_STATE_COMPLETED;

         for(nPos = 0; nPos < P_OPERATION_SUB_NUMBER; nPos++)
         {
            if(pOperationInfo->aSubOperationArray[nPos] != null)
            {
               PHandleDecrementReferenceCount(pContext, pOperationInfo->aSubOperationArray[nPos]);
               pOperationInfo->aSubOperationArray[nPos] = null;
            }
         }

         if(pOperationInfo->pSuperOperation != null)
         {
            for(nPos = 0; nPos < P_OPERATION_SUB_NUMBER; nPos++)
            {
               if(pOperationInfo->pSuperOperation->aSubOperationArray[nPos] == pOperationInfo)
               {
                  pOperationInfo->pSuperOperation->aSubOperationArray[nPos] = null;
                  PHandleDecrementReferenceCount(pContext, pOperationInfo);
                  break;
               }
            }
            pOperationInfo->pSuperOperation = null;
         }
      }
   }
}

/** See header file */
uint32_t PBasicGetOperationState(
         tContext* pContext,
         W_HANDLE hOperation)
{
   tOperationInfo* pOperationInfo;

   if ( (pOperationInfo = static_PBasicGetOperation(pContext, hOperation)) != null )
   {
      return pOperationInfo->nOperationState;
   }

   PDebugError("PBasicGetOperationState: Bad handle");

   return P_OPERATION_STATE_CANCELLED;
}

/* -----------------------------------------------------------------------------

   Connection Properties Functions

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See Client API Specifications */
W_ERROR PBasicGetConnectionPropertyNumber(
         tContext * pContext,
         W_HANDLE hConnection,
         uint32_t* pnPropertyNumber )
{
   return PHandleGetAllPropertyNumber(pContext, hConnection, pnPropertyNumber, W_TRUE);  /* Retrieve only visible properties */
}

/* See Client API Specifications */
W_ERROR PBasicGetConnectionProperties(
         tContext * pContext,
         W_HANDLE hConnection,
         uint8_t* pPropertyArray,
         uint32_t nArrayLength )
{
   W_ERROR nError = W_ERROR_BAD_HANDLE;
   uint32_t nPropertyNumber = 0;

   if (pPropertyArray == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PHandleGetAllPropertyNumber(pContext, hConnection, &nPropertyNumber, W_TRUE);  /* Retrieve only visible properties */

   if (  ( nError == W_SUCCESS )
      && ( nPropertyNumber != 0 ) )
   {
      if(nArrayLength < nPropertyNumber)
      {
         nError = W_ERROR_BUFFER_TOO_SHORT;
      }
      else
      {
         nError = PHandleGetAllProperties(pContext, hConnection, pPropertyArray, nArrayLength, W_TRUE);  /* Retrieve only visible properties */
      }
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR PBasicCheckConnectionProperty(
         tContext * pContext,
         W_HANDLE hConnection,
         uint8_t nPropertyIdentifier )
{
   if (W_TRUE == PReaderUserIsPropertyVisible(nPropertyIdentifier))
   {
      return PHandleCheckProperty( pContext, hConnection, nPropertyIdentifier);
   }
   else
   {
      /* This property is internal and not visible */
      return W_ERROR_ITEM_NOT_FOUND;
   }
}

/* See Client API Specifications */
const char * PBasicGetConnectionPropertyName(
         tContext * pContext,
         uint8_t nPropertyIdentifier )
{
   return PUtilTraceConnectionProperty(nPropertyIdentifier);
}

/* See Client API Specifications */
const char * PBasicGetErrorString(
         tContext * pContext,
         W_ERROR nError )
{
   return PUtilTraceError(nError);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */


/* -----------------------------------------------------------------------------

   Close Handle

----------------------------------------------------------------------------- */

/* See Client API Specifications */
void PBasicCloseHandle(
         tContext * pContext,
         W_HANDLE hHandle )
{
   PHandleClose( pContext, hHandle );
}

/* See Client API Specifications */
void PBasicCloseHandleSafe(
         tContext * pContext,
         W_HANDLE hHandle,
         tPBasicGenericCallbackFunction* pCallback,
         void* pCallbackParameter)
{
   PHandleCloseSafe( pContext, hHandle, pCallback, pCallbackParameter );
}

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See Client API Specifications */
W_ERROR WBasicCloseHandleSafeSync(
                  W_HANDLE hHandle )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WBasicCloseHandleSafe(
         hHandle,
         PBasicGenericSyncCompletion, &param );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   Event Loop Functions

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See Client API Specifications */
W_ERROR PBasicPumpEvent(
            tContext * pContext,
            bool_t bWait )
{
   PDebugError("WBasicPumpEvent: Only supported in the user API.");
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See Client API Specifications */
void PBasicStopEventLoop(
            tContext * pContext )
{
   PDebugError("WBasicStopEventLoop: Only supported in the user API.");
}

/* See Client API Specifications */
void PBasicExecuteEventLoop(
            tContext * pContext )
{
   PDebugError("WBasicExecuteEventLoop: Only supported in the user API.");
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

/* See Client API Specifications */
W_ERROR PBasicPumpEvent(
            tContext * pContext,
            bool_t bWait )
{
   bool_t bSomeCall = W_FALSE;
   W_ERROR nError;

loop:

   bSomeCall = W_FALSE;

   PContextLockForPump(pContext);
   if(PDFCPump(pContext) != W_FALSE)
   {
      bSomeCall = W_TRUE;
   }
   PContextReleaseLockForPump(pContext);

   /* Check if the context is still alive */
   if(PContextIsDead(pContext))
   {
      /* Destroy the context after a call to WBasicTerminate from
         a handler function of the client application */
      CMemoryFree(pContext);

      return W_ERROR_BAD_STATE;
   }

   nError = PDFCDriverPumpEvent(pContext, bWait);

   if (nError == W_SUCCESS)
   {
      bSomeCall = W_TRUE;
   }

   switch(nError)
   {
      case W_ERROR_NO_EVENT:
         /* Explicit fall through */
      case W_SUCCESS:
         PContextLockForPump(pContext);
         if(PDFCPump(pContext) != W_FALSE)
         {
            bSomeCall = W_TRUE;
         }
         PContextReleaseLockForPump(pContext);

         /* Check if the context is still alive */
         if(PContextIsDead(pContext))
         {
            /* Destroy the context after a call to WBasicTerminate from
               a handler function of the client application */
            CMemoryFree(pContext);

            nError = W_ERROR_BAD_STATE;
         }
         else
         {
            nError = (bSomeCall != W_FALSE)? W_SUCCESS : W_ERROR_NO_EVENT;
         }
         break;

      case W_ERROR_BAD_STATE:
         /* Check if the context is still alive */
         if(PContextIsDead(pContext))
         {
            /* Destroy the context after a call to WBasicTerminate from
               a handler function of the client application */
            CMemoryFree(pContext);
         }
         break;

      default:
         break;
   }

   if ((nError == W_ERROR_NO_EVENT) && (bWait != W_FALSE))
   {
      goto loop;
   }

   return nError;
}
#endif /* P_BUILD_CONFIG == P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) && (!defined P_CONFIG_CLIENT_SERVER)
/* See Client API Specifications */
void PBasicStopEventLoop(
            tContext * pContext )
{
   PDFCDriverStopEventLoop(pContext);
   /* @todo : if the IOCTL failed, the driver event loop will not be stopped */
}

/* See Client API Specifications */
void PBasicExecuteEventLoop(
            tContext * pContext )
{
   W_ERROR nError;

   do
   {
      PContextLockForPump(pContext);
      (void)PDFCPump(pContext);
      PContextReleaseLockForPump(pContext);

      /* Check if the context is still alive */
      if(PContextIsDead(pContext))
      {
         /* Destroy the context after a call to WBasicTerminate from
            a handler function of the client application */
         CMemoryFree(pContext);
         return;
      }

      nError = PDFCDriverPumpEvent(pContext, W_TRUE);
   }
   while((nError == W_SUCCESS) || (nError == W_ERROR_NO_EVENT));

   if(nError == W_ERROR_BAD_STATE)
   {
      /* Check if the context is still alive */
      if(PContextIsDead(pContext))
      {
         /* Destroy the context after a call to WBasicTerminate from
            a handler function of the client application */
         CMemoryFree(pContext);
      }
   }
}
#endif /* P_CONFIG_USER && !P_CONFIG_CLIENT_SERVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) && (defined P_CONFIG_CLIENT_SERVER)

void PBasicStopEventLoop(
            tContext * pContext )
{
   void* pInstance = PContextGetUserInstance(pContext);

   CUserStopEventLoop(pInstance);
}

/* See Client API Specifications */
void PBasicExecuteEventLoop(
            tContext * pContext )
{
   void* pInstance = PContextGetUserInstance(pContext);

   CUserExecuteEventLoop(pInstance);

   if(PContextIsDead(pContext))
   {
      CMemoryFree(pContext);
   }
}

#endif /* P_CONFIG_USER && P_CONFIG_CLIENT_SERVER */





