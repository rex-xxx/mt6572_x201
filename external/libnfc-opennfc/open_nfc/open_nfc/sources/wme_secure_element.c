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
   Contains the secure element implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( SE )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* The SE Open Connection structure containing the call context */
typedef struct __tPSEUserConnection
{
   tHandleObjectHeader sObjectHeader;

   tDFCCallbackContext sOpenCallback;
   tDFCCallbackContext sDestroyCallback;

   W_HANDLE hDriverConnection;

} tPSEUserConnection;

/* Destroy connection callback */
static void static_PSEUserDestroyConnectionFinal(
            tContext* pContext,
            void* pParameter,
            W_ERROR nError)
{
   tPSEUserConnection* pUserConnection = (tPSEUserConnection*)pParameter;

   PDFCPostContext2( &pUserConnection->sDestroyCallback, nError );

   /* Free the connection structure */
   CMemoryFree( pUserConnection );
}

/* Destroy connection callback */
static uint32_t static_PSEUserDestroyConnection(
         tContext* pContext,
         tPBasicGenericCallbackFunction* pCallback,
         void* pCallbackParameter,
         void* pObject )
{
   tPSEUserConnection* pUserConnection = (tPSEUserConnection*)pObject;

   PDebugTrace("static_PSEUserDestroyConnection");

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pUserConnection->sDestroyCallback );

   PHandleCloseSafe(pContext, pUserConnection->hDriverConnection, static_PSEUserDestroyConnectionFinal, pUserConnection);

   return P_HANDLE_DESTROY_PENDING;
}

static uint32_t static_PSEGetPropertyNumber(
            tContext* pContext,
            void* pObject
             )
{
   return 1;
}

static bool_t static_PSEGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   pPropertyArray[0] = W_PROP_SECURE_ELEMENT;
   return W_TRUE;
}


static bool_t static_PSECheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   PDebugTrace(
      "static_PSECheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   return ( nPropertyValue == W_PROP_SECURE_ELEMENT )?W_TRUE:W_FALSE;
}

/* Connection registry User Secure Element type */
tHandleType g_sPSEUserConnection = { null, static_PSEUserDestroyConnection,
   static_PSEGetPropertyNumber, static_PSEGetProperties, static_PSECheckProperties,
   null, null, null, null };

#define P_HANDLE_TYPE_SE_USER_CONNECTION (&g_sPSEUserConnection)

static W_ERROR static_PSeUser7816SmOpenChannel(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint32_t nType,
                  const uint8_t* pAID,
                  uint32_t nAIDLength)
{
   tPSEUserConnection* pUserConnection = (tPSEUserConnection*)pInstance;

   return PSeDriver7816SmOpenChannel(
      pContext,
      pUserConnection->hDriverConnection,
      pCallback, pCallbackParameter,
      nType,
      pAID, nAIDLength);
}

static W_ERROR static_PSeUser7816SmCloseChannel(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter)
{
   tPSEUserConnection* pUserConnection = (tPSEUserConnection*)pInstance;

   return PSeDriver7816SmCloseChannel(
      pContext,
      pUserConnection->hDriverConnection,
      nChannelReference,
      pCallback, pCallbackParameter);
}

static W_ERROR static_PSeUser7816SmExchangeApdu(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength)
{
   tPSEUserConnection* pUserConnection = (tPSEUserConnection*)pInstance;

   return PSeDriver7816SmExchangeApdu(
      pContext,
      pUserConnection->hDriverConnection,
      nChannelReference,
      pCallback, pCallbackParameter,
      pSendApduBuffer, nSendApduBufferLength,
      pReceivedApduBuffer, nReceivedApduBufferMaxLength);
}

static W_ERROR static_PSeUser7816SmGetData(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  uint32_t nType,
                  uint8_t* pBuffer,
                  uint32_t nBufferMaxLength,
                  uint32_t* pnActualLength)
{
   tPSEUserConnection* pUserConnection = (tPSEUserConnection*)pInstance;

   return PSeDriver7816SmGetData(
      pContext,
      pUserConnection->hDriverConnection,
      nChannelReference,
      nType,
      pBuffer, nBufferMaxLength,
      pnActualLength);
}

