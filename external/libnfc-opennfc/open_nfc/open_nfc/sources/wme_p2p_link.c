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
   Contains the implementation of the Peer 2 Peer functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P_LNK )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See client API */
W_ERROR PP2PSetConfiguration(
   tContext * pContext,
   const tWP2PConfiguration * pConfiguration)
{
   if (pConfiguration)
   {
      return PP2PSetConfigurationDriver(pContext, pConfiguration, sizeof(*pConfiguration));
   }
   else
   {
      /* all errors are handled in the driver implementation */
      return PP2PSetConfigurationDriver(pContext, null, 0);
   }
}

/* See client API */
W_ERROR PP2PGetConfiguration(
   tContext * pContext,
   tWP2PConfiguration * pConfiguration)
{
   if (pConfiguration)
   {
      return PP2PGetConfigurationDriver(pContext, pConfiguration, sizeof(*pConfiguration));
   }
   else
   {
      /* all errors are handled in the driver implementation */
      return PP2PGetConfigurationDriver(pContext, null, 0);
   }
}

/* See client API */
void WP2PEstablishLink(
  tWBasicGenericHandleCallbackFunction * pEstablishmentCallback,
  void * pEstablishmentCallbackParameter,
  tWBasicGenericCallbackFunction * pReleaseCallback,
  void * pReleaseCallbackParameter,
  W_HANDLE *phOperation
)
{
   W_HANDLE hLink;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   hLink = WP2PEstablishLinkDriver1(pEstablishmentCallback, pEstablishmentCallbackParameter);

   if (hLink != W_NULL_HANDLE)
   {
      WP2PEstablishLinkDriver2(hLink, pReleaseCallback, pReleaseCallbackParameter, phOperation);
   }
}

/* See header */
void PP2PEstablishLinkWrapper(
  tContext * pContext,
  tPBasicGenericHandleCallbackFunction * pEstablishmentCallback,
  void * pEstablishmentCallbackParameter,
  tPBasicGenericCallbackFunction * pReleaseCallback,
  void * pReleaseCallbackParameter,
  W_HANDLE *phOperation)
{
   W_HANDLE hLink;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   hLink = PP2PEstablishLinkDriver1Wrapper(pContext, pEstablishmentCallback, pEstablishmentCallbackParameter);

   if (hLink != W_NULL_HANDLE)
   {
      PP2PEstablishLinkDriver2Wrapper(pContext, hLink, pReleaseCallback, pReleaseCallbackParameter, phOperation);
   }
}

/* See client API */
W_ERROR PP2PGetLinkProperties(
   tContext * pContext,
   W_HANDLE hLink,
   tWP2PLinkProperties * pProperties
)
{
   if (pProperties)
   {
      return PP2PGetLinkPropertiesDriver(pContext, hLink, pProperties, sizeof(* pProperties));
   }
   else
   {
      /* all errors are handled in the driver implementation */
      return PP2PGetLinkPropertiesDriver(pContext, hLink, null, 0);
   }
}

static void static_PP2PURILookupCompleted(
      tContext* pContext,
      void * pCallbackParameter,
      uint8_t nDSAP,
      W_ERROR nError )
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) pCallbackParameter;

   PDebugError("static_PP2PURILookupCompleted");

   PDFCPostContext3(pCallbackContext, (uint32_t) nDSAP, nError);
   CMemoryFree(pCallbackContext);
}

/* See client API */
void PP2PURILookup(
      tContext * pContext,
      W_HANDLE hLink,
      tPP2PURILookupCompleted * pCallback,
      void * pCallbackParameter,
      const char16_t * pServiceURI)
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      if (pServiceURI != null)
      {
         PP2PURILookupDriver(pContext, hLink, static_PP2PURILookupCompleted, pCallbackContext, pServiceURI, PUtilStringLength(pServiceURI) * sizeof(char16_t));
      }
      else
      {
      /* all errors are handled in the driver implementation */
         PP2PURILookupDriver(pContext, hLink, static_PP2PURILookupCompleted, pCallbackContext, null, 0);
      }

      nError = PContextGetLastIoctlError(pContext);

      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PP2PURILookupCompleted, pCallbackContext, &sCallbackContext);
         PDFCPostContext3(&sCallbackContext, 0, nError);
      }
   }
   else
   {
      tDFCCallbackContext sCallbackContext;

      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, 0, W_ERROR_OUT_OF_RESOURCE);
   }
}

