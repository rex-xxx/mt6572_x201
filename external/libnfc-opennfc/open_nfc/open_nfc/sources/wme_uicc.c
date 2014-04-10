/*
 * Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
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
   Contains the UICC implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( UICC )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS

static uint32_t static_WUICCGetUiccSlot(void)
{
   uint32_t nSeNumber = 0;
   uint32_t nSlotIdentifier;

   if(WNFCControllerGetIntegerProperty(W_NFCC_PROP_SE_NUMBER, &nSeNumber) != W_SUCCESS)
   {
      return (uint32_t)-1;
   }

   for(nSlotIdentifier = 0; nSlotIdentifier < nSeNumber; nSlotIdentifier++)
   {
      tWSEInfoEx sSeInfo;

      if(WSEGetInfoEx(nSlotIdentifier, &sSeInfo) != W_SUCCESS)
      {
         return (uint32_t)-1;
      }

      if((sSeInfo.nCapabilities & W_SE_FLAG_UICC) != 0)
      {
         return nSlotIdentifier;
      }
   }

   return (uint32_t)-1;
}

static uint32_t static_PUICCGetUiccSlot(tContext * pContext)
{
   uint32_t nSeNumber = 0;
   uint32_t nSlotIdentifier;

   if(PNFCControllerGetIntegerProperty(pContext, W_NFCC_PROP_SE_NUMBER, &nSeNumber) != W_SUCCESS)
   {
      return (uint32_t)-1;
   }

   for(nSlotIdentifier = 0; nSlotIdentifier < nSeNumber; nSlotIdentifier++)
   {
      tWSEInfoEx sSeInfo;

      if(PSEGetInfoEx(pContext, nSlotIdentifier, &sSeInfo) != W_SUCCESS)
      {
         return (uint32_t)-1;
      }

      if((sSeInfo.nCapabilities & W_SE_FLAG_UICC) != 0)
      {
         return nSlotIdentifier;
      }
   }

   return (uint32_t)-1;
}


static uint32_t static_PUICCGetStatusCode(
            uint32_t nSeCode)
{
   uint32_t nSWPLinkStatus;

   switch(nSeCode)
   {
      case W_SE_SWP_STATUS_ERROR:
         nSWPLinkStatus = W_UICC_LINK_STATUS_ERROR;
         break;
      case W_SE_SWP_STATUS_INITIALIZATION:
         nSWPLinkStatus = W_UICC_LINK_STATUS_INITIALIZATION;
         break;
      case W_SE_SWP_STATUS_ACTIVE:
         nSWPLinkStatus = W_UICC_LINK_STATUS_ACTIVE;
         break;
      case W_SE_SWP_STATUS_NO_CONNECTION:
      case W_SE_SWP_STATUS_NO_SE:
      case W_SE_SWP_STATUS_DOWN:
      default:
         nSWPLinkStatus = W_UICC_LINK_STATUS_NO_UICC;
         break;
   }

   return nSWPLinkStatus;
}

W_ERROR WUICCGetSlotInfoSync(
            uint32_t* pnSWPLinkStatus)
{
   uint32_t nSlotIdentifier = static_WUICCGetUiccSlot();
   W_ERROR nError;
   bool_t bIsPresent;
   uint32_t nSWPLinkStatus;

   if(nSlotIdentifier == (uint32_t)-1)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   nError = WSEGetStatusSync(nSlotIdentifier, &bIsPresent, &nSWPLinkStatus);

   if(pnSWPLinkStatus != null)
   {
      *pnSWPLinkStatus = static_PUICCGetStatusCode(nSWPLinkStatus);
   }

   return nError;
}

static void static_PUICCGetSlotInfoSEGetStatusCompleted(
      tContext* pContext,
      void* pCallbackParameter,
      bool_t bIsPresent,
      uint32_t nSWPLinkStatus,
      W_ERROR nError )
{
   nSWPLinkStatus = static_PUICCGetStatusCode(nSWPLinkStatus);
   PDFCPostContext3((tDFCCallbackContext*)pCallbackParameter, nSWPLinkStatus, nError);
   CMemoryFree(pCallbackParameter);
}

void PUICCGetSlotInfo(
      tContext* pContext,
      tPUICCGetSlotInfoCompleted * pCallback,
      void * pCallbackParameter )
{
   tDFCCallbackContext* pCallbackContext = (tDFCCallbackContext*)CMemoryAlloc(sizeof(tDFCCallbackContext));
   uint32_t nSlotIdentifier = static_PUICCGetUiccSlot(pContext);
   W_ERROR nError = W_SUCCESS;

   if(nSlotIdentifier == (uint32_t)-1)
   {
      nError = W_ERROR_BAD_PARAMETER;
   }
   else if (pCallbackContext == null)
   {
      nError = W_ERROR_OUT_OF_RESOURCE;
   }
   else
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PSEGetStatus(pContext, nSlotIdentifier, static_PUICCGetSlotInfoSEGetStatusCompleted, pCallbackContext);
   }

   if(nError != W_SUCCESS)
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, nError);
      CMemoryFree(pCallbackContext);
   }
}

W_ERROR WUICCActivateSWPLineSync( void )
{
   uint32_t nSlotIdentifier = static_WUICCGetUiccSlot();

   if(nSlotIdentifier != (uint32_t)-1)
   {
      (void)WNFCControllerActivateSwpLine(nSlotIdentifier);
   }

   return W_SUCCESS;
}

/* See client API */
void PUICCActivateSWPLine(
   tContext* pContext,
   tPBasicGenericCallbackFunction * pCallback,
   void * pCallbackParameter )
{
   tDFCCallbackContext sCallbackContext;
   uint32_t nSlotIdentifier = static_PUICCGetUiccSlot(pContext);

   if(nSlotIdentifier != (uint32_t)-1)
   {
      (void)PNFCControllerActivateSwpLine(pContext, nSlotIdentifier);
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, W_SUCCESS);
}