/* See header file */
tP7816SmInterface g_sPSEUserSmInterface =
{
   static_PSeUser7816SmOpenChannel,
   static_PSeUser7816SmCloseChannel,
   static_PSeUser7816SmExchangeApdu,
   static_PSeUser7816SmGetData
};

static void static_PSEUserOpenConnectionCompletion(
                  tContext* pContext,
                  void * pCallbackParameter,
                  W_HANDLE hDriverConnection,
                  W_ERROR nError)
{
   tPSEUserConnection* pUserConnection = (tPSEUserConnection*)pCallbackParameter;
   W_HANDLE hUserConnection = W_NULL_HANDLE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEUserOpenConnectionCompletion: receive connection error");
      goto return_function;
   }

   nError = PHandleRegister(
            pContext,
            pUserConnection,
            P_HANDLE_TYPE_SE_USER_CONNECTION,
            &hUserConnection);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEUserOpenConnectionCompletion: cannot register the user handle");
      goto return_function;
   }

   pUserConnection->hDriverConnection = hDriverConnection;

   nError = P7816CreateConnectionInternal(
            pContext,
            (tP7816SmInstance*)pUserConnection, /* used as SM instance */
            &g_sPSEUserSmInterface,
            hUserConnection );

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEUserOpenConnectionCompletion: cannot create the ISO 7816 layer");
      goto return_function;
   }

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEUserOpenConnectionCompletion: return the error %s", PUtilTraceError(nError));
      if(hUserConnection == W_NULL_HANDLE)
      {
         if(hDriverConnection != W_NULL_HANDLE)
         {
            PHandleClose(pContext, hDriverConnection);
         }

       PDFCPostContext3( &pUserConnection->sOpenCallback,
         W_NULL_HANDLE, nError );

         CMemoryFree(pUserConnection);

       return;
      }
      else
      {
         PHandleClose(pContext, hUserConnection);
         hUserConnection = W_NULL_HANDLE;
      }
   }

   PDFCPostContext3( &pUserConnection->sOpenCallback,
      hUserConnection, nError );
}

/* See Header file */
W_ERROR PSEGetAtr(
            tContext* pContext,
            W_HANDLE hUserConnection,
            uint8_t* pAtrBuffer,
            uint32_t nAtrBufferLength,
            uint32_t* pnAtrLength )
{
   tPSEUserConnection* pUserConnection;
   W_ERROR nError = PHandleGetObject(pContext, hUserConnection, P_HANDLE_TYPE_SE_USER_CONNECTION, (void**)&pUserConnection);

   if ( (nError != W_SUCCESS) || (pUserConnection == null) )
   {
      PDebugError("PSEGetAtr: bad handle value");
      return nError;
   }

   return PSEDriverGetAtr(
            pContext,
            pUserConnection->hDriverConnection,
            pAtrBuffer, nAtrBufferLength, pnAtrLength );
}

/* See API Specification */
void PSEOpenConnection(
                  tContext* pContext,
                  uint32_t nSlotIdentifier,
                  bool_t bForce,
                  tPBasicGenericHandleCallbackFunction* pCallback,
                  void* pCallbackParameter )
{
   W_ERROR nError;
   tDFCCallbackContext sErrorCallback;
   tPSEUserConnection* pUserConnection = (tPSEUserConnection*)null;

   PDebugTrace("PSEOpenConnection()");

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEOpenConnection: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_error;
   }

   pUserConnection = (tPSEUserConnection*)CMemoryAlloc(sizeof(tPSEUserConnection));
   if(pUserConnection == null)
   {
      PDebugError("PSEOpenConnection: cannot allocate the connection");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pUserConnection->sOpenCallback );

   PSEDriverOpenConnection(
            pContext, nSlotIdentifier, bForce,
            static_PSEUserOpenConnectionCompletion, pUserConnection);

   if ((nError = PContextGetLastIoctlError(pContext)) == W_SUCCESS)
   {
      return;
   }