/* synchronous function of WP2PURILookup */
W_ERROR WP2PURILookupSync(
             W_HANDLE hLink,
             const char16_t* pServiceURI,
             uint8_t * pnDSAP)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WP2PURILookup(
         hLink,
         PBasicGenericSyncCompletionUint8WError, &param,
         pServiceURI);
   }

   return PBasicGenericSyncWaitForResultUint8WError(&param,
                                                    pnDSAP);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */


#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

   /* static function prototypes */

static void static_PP2PCreateLLCPInstance(tContext * pContext);
static void static_PP2PDestroyLLCPInstance(tContext * pContext, W_ERROR nError);


/* see header file */
void PP2PCreate(
      tP2PInstance* pP2PInstance )
{
   CMemoryFill(pP2PInstance, 0, sizeof(pP2PInstance));

   PLLCPCreate(&pP2PInstance->sLLCPInstance);
}

/* see header file */
void PP2PDestroy(
      tP2PInstance* pP2PInstance )
{
   PLLCPDestroy(&pP2PInstance->sLLCPInstance);
}

/* see header file */

bool_t PP2PCheckP2PSupport(
      tContext * pContext)
{
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );

   if (((pNFCControllerInfo->nProtocolCapabilities & W_NFCC_PROTOCOL_READER_P2P_INITIATOR) == 0) ||
       ((pNFCControllerInfo->nProtocolCapabilities & W_NFCC_PROTOCOL_CARD_P2P_TARGET) == 0))
   {
      return W_FALSE;
   }

   return W_TRUE;
}

/* See WP2PSetConfiguration */
W_ERROR PP2PSetConfigurationDriver(
      tContext * pContext,
      const tWP2PConfiguration * pConfiguration,
      uint32_t nLength)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   PDebugTrace("PP2PSetConfigurationDriver");

   if (PP2PCheckP2PSupport(pContext) == W_FALSE)
   {
      return W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
   }

   if (pConfiguration == null)
   {
      PDebugError("PP2PSetConfigurationDriver : pConfiguration == null");
      return W_ERROR_BAD_PARAMETER;
   }

    if ((pConfiguration->nLocalLTO < 10) || (pConfiguration->nLocalLTO > 2550))
   {
      PDebugError("PP2PSetConfigurationDriver : LTO must be in the range [10-2550] ms");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pConfiguration->nLocalLTO %10 ) != 0)
   {
     PDebugError("PP2PSetConfigurationDriver : LTO must be a multiple of 10 ms");
     return W_ERROR_BAD_PARAMETER;
   }

   if ( (pConfiguration->nLocalMIU < 128) || (pConfiguration->nLocalMIU > 256))
   {
      PDebugError("PP2PSetConfigurationDriver : MIU must be in the range 128-256");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pConfiguration->bAllowInitiatorMode != W_FALSE) && (pConfiguration->bAllowInitiatorMode == W_FALSE))
   {
      PDebugError("PP2PSetConfigurationDriver : bAllowInitiatorMode must be W_TRUE/W_FALSE");
      return W_ERROR_BAD_PARAMETER;
   }

   pLLCPInstance->nLocalTimeoutMs = pConfiguration->nLocalLTO;
   pLLCPInstance->nLocalLinkMIU = pConfiguration->nLocalMIU;
   pLLCPInstance->bAllowInitiatorMode = pConfiguration->bAllowInitiatorMode;
   pLLCPInstance->bAllowActiveMode = pConfiguration->bAllowActiveMode;
   pLLCPInstance->bAllowTypeATargetProtocol = pConfiguration->bAllowTypeATargetProtocol;

   return W_SUCCESS;
}

/* See WP2PSetConfiguration */
W_ERROR PP2PGetConfigurationDriver(
      tContext * pContext,
      tWP2PConfiguration * pConfiguration,
      uint32_t nLength)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   PDebugTrace("PP2PGetConfigurationDriver");

   if (PP2PCheckP2PSupport(pContext) == W_FALSE)
   {
      return W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
   }

   if (pConfiguration == null)
   {
      PDebugError("PP2PSetConfigurationDriver : pConfiguration == null");
      return W_ERROR_BAD_PARAMETER;
   }

   pConfiguration->nLocalLTO = pLLCPInstance->nLocalTimeoutMs;
   pConfiguration->nLocalMIU = pLLCPInstance->nLocalLinkMIU;
   pConfiguration->bAllowInitiatorMode =  pLLCPInstance->bAllowInitiatorMode;
   pConfiguration->bAllowActiveMode = pLLCPInstance->bAllowActiveMode;
   pConfiguration->bAllowTypeATargetProtocol = pLLCPInstance->bAllowTypeATargetProtocol;

   return W_SUCCESS;
}

/**
  * Adds a link into the list of P2P links
  */