void WUICCGetAccessPolicy(
      uint32_t nStorageType,
      tWUICCAccessPolicy* pAccessPolicy)
{
   uint32_t nSlotIdentifier = static_WUICCGetUiccSlot();
   tWSEInfoEx sSEInfo;
   uint32_t nProtocols;

   if((pAccessPolicy == null)
   || ((nStorageType != W_NFCC_STORAGE_PERSISTENT) && (nStorageType != W_NFCC_STORAGE_VOLATILE)))
   {
      return;
   }

   if(nSlotIdentifier == (uint32_t)-1)
   {
      return;
   }

   if(WSEGetInfoEx(nSlotIdentifier, &sSEInfo) == W_SUCCESS)
   {
      if(nStorageType == W_NFCC_STORAGE_VOLATILE)
      {
         nProtocols = sSEInfo.nVolatilePolicy;
      }
      else
      {
         nProtocols = sSEInfo.nPersistentPolicy;
      }

      pAccessPolicy->nReaderISO14443_4_A_Priority = ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_4_A) != 0)?W_PRIORITY_EXCLUSIVE:W_PRIORITY_NO_ACCESS;
      pAccessPolicy->nReaderISO14443_4_B_Priority = ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_4_B) != 0)?W_PRIORITY_EXCLUSIVE:W_PRIORITY_NO_ACCESS;
      pAccessPolicy->nCardISO14443_4_A_Priority = ((nProtocols & W_NFCC_PROTOCOL_CARD_ISO_14443_4_A) != 0)?W_PRIORITY_EXCLUSIVE:W_PRIORITY_NO_ACCESS;
      pAccessPolicy->nCardISO14443_4_B_Priority = ((nProtocols & W_NFCC_PROTOCOL_CARD_ISO_14443_4_B) != 0)?W_PRIORITY_EXCLUSIVE:W_PRIORITY_NO_ACCESS;
      pAccessPolicy->nCardBPrime_Priority = ((nProtocols & W_NFCC_PROTOCOL_CARD_BPRIME) != 0)?W_PRIORITY_EXCLUSIVE:W_PRIORITY_NO_ACCESS;
      pAccessPolicy->nCardFeliCa_Priority = ((nProtocols & W_NFCC_PROTOCOL_CARD_FELICA) != 0)?W_PRIORITY_EXCLUSIVE:W_PRIORITY_NO_ACCESS;
   }
}