return_error:

   PDebugError("PSEOpenConnection: return the error %s", PUtilTraceError(nError));

   CMemoryFree(pUserConnection);

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &sErrorCallback );

   PDFCPostContext3( &sErrorCallback,
      W_NULL_HANDLE, nError );
}

/* See API Specification */
W_ERROR PSEGetInfoEx(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            tWSEInfoEx* pSEInfo )
{
   W_ERROR nError;

   PDebugTrace("PSEGetInfo()");

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEGetInfoEx: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
   }
   else if (pSEInfo != null)
   {
      nError = PSEDriverGetInfo(pContext, nSlotIdentifier, pSEInfo, sizeof(tWSEInfoEx));

      /* Filter the capabilities to remove internal flags */
      pSEInfo->nCapabilities &= (W_SE_FLAG_COMMUNICATION
         | W_SE_FLAG_END_OF_TRANSACTION_NOTIFICATION | W_SE_FLAG_REMOVABLE
         | W_SE_FLAG_HOT_PLUG | W_SE_FLAG_UICC | W_SE_FLAG_SWP);
   }
   else
   {
      nError = W_ERROR_BAD_PARAMETER;
   }

   return nError;
}

/* See API Specifications */
W_ERROR WSEOpenConnectionSync(
            uint32_t nSlotIdentifier,
            bool_t bForce,
            W_HANDLE* phConnection)
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WSEOpenConnectionSync()");

   if (phConnection == null)
   {
      PDebugTrace("WSEOpenConnectionSync: phConnection == null");
      return W_ERROR_BAD_PARAMETER;
   }
   else
   {
      *phConnection = W_NULL_HANDLE;
   }

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WSEOpenConnection( nSlotIdentifier, bForce, PBasicGenericSyncCompletionHandle, &param );
   }

   return PBasicGenericSyncWaitForResultHandle(&param, phConnection);
}

W_ERROR WSESetPolicySync(
            uint32_t nSlotIdentifier,
            uint32_t nStorageType,
            uint32_t nProtocols)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WSESetPolicy( nSlotIdentifier, nStorageType, nProtocols, PBasicGenericSyncCompletion, &param );
   }

   return PBasicGenericSyncWaitForResult(&param);
}


static void static_PSEDriverSetPolicyCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   PDFCPostContext2(pCallbackParameter, nError);
   CMemoryFree(pCallbackParameter);
}


void PSESetPolicy(
      tContext* pContext,
      uint32_t nSlotIdentifier,
      uint32_t nStorageType,
      uint32_t nProtocols,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *)CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PSEDriverSetPolicy(pContext, nSlotIdentifier, nStorageType, nProtocols, static_PSEDriverSetPolicyCompleted, pCallbackContext);

      nError = PContextGetLastIoctlError(pContext);
      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PSEDriverSetPolicyCompleted, pCallbackContext, &sCallbackContext);
         PDFCPostContext2(&sCallbackContext, nError);
      }
   }
   else
   {
      tDFCCallbackContext sCallbackContext;

      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
   }
}

static void static_PSEDriverGetStatusCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            bool_t bIsPresent,
            uint32_t nSWPLinkStatus,
            W_ERROR nError )
{
   PDFCPostContext4(pCallbackParameter, bIsPresent, nSWPLinkStatus, nError);
   CMemoryFree(pCallbackParameter);
}

/* See API Specification */
void PSEGetStatus(
         tContext* pContext,
         uint32_t nSlotIdentifier,
         tPSEGetStatusCompleted* pCallback,
         void* pCallbackParameter )
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *)CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError = W_ERROR_OUT_OF_RESOURCE;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PSEDriverGetStatus(pContext, nSlotIdentifier, static_PSEDriverGetStatusCompleted, pCallbackContext);

      nError = PContextGetLastIoctlError(pContext);
   }

   if (nError != W_SUCCESS)
   {
      tDFCCallbackContext sCallbackContext;

      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext4(&sCallbackContext, W_FALSE, W_SE_SWP_STATUS_NO_CONNECTION, nError);
   }
}