static void static_PP2PRegisterLink(
   tContext * pContext,
   tP2PLink * pP2PLink)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);

   PDebugTrace("PP2PRegisterLink : hLink %08x - %p", pP2PLink->hLink, pP2PLink );

   pP2PLink->bIsRegistered = W_TRUE;
   pP2PLink->pNext = pP2PInstance->pFirstP2PLink;
   pP2PInstance->pFirstP2PLink = pP2PLink;
}

/*
 * link deactivation request completion
 */
static void static_PLLCPRequestLinkDeactivationCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   /* the link deactivation request has been sent, cut the link */
   static_PP2PDestroyLLCPInstance(pContext, W_SUCCESS);
}

static void static_PP2PIncrementLLCPUsageCount(
      tContext * pContext)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   pP2PInstance->nNbEstablishRequests++;
   if (pP2PInstance->nNbEstablishRequests == 1)
   {
      /* request establishment of the LLCP link */
      static_PP2PCreateLLCPInstance(pContext);
   }
}

static void static_PP2PDecrementLLCPUsageCount(
      tContext * pContext)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);

   CDebugAssert(pP2PInstance->nNbEstablishRequests > 0);

   pP2PInstance->nNbEstablishRequests--;
   if (pP2PInstance->nNbEstablishRequests == 0)
   {
      /* There's no longer any link registered */
      if (pP2PInstance->sLLCPInstance.nRole == LLCP_ROLE_NONE)
      {
         /* The P2P RF connection is not established, stop the RF now */
         static_PP2PDestroyLLCPInstance(pContext, W_SUCCESS);
      }
      else
      {
         /* The P2P RF link is established, send a DISC (0, 0) */
         PLLCPRequestLinkDeactivation(pContext, static_PLLCPRequestLinkDeactivationCompleted, null);
      }
   }
}

/**
  * Removes a link from the list of P2P links
  */
static void static_PP2PUnregisterLink(
   tContext * pContext,
   tP2PLink * pP2PLink)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PLink * pCurrent, * pLast;

   PDebugTrace("PP2PUnregisterLink : hLink %08x - %p", pP2PLink->hLink, pP2PLink );

   CDebugAssert(pP2PLink->bIsRegistered != W_FALSE);

   for ( pLast = null, pCurrent = pP2PInstance->pFirstP2PLink; pCurrent != null; pLast = pCurrent, pCurrent = pCurrent->pNext)
   {
      if (pCurrent == pP2PLink)
      {
         if (pLast == null)
         {
            pP2PInstance->pFirstP2PLink = pCurrent->pNext;
         }
         else
         {
            pLast->pNext = pCurrent->pNext;
         }

         pP2PLink->bIsRegistered = W_FALSE;


         return;
      }
   }
}

/**
  * calls link establishment callback
  *
  * This function is  called after P2P RF link establishment
  * or when a new link establishment is requested and the P2P RF link is already established
  */
static void static_PP2PCallLinkEstablishmentCallback(
   tContext * pContext,
   tP2PLink * pP2PLink,
   W_ERROR    nError)
{
   PDebugTrace("static_PP2PCallLinkEstablishmentCallback : hLink %08x - %p %s", pP2PLink->hLink, pP2PLink, PUtilTraceError(nError));
   if (pP2PLink->bEstablishedCalled == W_FALSE)
   {
      /* mark the operation as completed */
      if (pP2PLink->hEstablishOperation != W_NULL_HANDLE)
      {
		 PBasicSetOperationCompleted(pContext, pP2PLink->hEstablishOperation);
         pP2PLink->hEstablishOperation = W_NULL_HANDLE;
      }

      if (nError == W_SUCCESS)
      {
		 PDFCPostContext3(&pP2PLink->sEstablishmentCC, pP2PLink->hLink, W_SUCCESS);
      }
      else
      {
		 PDFCPostContext3(&pP2PLink->sEstablishmentCC, W_NULL_HANDLE, nError);
         PDFCFlushCall(&pP2PLink->sReleaseCC);
      }
      pP2PLink->bEstablishedCalled = W_TRUE;
   }
}