W_ERROR WUICCSetAccessPolicySync(
               uint32_t nStorageType,
               const tWUICCAccessPolicy* pAccessPolicy)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WUICCSetAccessPolicy( nStorageType, pAccessPolicy, PBasicGenericSyncCompletion, &param );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See client API */
void WUICCSetAccessPolicy(
      uint32_t nStorageType,
      const tWUICCAccessPolicy * pAccessPolicy,
      tWBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   uint32_t nSlotIdentifier = static_WUICCGetUiccSlot();
   uint32_t nProtocols = 0;

   if (pAccessPolicy == null)
   {
      PDebugError("WUICCSetAccessPolicy : pAccessPolicy is null");
      return;
   }

   if(nSlotIdentifier == (uint32_t)-1)
   {
      return;
   }

   if(pAccessPolicy->nReaderISO14443_4_A_Priority != W_PRIORITY_NO_ACCESS)
   {
      nProtocols |= W_NFCC_PROTOCOL_READER_ISO_14443_4_A;
   }
   if(pAccessPolicy->nReaderISO14443_4_B_Priority != W_PRIORITY_NO_ACCESS)
   {
      nProtocols |= W_NFCC_PROTOCOL_READER_ISO_14443_4_B;
   }
   if(pAccessPolicy->nCardISO14443_4_A_Priority != W_PRIORITY_NO_ACCESS)
   {
      nProtocols |= W_NFCC_PROTOCOL_CARD_ISO_14443_4_A;
   }
   if(pAccessPolicy->nCardISO14443_4_B_Priority != W_PRIORITY_NO_ACCESS)
   {
      nProtocols |= W_NFCC_PROTOCOL_CARD_ISO_14443_4_B;
   }
   if(pAccessPolicy->nCardBPrime_Priority != W_PRIORITY_NO_ACCESS)
   {
      nProtocols |= W_NFCC_PROTOCOL_CARD_BPRIME;
   }
   if(pAccessPolicy->nCardFeliCa_Priority != W_PRIORITY_NO_ACCESS)
   {
      nProtocols |= W_NFCC_PROTOCOL_CARD_FELICA;
   }

   WSESetPolicy(nSlotIdentifier, nStorageType, nProtocols, pCallback, pCallbackParameter );
}

/* See Client API Specification */
uint32_t WUICCGetTransactionEventAID(
                  uint8_t* pBuffer,
                  uint32_t nBufferLength)
{
   uint32_t nSlotIdentifier = static_WUICCGetUiccSlot();

   if(nSlotIdentifier == (uint32_t)-1)
   {
      return 0;
   }

   return WSEGetTransactionAID(nSlotIdentifier, pBuffer, nBufferLength);
}

/* See Client API Specification */
W_ERROR WUICCGetConnectivityEventParameter(
            uint8_t* pDataBuffer,
            uint32_t nBufferLength,
            uint32_t* pnActualDataLength)
{
   uint32_t nSlotIdentifier = static_WUICCGetUiccSlot();

   if(nSlotIdentifier == (uint32_t)-1)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   return WSEGetConnectivityEventParameter(nSlotIdentifier, pDataBuffer, nBufferLength, pnActualDataLength);
}

/* See Client API Specification */
W_ERROR WUICCMonitorConnectivityEvent(
            tWUICCMonitorConnectivityEventHandler* pHandler,
            void* pHandlerParameter,
            W_HANDLE* phEventRegistry)
{
   uint32_t nSlotIdentifier = static_WUICCGetUiccSlot();

   if(nSlotIdentifier == (uint32_t)-1)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   return WSEMonitorConnectivityEvent(nSlotIdentifier, (tWBasicGenericEventHandler2*)pHandler, pHandlerParameter, phEventRegistry);
}

/* See Client API Specification */
W_ERROR WUICCMonitorTransactionEvent(
            tWUICCMonitorTransactionEventHandler* pHandler,
            void* pHandlerParameter,
            W_HANDLE* phEventRegistry)
{
   uint32_t nSlotIdentifier = static_WUICCGetUiccSlot();

   if(nSlotIdentifier == (uint32_t)-1)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   return WSEMonitorEndOfTransaction(nSlotIdentifier, (tWBasicGenericEventHandler2*)pHandler, pHandlerParameter, phEventRegistry);
}

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