/* Declare a synchronous data structure */
typedef struct __tPSEGetStatusSync_Parameters
{
   tPBasicGenericSyncParameters common;

   bool_t bIsPresent;
   uint32_t nSWPLinkStatus;

} tPSEGetStatusSync_Parameters;

static void static_PSEGetStatusSyncCompletedSync(
            void* pCallbackParameter,
            bool_t bIsPresent,
            uint32_t nSWPLinkStatus,
            W_ERROR nError)
{
   tPSEGetStatusSync_Parameters* pParam = (tPSEGetStatusSync_Parameters*)pCallbackParameter;

   pParam->bIsPresent = bIsPresent;
   pParam->nSWPLinkStatus = nSWPLinkStatus;
   pParam->common.nResult = nError;

   CSyncSignalWaitObject(&pParam->common.hWaitObject);
}

/* See API Specification */
W_ERROR WSEGetStatusSync(
      uint32_t nSlotIdentifier,
      bool_t* pbIsPresent,
      uint32_t* pnSWPLinkStatus)
{
   tPSEGetStatusSync_Parameters param;

   if((pnSWPLinkStatus == null) || (pbIsPresent == null))
   {
      PDebugError("WSEGetStatusSync: bad parameter");
      return W_ERROR_BAD_PARAMETER;
   }

   if (WBasicGenericSyncPrepare(&param.common) == W_FALSE)
   {
      PDebugError("WSEGetStatusSync: WBasicGenericSyncPrepare failed");

      *pnSWPLinkStatus = W_SE_SWP_STATUS_NO_CONNECTION;
      *pbIsPresent = W_FALSE;

      return param.common.nResult;
   }

   /* Send the command asynchronously */
   WSEGetStatus( nSlotIdentifier, static_PSEGetStatusSyncCompletedSync, &param );

   CSyncWaitForObject(&param.common.hWaitObject);
   CSyncDestroyWaitObject(&param.common.hWaitObject);

   *pnSWPLinkStatus = param.nSWPLinkStatus;
   *pbIsPresent = param.bIsPresent;

   return param.common.nResult;
}

/* See API Specification */
W_ERROR PSECheckAIDAccess(
         tContext* pContext,
         uint32_t nSlotIdentifier,
         const uint8_t* pAIDBuffer,
         uint32_t nAIDLength,
         const uint8_t* pImpersonationDataBuffer,
         uint32_t nImpersonationDataBufferLength )
{
#ifdef P_INCLUDE_SE_SECURITY
   return PSEDriverImpersonateAndCheckAidAccess(
            pContext,
            nSlotIdentifier,
            pAIDBuffer, nAIDLength,
            pImpersonationDataBuffer, nImpersonationDataBufferLength );
#else
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
}

/* **************************************************************************

      Implementation of WSEMonitorEndOfTransaction()

   ************************************************************************** */

/* *************************************************************************

      DEPRECATED FUNCTIONS

   ************************************************************************* */
#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS

/* See API Specification */
W_ERROR WSEGetInfo(
            uint32_t nSlotIdentifier,
            tSEInfo* pSEInfo )
{
   tWSEInfoEx sSeInfo;
   W_ERROR nError = WSEGetInfoEx(nSlotIdentifier, &sSeInfo);

   if(nError == W_SUCCESS)
   {
      pSEInfo->nSlotIdentifier = sSeInfo.nSlotIdentifier;
      pSEInfo->nSupportedProtocols = sSeInfo.nSupportedProtocols;
      pSEInfo->nCapabilities = sSeInfo.nCapabilities;
      pSEInfo->nVolatilePolicy = sSeInfo.nVolatilePolicy;
      pSEInfo->nPersistentPolicy = sSeInfo.nPersistentPolicy;
      CMemoryCopy(pSEInfo->aDescription, sSeInfo.aDescription, W_SE_DESCRIPTION_LENGTH * sizeof(char16_t));
   }

   pSEInfo->bIsPresent = ((sSeInfo.nCapabilities & W_SE_FLAG_REMOVABLE) == 0)?W_TRUE:W_FALSE;

   return nError;
}

#endif /* P_INCLUDE_DEPRECATED_FUNCTIONS */

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