/* See header */
void PP2PCallAllLinkEstablishmentCallbacks(
   tContext * pContext,
   W_ERROR    nError)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PLink     * pP2PLink;

   PDebugTrace("PP2PCallAllLinkEstablishmentCallbacks %s", PUtilTraceError(nError) );

   if (nError == W_SUCCESS)
   {
      /* The P2P link establishment succeeded */

      for (pP2PLink = pP2PInstance->pFirstP2PLink; pP2PLink != null; pP2PLink = pP2PLink->pNext)
      {
		 static_PP2PCallLinkEstablishmentCallback(pContext, pP2PLink, W_SUCCESS);
      }
   }
   else
   {
      /* The P2P link establishment failed */
      for (pP2PLink = pP2PInstance->pFirstP2PLink; pP2PLink != null; pP2PLink = pP2PInstance->pFirstP2PLink)
      {
		 static_PP2PUnregisterLink(pContext, pP2PLink);
         static_PP2PCallLinkEstablishmentCallback(pContext, pP2PLink, nError);
         static_PP2PDecrementLLCPUsageCount(pContext);
         PHandleClose(pContext, pP2PLink->hLink);

      }
   }
}

/**
  * calls link release callback
  *
  * This function is called when the P2P RF link is broken or in case of error
  *
  * nError = W_SUCCESS means the release callback is called due to user request
  * nError != W_SUCCESS means the release callback is called due to error
  */

static void static_PP2PCallLinkReleaseCallback(
   tContext * pContext,
   tP2PLink * pP2PLink,
   W_ERROR    nError)
{
   /* call only the release callback if the establishment callback has been already called */

   PDebugTrace("static_PP2PCallLinkReleaseCallback : hLink %08x - %p %s", pP2PLink->hLink, pP2PLink, PUtilTraceError(nError) );
   if (pP2PLink->bEstablishedCalled != W_FALSE)
   {
	  PDFCPostContext2(&pP2PLink->sReleaseCC, nError);
   }
   else
   {
      if (nError == W_SUCCESS)
      {
		 nError = W_ERROR_TIMEOUT;
      }
      static_PP2PCallLinkEstablishmentCallback(pContext, pP2PLink, nError);
   }
}

/**
  * This function calls the release callback of all the links registered
  */
void PP2PCallAllLinkReleaseCallbacks(
   tContext * pContext,
   W_ERROR    nError)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PLink * pP2PLink;

   PDebugTrace("static_PP2PCallLinkReleaseCallbacks : %s", PUtilTraceError(nError) );

   for (pP2PLink = pP2PInstance->pFirstP2PLink; pP2PLink != null; pP2PLink = pP2PInstance->pFirstP2PLink)
   {
      static_PP2PUnregisterLink(pContext, pP2PLink);

      if (pP2PLink->bEstablishedCalled != W_FALSE)
      {
         static_PP2PCallLinkReleaseCallback(pContext, pP2PLink, nError);

         if (pP2PLink->bIsDestroyPending == W_FALSE)
         {
            static_PP2PDecrementLLCPUsageCount(pContext);
         }
         else
         {
            CMemoryFree(pP2PLink);
         }
      }
      else
      {
         static_PP2PCallLinkReleaseCallback(pContext, pP2PLink, nError);

         if (pP2PLink->bIsDestroyPending == W_FALSE)
         {
            static_PP2PDecrementLLCPUsageCount(pContext);
         }
         PHandleClose(pContext, pP2PLink->hLink);
      }
   }
}

/**
  * Destroys a P2P link
  *
  * This function is called when the handle associated to the link is closed by user or internally
  */

static uint32_t static_PP2PDestroyLink( tContext* pContext, void* pObject )
{
   tP2PLink * pP2PLink = pObject;

   PDebugTrace("static_PP2PDestroyLink : hLink %08x - %p", pP2PLink->hLink, pP2PLink);

   if (pP2PLink->bIsRegistered)
   {
      /* mark the operation as completed */
      if (pP2PLink->hEstablishOperation != W_NULL_HANDLE)
      {
         PBasicSetOperationCompleted(pContext, pP2PLink->hEstablishOperation);
         pP2PLink->hEstablishOperation = W_NULL_HANDLE;
      }
      pP2PLink->bIsDestroyPending = W_TRUE;

      /* Decrement the LLCP usage count. This will stop the established LLCP link */
      static_PP2PDecrementLLCPUsageCount(pContext);
   }
   else
   {
      CMemoryFree(pObject);
   }

   return P_HANDLE_DESTROY_DONE;
}

tHandleType g_sP2PLink = { static_PP2PDestroyLink, null, null, null, null, null, null, null, null } ;

#define P_HANDLE_TYPE_P2P_LINK  (&g_sP2PLink)

W_ERROR PP2PGetLinkObject(tContext * pContext, W_HANDLE hLink, tP2PLink ** ppLink)
{
   return PHandleGetConnectionObject(pContext, hLink, P_HANDLE_TYPE_P2P_LINK, (void **) ppLink);
}


/**
  * Cancels a link establishment request
  *
  * This function is called when user calls WBasicCancelOperation
  */

static void static_P2PCancelLinkEstablishment(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tP2PLink * pP2PLink = pCancelParameter;

   PDebugTrace("static_P2PCancelLinkEstablishment : hLink %08x - %p", pP2PLink->hLink, pP2PLink );

   if (bIsClosing == W_FALSE)
   {
      /* Cancel of the establish link operation has been requested */

      if (pP2PLink->bIsRegistered != W_FALSE)
      {
         static_PP2PUnregisterLink(pContext, pP2PLink);
         static_PP2PDecrementLLCPUsageCount(pContext);
      }
      else
      {
         PDebugError("static_P2PCancelLinkEstablishment : cancel on a non registered link !!!!");
      }      
      static_PP2PCallLinkEstablishmentCallback(pContext, pP2PLink, W_ERROR_CANCEL);   

      /* close the handle associated to the link, this will free the link object */
      PHandleClose(pContext, pP2PLink->hLink);   
   }
   else
   {
      /* User closed the operation handle */
      pP2PLink->hEstablishOperation = W_NULL_HANDLE;
   }
}


/* See header */
W_HANDLE PP2PEstablishLinkDriver1Wrapper(
   tContext * pContext,
   tPBasicGenericHandleCallbackFunction* pEstablishmentCallback,
   void* pEstablishmentCallbackParameter)
{
   return PP2PEstablishLinkDriver1(pContext, pEstablishmentCallback, pEstablishmentCallbackParameter);
}

void static_PP2PEstablishLinkDriver1InternalCompleted( tContext* pContext, void * pCallbackParameter, W_HANDLE hHandle, W_ERROR nResult )
{
   tDFCDriverCCReference * ppDriverCC = (tDFCDriverCCReference *) pCallbackParameter;

   PDFCDriverPostCC3(* ppDriverCC, hHandle, nResult);
   CMemoryFree(ppDriverCC);
}

W_HANDLE PP2PEstablishLinkDriver1(
   tContext * pContext,
   tPBasicGenericHandleCallbackFunction* pEstablishmentCallback,
   void* pEstablishmentCallbackParameter
 )
{
   tDFCDriverCCReference * ppDriverCC = (tDFCDriverCCReference *) CMemoryAlloc(sizeof(tDFCDriverCCReference));
   W_HANDLE hHandle;

   if (ppDriverCC != null)
   {
      PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pEstablishmentCallback, pEstablishmentCallbackParameter, ppDriverCC);
      hHandle = PP2PEstablishLinkDriver1Internal(pContext, static_PP2PEstablishLinkDriver1InternalCompleted, ppDriverCC);
   }
   else
   {
      tDFCDriverCCReference pDriverCC;

      PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pEstablishmentCallback, pEstablishmentCallbackParameter, &pDriverCC);
      PDFCDriverPostCC3(pDriverCC, W_NULL_HANDLE, W_ERROR_OUT_OF_RESOURCE);
      hHandle = W_NULL_HANDLE;
   }

   return hHandle;
}

/* See WP2PEstablishLink */
W_HANDLE PP2PEstablishLinkDriver1Internal(
   tContext * pContext,
   tPBasicGenericHandleCallbackFunction* pEstablishmentCallback,
   void* pEstablishmentCallbackParameter
 )
{
   tDFCCallbackContext   sCallbackContext;
   tP2PLink            * pP2PLink = null;
   W_ERROR               nError;

   PDFCFillCallbackContext( pContext, (tDFCCallback*)pEstablishmentCallback, pEstablishmentCallbackParameter, & sCallbackContext);

   if (PP2PCheckP2PSupport(pContext) == W_FALSE)
   {
      nError = W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
      goto error;
   }

   pP2PLink = CMemoryAlloc(sizeof(tP2PLink));

   if (pP2PLink == null)
   {
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   CMemoryFill(pP2PLink, 0, sizeof(tP2PLink));

   nError = PHandleRegister(pContext, pP2PLink, P_HANDLE_TYPE_P2P_LINK, &pP2PLink->hLink);

   if (nError != W_SUCCESS)
   {
      goto error;
   }

   pP2PLink->sEstablishmentCC = sCallbackContext;

   return pP2PLink->hLink;

error:
   if (pP2PLink != null)
   {
      CMemoryFree(pP2PLink);
   }
   PDFCPostContext3(&sCallbackContext, W_NULL_HANDLE, nError);

   return W_NULL_HANDLE;
}

/* See header */
void PP2PEstablishLinkDriver2Wrapper(
   tContext * pContext,
   W_HANDLE hLink,
   tPBasicGenericCallbackFunction* pReleaseCallback,
   void* pReleaseCallbackParameter,
   W_HANDLE * phOperation)
{
   PP2PEstablishLinkDriver2(pContext, hLink, pReleaseCallback, pReleaseCallbackParameter, phOperation);
}


void static_PP2PEstablishLinkDriver2InternalCompleted( tContext* pContext, void * pCallbackParameter, W_ERROR nResult )
{
   tDFCDriverCCReference * ppDriverCC = (tDFCDriverCCReference *) pCallbackParameter;

   PDFCDriverPostCC2(* ppDriverCC, nResult);
   CMemoryFree(ppDriverCC);
}

void PP2PEstablishLinkDriver2(
   tContext * pContext,
   W_HANDLE hLink,
   tPBasicGenericCallbackFunction* pReleaseCallback,
   void* pReleaseCallbackParameter,
   W_HANDLE * phOperation)
{
   tDFCDriverCCReference * ppDriverCC = (tDFCDriverCCReference *) CMemoryAlloc(sizeof(tDFCDriverCCReference));

   if (ppDriverCC != null)
   {
      PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pReleaseCallback, pReleaseCallbackParameter, ppDriverCC);
      PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PP2PEstablishLinkDriver2InternalCompleted, ppDriverCC, phOperation);
   }
   else
   {
      tDFCDriverCCReference pDriverCC;

      if (phOperation != null)
      {
         * phOperation = W_NULL_HANDLE;
      }

      PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pReleaseCallback, pReleaseCallbackParameter, &pDriverCC);
      PDFCDriverPostCC2(pDriverCC, W_ERROR_OUT_OF_RESOURCE);
   }
}

/* See header */
void PP2PEstablishLinkDriver2Internal(
   tContext * pContext,
   W_HANDLE hLink,
   tPBasicGenericCallbackFunction* pReleaseCallback,
   void* pReleaseCallbackParameter,
   W_HANDLE * phOperation)
{
   tP2PInstance  * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = &pP2PInstance->sLLCPInstance;
   tP2PLink * pP2PLink = null;
   W_ERROR nError;

   if (phOperation != null)
   {
      * phOperation = W_NULL_HANDLE;
   }

   nError =  PHandleGetObject(pContext, hLink, P_HANDLE_TYPE_P2P_LINK, (void **) & pP2PLink);

   if ((nError != W_SUCCESS) || (pP2PLink == null))
   {
      /* Should not occur, internal error */
      PDebugError("PP2PEstablishLinkDriver2 : unable to retrieve link !!!! ");
      return;
   }

   PDebugTrace("PP2PEstablishLinkDriver2 : %08x %p", pP2PLink->hLink, pP2PLink);

   PDFCFillCallbackContext( pContext, (tDFCCallback*)pReleaseCallback, pReleaseCallbackParameter, & pP2PLink->sReleaseCC);

   if (phOperation != null)
   {
      * phOperation = pP2PLink->hEstablishOperation = PBasicCreateOperation(pContext, static_P2PCancelLinkEstablishment, pP2PLink);

      if (* phOperation == W_NULL_HANDLE)
      {
         /* unable to allocate the establish operation !!! */
         static_PP2PCallLinkEstablishmentCallback(pContext, pP2PLink, W_ERROR_OUT_OF_RESOURCE);
         PHandleClose(pContext, pP2PLink->hLink);
         return;
      }
   }

   /* Add the link to the P2P instance */
   static_PP2PRegisterLink(pContext, pP2PLink);
   static_PP2PIncrementLLCPUsageCount(pContext);

   if (pLLCPInstance->nRole != LLCP_ROLE_NONE)
   {
      /* If the LLCP is already connected, call the establishment callback now */
      static_PP2PCallLinkEstablishmentCallback(pContext, pP2PLink, W_SUCCESS);
   }
}

/* See WP2PGetLinkProperties */

W_ERROR PP2PGetLinkPropertiesDriver(
      tContext * pContext,
      W_HANDLE hLink,
      tWP2PLinkProperties * pProperties,
      uint32_t nLength)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;
   tP2PLink * pP2PLink;
   W_ERROR nError;

   if (PP2PCheckP2PSupport(pContext) == W_FALSE)
   {
      return W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
   }

   nError = PP2PGetLinkObject(pContext, hLink, &pP2PLink);
   if (nError != W_SUCCESS)
   {
      return nError;
   }

   if ((pProperties == null) || (nLength != sizeof(tWP2PLinkProperties)))
   {
      PDebugError("PP2PGetPropertiesDriver : pProperties == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pLLCPInstance->nRole == LLCP_ROLE_NONE)
   {
      PDebugError("PP2PGetLLCProperties : LLC is not established");
      return W_ERROR_BAD_STATE;
   }

   pProperties->nAgreedLLCPVersion = (pLLCPInstance->nAgreedVersionMajor) * 16 + pLLCPInstance->nAgreedVersionMinor;
   pProperties->nRemoteLLCPVersion = (pLLCPInstance->nRemoteVersionMajor) * 16 + pLLCPInstance->nRemoteVersionMinor;
   pProperties->nRemoteMIU = pLLCPInstance->nRemoteLinkMIU;
   pProperties->nRemoteLTO = pLLCPInstance->nRemoteTimeoutMs;
   pProperties->nRemoteWKS = pLLCPInstance->nRemoteWKS;
   pProperties->nBaudRate = pLLCPInstance->nBaudRate;

   pProperties->bIsInitiator = (pLLCPInstance->nRole == LLCP_ROLE_INITIATOR) ? W_TRUE : W_FALSE;

   return W_SUCCESS;
}


/* See header file */
uint8_t PP2PGetRFActivity(
                  tContext* pContext)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);

   if (pP2PInstance->nState == P_LLCP_STATE_STARTED)
   {
      if (pP2PInstance->sLLCPInstance.nRole == LLCP_ROLE_NONE)
      {
         return W_NFCC_RF_ACTIVITY_DETECTION;
      }
      else
      {
         return W_NFCC_RF_ACTIVITY_ACTIVE;
      }
   }
   else
   {
      return W_NFCC_RF_ACTIVITY_INACTIVE;
   }
}

/* See header */
void PP2PLinkDeactivationCallback(
   tContext * pContext,
   void * pCallbackParameter
   )
{
   PDebugTrace("PP2PLinkDeactivationCallback");

   PP2PLinkGeneralFailure(pContext, W_ERROR_TIMEOUT);
}

/*
 * Creates LLCP instance
 */

static void static_PP2PCreateLLCPInstance(
   tContext * pContext)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);

   PDebugTrace("PP2PCreateLLCPInstance");

   PP2PCleanAllSocketContexts(pContext);

   pP2PInstance->nTargetState = P_LLCP_STATE_STARTED;

   if (pP2PInstance->nState == P_LLCP_STATE_STOPPED)
   {
      PP2PInitiatorStartDetection(pContext);
   }
}

/**
 * Destroys LLCP instance
 */

static void static_PP2PDestroyLLCPInstance(
   tContext * pContext,
   W_ERROR    nError
   )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPPDUHeader * pPDU;

   PDebugTrace("PP2PDestroyLLCPInstance %s", PUtilTraceError(nError));

   /* Cancel the P2P timer */
   PMultiTimerCancel(pContext, TIMER_T9_P2P);

   /* Free all remaining PDUs */

   while ((pPDU = PLLCPFramerGetNextXmitPDU(pContext, W_FALSE)) != null)
   {
      PLLCPFreePDU(pContext, pPDU);
   }
   pP2PInstance->sLLCPInstance.nRole = LLCP_ROLE_NONE;

   pP2PInstance->nTargetState = P_LLCP_STATE_STOPPED;
   pP2PInstance->nLinkReleaseCause = nError;

   /* save the current error for later call */
   pP2PInstance->nLinkReleaseCause = nError;

   PDebugTrace("PP2PDestroyLLCPInstance[jim] pP2PInstance->nState= %d, nTargetState= %d", pP2PInstance->nState, pP2PInstance->nTargetState);

   if ((pP2PInstance->nState == P_LLCP_STATE_STARTED) || (pP2PInstance->nState == P_LLCP_STATE_ERROR))
   {
	      PP2PInitiatorStopDetection(pContext);
   }
}

/* See header */
void PP2PLinkGeneralFailure(
   tContext    * pContext,
   W_ERROR       nError
   )
{
   PDebugTrace("PP2PLinkGeneralFailure %s", PUtilTraceError(nError));

   static_PP2PDestroyLLCPInstance(pContext, nError);
}

/* See header */
void PP2PRemoteTimeoutExpiry(
   tContext * pContext,
   void     * pCallbackParameter
   )
{
   PDebugTrace("static_PP2PRemoteTimeoutExpiry");

   PP2PLinkGeneralFailure(pContext, W_ERROR_TIMEOUT);
}

/* See WP2PURILookup */
void PP2PURILookupDriver(
   tContext * pContext,
   W_HANDLE hLink,
   tPP2PURILookupCompleted* pCallback,
   void* pCallbackParameter,
   const char16_t* pServiceURI,
   uint32_t nSize)
{
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;
   tDFCDriverCCReference pDriverCC;
   tP2PLink * pP2PLink;
   W_ERROR nError;
   uint8_t * pUTF8ServiceURI = null;
   uint32_t nUTF8ServiceLength;
   tLLCPPDUHeader * pPDU;
   tDecodedPDU sPDUDescriptor;

   PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pDriverCC);

   if (((pNFCControllerInfo->nProtocolCapabilities & W_NFCC_PROTOCOL_READER_P2P_INITIATOR) == 0) ||
       ((pNFCControllerInfo->nProtocolCapabilities & W_NFCC_PROTOCOL_CARD_P2P_TARGET) == 0))
   {
      PDebugError("PP2PURILookupDriver : NFC controller is not P2P capable");
      nError = W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
      goto error;
   }

   nError = PP2PGetLinkObject(pContext, hLink, &pP2PLink);

   if (nError != W_SUCCESS)
   {
      PDebugError("PP2PURILookupDriver : can not retrieve link object");
      goto error;
   }

   if (pP2PInstance->sLLCPInstance.bURILookupInProgress != W_FALSE)
   {
      PDebugError("PP2PURILookupDriver : only one URI lookup at the same time");
      nError = W_ERROR_BAD_STATE;
      goto error;
   }

   if ((pP2PInstance->sLLCPInstance.nAgreedVersionMajor != LLCP_VERSION_11_MAJOR) || (pP2PInstance->sLLCPInstance.nAgreedVersionMinor < LLCP_VERSION_11_MINOR))
   {
      PDebugError("PP2PURILookupDriver : invalid LLCP version");
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto error;
   }

   if (pServiceURI == null)
   {
      PDebugError("PP2PURILookupDriver : invalid URI");
      nError = W_ERROR_BAD_PARAMETER;
      goto error;
   }

   nUTF8ServiceLength = PUtilConvertUTF16ToUTF8(null, pServiceURI, nSize / sizeof(char16_t));

   if (nUTF8ServiceLength > 255)
   {
      PDebugError("PP2PURILookupDriver : URI too long");
      nError = W_ERROR_BAD_PARAMETER;
      goto error;
   }

   pUTF8ServiceURI = CMemoryAlloc(nUTF8ServiceLength);

   if (pUTF8ServiceURI == null)
   {
      PDebugError("PP2PURILookupDriver : unable to allocate UTF8 URI");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   PUtilConvertUTF16ToUTF8(pUTF8ServiceURI, pServiceURI, nSize / sizeof(char16_t));

   pPDU = PLLCPAllocatePDU(pContext);

   if (pPDU == null)
   {
      PDebugError("PP2PURILookupDriver : unable to allocate PDU");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   pLLCPInstance->bURILookupInProgress = W_TRUE;
   pLLCPInstance->pURILookupCC = pDriverCC;

   PLLCPResetDescriptor(&sPDUDescriptor);

   sPDUDescriptor.nType = LLCP_PDU_SNL;
   sPDUDescriptor.nSSAP = LLCP_SDP_SAP;
   sPDUDescriptor.nDSAP = LLCP_SDP_SAP;
   sPDUDescriptor.sPayload.sParams.sSDREQ.bIsPresent = W_TRUE;
   sPDUDescriptor.sPayload.sParams.sSDREQ.nSNLength =  (uint8_t) nUTF8ServiceLength;
   sPDUDescriptor.sPayload.sParams.sSDREQ.pSN = pUTF8ServiceURI;
   sPDUDescriptor.sPayload.sParams.sSDREQ.nTID = pLLCPInstance->nCurrentTID;

   PLLCPBuildPDU(&sPDUDescriptor, &pPDU->nLength, pPDU->aPayload);
   PLLCPFramerEnterXmitPacket(pContext, pPDU);

   CMemoryFree(pUTF8ServiceURI);

   return;

error:

   if (pUTF8ServiceURI != null)
   {
      CMemoryFree(pUTF8ServiceURI);
   }

   PDFCDriverPostCC3(pDriverCC, 0, nError);
}

/* See header */
void PP2PCallURILookUpCallback(
   tContext * pContext,
   W_ERROR nError)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tLLCPInstance * pLLCPInstance = & pP2PInstance->sLLCPInstance;

   if (pLLCPInstance->bURILookupInProgress != W_FALSE)
   {
         PDFCDriverPostCC3(pLLCPInstance->pURILookupCC, (uint32_t) 0, nError);
         pLLCPInstance->bURILookupInProgress = W_FALSE;
         pLLCPInstance->nCurrentTID++;
   }
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
