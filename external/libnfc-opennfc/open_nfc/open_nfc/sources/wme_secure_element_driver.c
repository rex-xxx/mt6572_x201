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

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* The watchdog timeout in ms */
#define P_SE_WATCHDOG_TIMEOUT   1000

static P_INLINE uint32_t static_PSESwitchGetPolicy(
            const tNFCControllerSEPolicy* pPolicy,
            uint32_t nSlotIdentifier)
{
   if(pPolicy->nSlotIdentifier == nSlotIdentifier)
   {
	  return pPolicy->nSEProtocolPolicy;
   }

   return 0;
}

static void static_PSEMonitorEndOfTransactionEventReceived(
         tContext* pContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         const uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nNALMessageReceptionCounter);

static void static_PSEMonitorEndOfTransactionProcessAIDListCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError);

#ifdef P_INCLUDE_SE_SECURITY
static void static_PSEMonitorUpdateAclCloseConnectionCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   PDebugTrace("static_PSEMonitorUpdateAclCloseConnectionCompleted()");

   /* Ignore error */
   static_PSEMonitorEndOfTransactionProcessAIDListCompleted(pContext, pCallbackParameter, W_SUCCESS);
}
#endif

#ifdef P_INCLUDE_SE_SECURITY
static void static_PSEMonitorUpdateAclOpenConnectionCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_HANDLE hConnection,
         W_ERROR nResult)
{
   PDebugTrace("static_PSEMonitorUpdateAclOpenConnectionCompleted()");

   PBasicCloseHandleSafe(pContext, hConnection, static_PSEMonitorUpdateAclCloseConnectionCompleted, pCallbackParameter);
}
#endif

/** Initialize a Secure Element */
static void static_PSEOpenConnection(
                  tContext* pContext,
                  uint32_t nSlotIdentifier,
                  bool_t bForce,
                  bool_t bFromUser,
                  tPBasicGenericHandleCallbackFunction* pCallback,
                  void* pCallbackParameter );

#ifdef P_INCLUDE_SE_SECURITY
/** Open & close the connection to force the update of the ACL */
static void static_PSEMonitorUpdateAclAndCallProcessAIDListCompleted(
         tContext* pContext,
         tSESlot* pSlot)
{
   tSEInstance* pSEInstance = PContextGetSEInstance(pContext);
   uint32_t nSlotIdentifier = (uint32_t)(pSlot - pSEInstance->pSlotArray);

   PDebugTrace("static_PSEMonitorUpdateAclAndCallProcessAIDListCompleted()");

   if (pSlot->hDriverConnection == W_NULL_HANDLE)
   {
      static_PSEOpenConnection(
                  pContext, nSlotIdentifier, W_TRUE, W_FALSE,
                  static_PSEMonitorUpdateAclOpenConnectionCompleted, pSlot );
   }
   else
   {
      PSecurityStackLoadAcl(pContext,
         pSlot->pSecurityStackInstance,
         static_PSEMonitorUpdateAclCloseConnectionCompleted, pSlot );
   }
}
#endif

/* The pending operation flags */
#define  P_SE_FLAG_PENDING_SET_POLICY                 0x0001
#define  P_SE_FLAG_PENDING_COMMUNICATION              0x0002
#define  P_SE_FLAG_PENDING_EOT_REGITRATION            0x0004
#define  P_SE_FLAG_PENDING_HOTPLUG_REGISTRATION       0x0008
#define  P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER    0x0010
#define  P_SE_FLAG_PENDING_OPERATION                  0x0020
#define  P_SE_FLAG_PENDING_OPERATION_LOGICAL_CHANNEL  0x0040
#define  P_SE_FLAG_PENDING_JUPITER                    0x0080
#define  P_SE_FLAG_PENDING_SET_SWP_POLICY_PERSISTENT  0x0100
#define  P_SE_FLAG_PENDING_SET_SWP_POLICY_VOLATILE    0x0200
#define  P_SE_FLAG_PENDING_CONNECTIVITY_REGITRATION   0x0400
#define  P_SE_FLAG_PENDING_ACTIVATE_SWP               0x0800
#ifdef P_INCLUDE_GET_TRANSACTION_AID
#define  P_SE_FLAG_PENDING_END_OF_TRANSACTION_EVENT   0x1000
#define  P_SE_FLAG_PENDING_GET_TRANSACTION_AID        0x2000
#endif /* P_INCLUDE_GET_TRANSACTION_AID */

static void static_PSEMonitorConnectivityEventReceived(
         tContext* pContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         const uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nNALMessageReceptionCounter)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;
   uint32_t nMessageCode;

   PDebugTrace("static_PSEMonitorConnectivityEventReceived()");

   CDebugAssert(nEventIdentifier == NAL_EVT_UICC_CONNECTIVITY);
   if((pBuffer == null) || (nLength == 0))
   {
      PDebugError("static_PSEMonitorConnectivityEventReceived: Error of protocol");
      /* Can't do anything about it, just return */
      return;
   }

   nMessageCode = pBuffer[0];

   if(nMessageCode == 0x10 /* EVT_CONNECTIVITY */)
   {
      /*
         This event notifies the terminal host that it shall send a "HCI connectivity event"
         as defined in ETSI TS 102 223: "Smart Cards; Card Application Toolkit (CAT)".
         This event has no parameters
      */
#ifdef P_INCLUDE_SE_SECURITY
      PSeHalTriggerStkPolling(pContext, pSlot->nHalSlotIdentifier);
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
   }
   else if(nMessageCode == 0x12 /* EVT_TRANSACTION */)
   {
      if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_EOT_REGITRATION) != 0)
      {
         uint32_t nRemaingBytes;

         /* Reset the sizes */
         pSlot->sMonitorEndOfTransaction.nLastAidListLength =  0;
         pSlot->sConnectivity.nDataLength = 0;

         /* BER-TLV parser */

         /* skip Message Code */
         nRemaingBytes = nLength - 1;
         pBuffer++;

         while(nRemaingBytes >= 2)
         {
            uint8_t nType = *pBuffer++;  /* Parsing only 1-byte types */
            uint32_t nDataLength = *pBuffer++;
            nRemaingBytes -= 2;

            if(nDataLength > 127)
            {
               if((nDataLength != 0x81) || (nRemaingBytes == 0))
               {
                  /* Only length <= 255 are supported */
                  PDebugError("static_PSEMonitorConnectivityEventReceived: Error in the BER-TLV format sent by the UICC");
                  break;
               }
               nDataLength = *pBuffer++;
               nRemaingBytes--;
            }

            if(nDataLength > nRemaingBytes)
            {
               PDebugError("static_PSEMonitorConnectivityEventReceived: Error in the BER-TLV format sent by the UICC");
               break;
            }

            if(nType == 0x81) /* AID */
            {
               pSlot->sMonitorEndOfTransaction.nLastAidListLength = nDataLength + 1;

               if(pSlot->sMonitorEndOfTransaction.nLastAidListLength > P_SE_MAX_AID_LIST_LENGTH)
               {
                  PDebugWarning("static_PSEMonitorConnectivityEventReceived: AID too long (%d bytes), truncated to %d bytes",
                     nDataLength, P_SE_MAX_AID_LIST_LENGTH - 1);
                  pSlot->sMonitorEndOfTransaction.nLastAidListLength = P_SE_MAX_AID_LIST_LENGTH;
               }

               if(pSlot->sMonitorEndOfTransaction.nLastAidListLength > 1)
               {
                  pSlot->sMonitorEndOfTransaction.aLastAidList[0] = (uint8_t)(pSlot->sMonitorEndOfTransaction.nLastAidListLength - 1);
                  CMemoryCopy(&pSlot->sMonitorEndOfTransaction.aLastAidList[1], pBuffer, pSlot->sMonitorEndOfTransaction.nLastAidListLength - 1);
               }
            }
            else if(nType == 0x82) /* Parameters */
            {
               pSlot->sConnectivity.nDataLength = nDataLength;

               if(pSlot->sConnectivity.nDataLength > P_SE_MAX_CONNECTIVITY_DATA_LENGTH)
               {
                  PDebugWarning("static_PSEMonitorConnectivityEventReceived: parameter too long (%d bytes), truncated to %d bytes",
                     pSlot->sConnectivity.nDataLength, P_SE_MAX_CONNECTIVITY_DATA_LENGTH);
                  pSlot->sConnectivity.nDataLength = P_SE_MAX_CONNECTIVITY_DATA_LENGTH;
               }

               if(pSlot->sConnectivity.nDataLength != 0)
               {
                  CMemoryCopy(pSlot->sConnectivity.aDataBuffer,
                     pBuffer, pSlot->sConnectivity.nDataLength);
               }
            }
            else
            {
               PDebugWarning(
                  "static_PSEMonitorConnectivityEventReceived: unknown data type %02X, ignored", nType);
            }

            pBuffer += nDataLength;
            nRemaingBytes -= nDataLength;
         }

         pSlot->sMonitorEndOfTransaction.nCardProtocols = 0;

#ifdef P_INCLUDE_SE_SECURITY
         if ((pSlot->sMonitorEndOfTransaction.nLastAidListLength > P_7816SM_MIN_AID_LENGTH) && (pSlot->pSecurityStackInstance != null))
         {
            /* Filter the AID and send the event */
            static_PSEMonitorUpdateAclAndCallProcessAIDListCompleted(pContext, pSlot);
         }
#else
         /* Send the event */
         static_PSEMonitorEndOfTransactionProcessAIDListCompleted(pContext, pSlot, W_SUCCESS);
#endif /* P_INCLUDE_SE_SECURITY */
      }
   }
   else if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_CONNECTIVITY_REGITRATION) != 0)
   {
      pSlot->sConnectivity.nDataLength = nLength - 1;

      if(pSlot->sConnectivity.nDataLength > P_SE_MAX_CONNECTIVITY_DATA_LENGTH)
      {
         PDebugWarning("static_PSEMonitorConnectivityEventReceived: Message 0x%02X too long (%d bytes), truncated to %d bytes",
            nMessageCode, pSlot->sConnectivity.nDataLength, P_SE_MAX_CONNECTIVITY_DATA_LENGTH);
         pSlot->sConnectivity.nDataLength = P_SE_MAX_CONNECTIVITY_DATA_LENGTH;
      }

      if(pSlot->sConnectivity.nDataLength != 0)
      {
         CMemoryCopy(pSlot->sConnectivity.aDataBuffer,
            &pBuffer[1], pSlot->sConnectivity.nDataLength);
      }

      PDFCDriverPostCC3( pSlot->sConnectivity.pMonitorConnectivityDriverCC,
         nMessageCode, pSlot->sConnectivity.nDataLength );
   }
}

/* See API documentation */
W_ERROR PSEGetConnectivityEventParameter(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            uint8_t* pDataBuffer,
            uint32_t nBufferLength,
            uint32_t* pnActualDataLength)
{
   W_ERROR nError = W_SUCCESS;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot;

   PDebugTrace("PSEGetConnectivityEventParameter()");

   if(pnActualDataLength != null)
   {
      *pnActualDataLength = 0;
   }

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEGetConnectivityEventParameter: bad NFC Controller mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEGetConnectivityEventParameter: Wrong SE slot identifier");
      return W_ERROR_BAD_PARAMETER;
   }
   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   /* Check access */
   if((pSlot->nPendingFlags &
      (P_SE_FLAG_PENDING_EOT_REGITRATION | P_SE_FLAG_PENDING_CONNECTIVITY_REGITRATION)) == 0)
   {
      PDebugError("PSEGetConnectivityEventParameter: no monitor active");
      nError = W_ERROR_BAD_STATE;
      goto return_function;
   }

   if(nBufferLength < pSlot->sConnectivity.nDataLength)
   {
      PDebugError("PSEGetConnectivityEventParameter: buffer too short");
      nError = W_ERROR_BUFFER_TOO_SHORT;
      goto return_function;
   }

   if(pDataBuffer != null)
   {
      CMemoryCopy(pDataBuffer, &pSlot->sConnectivity.aDataBuffer, pSlot->sConnectivity.nDataLength);
   }

return_function:

   if(pnActualDataLength != null)
   {
      if((nError == W_SUCCESS) || (nError == W_ERROR_BUFFER_TOO_SHORT))
      {
         *pnActualDataLength = pSlot->sConnectivity.nDataLength;
      }
   }

   return nError;
}

/* Destroy the monitor connectivity event registry */
static uint32_t static_PSEDestroyMonitorConnectivity(
            tContext* pContext,
            void* pObject )
{
   tSESlot* pSlot = P_HANDLE_GET_STRUCTURE(tSESlot, pObject, sConnectivity.sObjectHeaderRegistry);

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_CONNECTIVITY_REGITRATION) != 0);

   PDFCDriverFlushCall(pSlot->sConnectivity.pMonitorConnectivityDriverCC);

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_CONNECTIVITY_REGITRATION;

   return P_HANDLE_DESTROY_DONE;
}

/* monitor connectivity registry type */
tHandleType g_sSEMonitorConnectivity = { static_PSEDestroyMonitorConnectivity, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_SE_MONITOR_CONNECTIVITY (&g_sSEMonitorConnectivity)

/* See API documentation */
W_ERROR PSEMonitorConnectivityEvent(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            tPBasicGenericEventHandler2* pHandler,
            void* pHandlerParameter,
            W_HANDLE* phEventRegistry)
{
   W_ERROR nError = W_SUCCESS;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot;

   PDebugTrace("PSEMonitorConnectivityEvent()");

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEMonitorConnectivityEvent: bad NFC Controller mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   if((phEventRegistry == null) /* || (pHandler == null) @todo: fix autogen */)
   {
      PDebugError("PSEMonitorConnectivityEvent: bad parameters");
      return W_ERROR_BAD_PARAMETER;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEMonitorConnectivityEvent: Wrong SE slot identifier");
      return W_ERROR_BAD_PARAMETER;
      goto return_error;
   }
   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   /* Check the slot capability */
   if(PSeHalIsSwpSlot(pSlot->nHalSlotIdentifier) == W_FALSE)
   {
      PDebugWarning("PSEMonitorConnectivityEvent: Connectivity only supported on SWP");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   /* This flag is needed to protect the operation against re-entrantcy */
   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_CONNECTIVITY_REGITRATION) != 0)
   {
      PDebugError("PSEMonitorConnectivityEvent: service already used");
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   if((nError = PHandleRegister(pContext,
            &pSlot->sConnectivity.sObjectHeaderRegistry,
            P_HANDLE_TYPE_SE_MONITOR_CONNECTIVITY, phEventRegistry)) != W_SUCCESS)
   {
      PDebugError("PSEMonitorConnectivityEvent: error registering the monitor object");
      goto return_error;
   }

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pHandler, pHandlerParameter,
      &pSlot->sConnectivity.pMonitorConnectivityDriverCC );

   pSlot->nPendingFlags |= P_SE_FLAG_PENDING_CONNECTIVITY_REGITRATION;

   return W_SUCCESS;

return_error:

   PDebugError("PSEMonitorConnectivityEvent: return the error %s", PUtilTraceError(nError));

   *phEventRegistry = W_NULL_HANDLE;
   return nError;
}

static void static_PSE14P4SmRawExchangeApduCompletion(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError )
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSE14P4SmRawExchangeApduCompletion: Failed with error %s", PUtilTraceError(nError));
   }

   PDFCPostContext3(
         &pSlot->sExchange.sCallbackContext,
         nDataLength, nError );
}

static void static_PSE14P4SmRawExchangeApdu(
                  tContext* pContext,
                  tP7816SmRawInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength)
{
   tSESlot* pSlot = (tSESlot*)pInstance;

   PDFCFillCallbackContext(
      pContext, (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->sExchange.sCallbackContext );

   CDebugAssert(pSlot->hDriverConnection != W_NULL_HANDLE);

   (void)P14P4DriverExchangeDataInternal(
      pContext,
      pSlot->hDriverConnection,
      static_PSE14P4SmRawExchangeApduCompletion,
      pSlot,
      pSendApduBuffer, nSendApduBufferLength,
      pReceivedApduBuffer, nReceivedApduBufferMaxLength,
      W_FALSE, 0,
      W_FALSE,
      W_FALSE);
}

static tP7816SmRawInterface g_sPSE14P4SmRawInterface =
{
   /** The function that is called to perform an APDU exchange */
   static_PSE14P4SmRawExchangeApdu
};

/**
 * Builds the SE information.
 *
 * @param[in]  pContext  The current context
 *
 * @param[in]  pSEInstance  The SE instance
 *
 * @return  The result error code.
 **/
static W_ERROR static_PSEBuildInfo(
            tContext* pContext,
            tSEInstance* pSEInstance)
{
   W_ERROR nError = W_SUCCESS;

   PDebugTrace("static_PSEBuildInfo()");

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("static_PSEBuildInfo: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_function;
   }

   if(pSEInstance->bSEInfoIsBuilt == W_FALSE)
   {
      uint32_t nSeNumber = PNFCControllerGetSecureElementNumber(pContext);
      uint32_t nSlotIdentifier;
      tSESlot* pSlot;
      tNFCControllerSEPolicy sPersistentPolicy;
      tNFCControllerSEPolicy sVolatilePolicy;

      pSEInstance->nSeNumber = 0;

      if(pSEInstance->pSlotArray != null)
      {
         CMemoryFree(pSEInstance->pSlotArray);
         pSEInstance->pSlotArray = null;
      }

      nError = PNFCControllerGetSESwitchPosition(pContext,
         &sPersistentPolicy, &sVolatilePolicy, null, null);
      if(nError != W_SUCCESS)
      {
         PDebugError("static_PSEBuildInfo: Error %s returned by PNFCControllerGetSESwitchPosition()",
            PUtilTraceError(nError));
         goto return_function;
      }

      if(nSeNumber != 0)
      {
         pSEInstance->pSlotArray = (tSESlot*)CMemoryAlloc(sizeof(tSESlot)* nSeNumber);
         if(pSEInstance->pSlotArray == null)
         {
            PDebugError("static_PSEBuildInfo: Cannot allocate the slot array");
            nError = W_ERROR_OUT_OF_RESOURCE;
            goto return_function;
         }
         CMemoryFill(pSEInstance->pSlotArray, 0, sizeof(tSESlot)* nSeNumber);
      }
      pSEInstance->nSeNumber = nSeNumber;

      for(nSlotIdentifier = 0; nSlotIdentifier < nSeNumber; nSlotIdentifier++)
      {
         tWSEInfoEx* pConstantInfo;
         uint32_t nConstantProtocols;

         pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

         pConstantInfo = &pSlot->sConstantInfo;
         nError = PNFCControllerGetSecureElementHardwareInfo(pContext, nSlotIdentifier,
            pConstantInfo->aDescription,
            &pConstantInfo->nCapabilities,
            &nConstantProtocols,
            &pSlot->nHalSlotIdentifier);

         if(nError != W_SUCCESS)
         {
            PDebugError("static_PSEBuildInfo: Error %s returned by PNFCControllerGetSecureElementHardwareInfo()",
               PUtilTraceError(nError));
            goto return_function;
         }

         pSlot->nConstantProtocols = nConstantProtocols;

         pConstantInfo->nSlotIdentifier = nSlotIdentifier;

         if((pConstantInfo->nCapabilities & W_SE_FLAG_REMOVABLE) != 0)
         {
            /* The SE is not yet detected */
            pSlot->sStatus.bIsPresent = W_FALSE;
         }
         else
         {
            /* The flag is always set to W_TRUE for a non-removable SE. */
            pSlot->sStatus.bIsPresent = W_TRUE;
         }

         /* Special process for SWP */
         if(PSeHalIsSwpSlot(pSlot->nHalSlotIdentifier) != W_FALSE)
         {
            nError = PNFCControllerGetSwpAccessPolicy(pContext,
                  W_NFCC_STORAGE_PERSISTENT,
                  &pConstantInfo->nPersistentPolicy);

            if(nError != W_SUCCESS)
            {
               PDebugError("static_PSEBuildInfo: Error %s returned by PNFCControllerGetSwpAccessPolicy()",
                  PUtilTraceError(nError));
               goto return_function;
            }

            PDebugTrace("SWP SE#%d - card protocol: %s", nSlotIdentifier, PUtilTraceCardProtocol(pContext, pConstantInfo->nPersistentPolicy));
            PDebugTrace("SWP SE#%d - reader protocol: %s", nSlotIdentifier, PUtilTraceReaderProtocol(pContext, pConstantInfo->nPersistentPolicy));

            /* Copy the persistent policy into the volatile policy */
            pConstantInfo->nVolatilePolicy = pConstantInfo->nPersistentPolicy;

            /* Register for the connectivity events */
            PNALServiceRegisterForEvent(
                  pContext,
                  NAL_SERVICE_UICC,
                  NAL_EVT_UICC_CONNECTIVITY,
                  &pSlot->sConnectivity.sMonitorConnectivityOperation,
                  static_PSEMonitorConnectivityEventReceived,
                  pSlot );

            /* SWP support always the end-of-transaction events */
            pConstantInfo->nCapabilities |= W_SE_FLAG_END_OF_TRANSACTION_NOTIFICATION;

            pConstantInfo->nSupportedProtocols = nConstantProtocols;
         }
         /* Special process for Proprietary slots */
         else if(PSeHalIsProprietarySlot(pSlot->nHalSlotIdentifier) != W_FALSE)
         {
            /* Limit the visible protocols to the card emeulation */
            pConstantInfo->nSupportedProtocols = nConstantProtocols & W_NFCC_PROTOCOL_CARD_ALL;

            pConstantInfo->nPersistentPolicy = static_PSESwitchGetPolicy(&sPersistentPolicy, nSlotIdentifier);

            pConstantInfo->nVolatilePolicy = static_PSESwitchGetPolicy(&sVolatilePolicy, nSlotIdentifier);
         }
         /* Special process for Standalone slots */
         else if(PSeHalIsStandaloneSlot(pSlot->nHalSlotIdentifier) != W_FALSE)
         {
            CDebugAssert(nConstantProtocols == 0);
            pConstantInfo->nSupportedProtocols = 0;
            pConstantInfo->nPersistentPolicy = 0;
            pConstantInfo->nVolatilePolicy = 0;
         }
         else
         {
            CDebugAssert(W_FALSE);
         }

         if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_RF) != 0)
         {
            pSlot->s7816Sm.pLowLevelSmInterface = &g_sP7816SmInterface;

            nError = P7816SmCreateInstance(pContext, &g_sPSE14P4SmRawInterface,
               (tP7816SmRawInstance*)pSlot, &pSlot->s7816Sm.pLowLevelSmInstance);
         }
#ifdef P_INCLUDE_SE_SECURITY
         else if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION) != 0)
         {
            pSlot->s7816Sm.pLowLevelSmInterface = &g_sPSeHalSmInterface;

            nError = PSeHalSmCreateInstance(pContext, pSlot,
               (tPSeHalSmInstance**)&pSlot->s7816Sm.pLowLevelSmInstance);
         }
#endif /* P_INCLUDE_SE_SECURITY */

         if (nError != W_SUCCESS)
         {
            PDebugError("static_PSEBuildInfo: Error returned by P7816SmCreateInstance()");

            nError = W_ERROR_OUT_OF_RESOURCE;
            goto return_function;
         }

         /* Also set the high level interface/instance (may be superseded by the Security Stack) */
         pSlot->s7816Sm.pSmInterface = pSlot->s7816Sm.pLowLevelSmInterface;
         pSlot->s7816Sm.pSmInstance = pSlot->s7816Sm.pLowLevelSmInstance;

#ifdef P_INCLUDE_SE_SECURITY
         /* Activate the security stack */
         if((pConstantInfo->nCapabilities & W_SE_FLAG_COMMUNICATION) != 0)
         {
            const tCSecurityDefaultPrincipal* pDefaultPrincipalList;
            uint32_t nDefaultPrincipalNumber;

            if(CSecurityGetConfiguration(
               pSlot->nHalSlotIdentifier,
               &pDefaultPrincipalList,
               &nDefaultPrincipalNumber) != W_FALSE)
            {
               uint32_t nUpdateStrategy;

               /* Compute the update strategy */
               nUpdateStrategy = P_SECSTACK_UPDATE_MASTER;
               if(((pConstantInfo->nCapabilities & W_SE_FLAG_SE_HAL) != 0)
               && ((pConstantInfo->nCapabilities & W_SE_FLAG_UICC) != 0))
               {
                  if(((pConstantInfo->nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_RF) == 0)
                  && ((pConstantInfo->nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_SWP) == 0))
                  {
                     if((pConstantInfo->nCapabilities & W_SE_FLAG_STK_REFRESH_SUPPORT) != 0)
                     {
                        nUpdateStrategy = P_SECSTACK_UPDATE_SLAVE_WITH_NOTIFICATION;
                     }
                     else
                     {
                        nUpdateStrategy = P_SECSTACK_UPDATE_SLAVE_NO_NOTIFICATION;
                     }
                  }
               }

               pSlot->pSecurityStackInstance = PSecurityStackCreateInstance(
                  pContext,
                  pSlot->nHalSlotIdentifier,
                  pSlot->s7816Sm.pLowLevelSmInterface,
                  pSlot->s7816Sm.pLowLevelSmInstance,
                  pDefaultPrincipalList,
                  nDefaultPrincipalNumber,
                  nUpdateStrategy,
                  ((pConstantInfo->nCapabilities & W_SE_FLAG_UICC) != 0)?W_TRUE:W_FALSE);

               if(pSlot->pSecurityStackInstance == null)
               {
                  PDebugError("static_PSEBuildInfo: Error returned by PSecurityStackCreateInstance()");

                  if (pSlot->s7816Sm.pLowLevelSmInterface == &g_sPSeHalSmInterface)
                  {
                     PSeHalSmDestroyInstance(pContext, (tPSeHalSmInstance*)pSlot->s7816Sm.pSmInstance);
                     pSlot->s7816Sm.pSmInstance = (tP7816SmInstance*)null;
                  }
                  else
                  {
                     P7816SmDestroyInstance(pContext, pSlot->s7816Sm.pSmInstance);
                     pSlot->s7816Sm.pSmInstance = (tP7816SmInstance*)null;
                  }

                  nError = W_ERROR_OUT_OF_RESOURCE;
                  goto return_function;
               }

               /* Set the high-level interface/instance to point to the Security Stack */
               pSlot->s7816Sm.pSmInterface = &g_PSecurityStackSmInterface;
               pSlot->s7816Sm.pSmInstance = pSlot->pSecurityStackInstance;
            }
         }

         if (pSlot->pSecurityStackInstance == null)
         {
            /* There is no security stack */
            pSlot->s7816Sm.pSmInterface = pSlot->s7816Sm.pLowLevelSmInterface;
            pSlot->s7816Sm.pSmInstance = pSlot->s7816Sm.pLowLevelSmInstance;
         }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
      }

      PNALServiceRegisterForEvent(
         pContext,
         NAL_SERVICE_SECURE_ELEMENT,
         NAL_EVT_SE_CARD_EOT,
         &pSEInstance->sEndOfTransactionOperation,
         static_PSEMonitorEndOfTransactionEventReceived,
         pSEInstance );
   }

return_function:

   pSEInstance->bSEInfoIsBuilt = W_TRUE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEBuildInfo: returning error %s", PUtilTraceError(nError));

      PDebugWarning("static_PSEBuildInfo: Deactivating the Secure Element(s)");
      pSEInstance->nSeNumber = 0;
   }

   return nError;
}

/* See header file */
W_ERROR PSEDriverGetInfo(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            tWSEInfoEx* pSEInfo,
            uint32_t nSize )
{
   W_ERROR nError = W_SUCCESS;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tNFCControllerSEPolicy sPersistentPolicy;
   tNFCControllerSEPolicy sVolatilePolicy;
   tWSEInfoEx* pConstantInfo;

   PDebugTrace("PSEDriverGetInfo()");

   if((pSEInfo == null) || (nSize != sizeof(tWSEInfoEx)))
   {
      PDebugError("PSEDriverGetInfo: wrong buffer parameters");
      nError = W_ERROR_BAD_PARAMETER;
      pSEInfo = null;
      goto return_function;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEDriverGetInfo: wrong slot identifier");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_function;
   }

   pConstantInfo = &pSEInstance->pSlotArray[nSlotIdentifier].sConstantInfo;

   /* Update the information for the proprietary slots */
   if(PSeHalIsProprietarySlot(pSEInstance->pSlotArray[nSlotIdentifier].nHalSlotIdentifier) != W_FALSE)
   {
	  nError = PNFCControllerGetSESwitchPosition(pContext,
         &sPersistentPolicy, &sVolatilePolicy, null, null);
      if(nError != W_SUCCESS)
      {
         PDebugError("PSEDriverGetInfo: error returned by PNFCControllerGetSESwitchPosition");
         goto return_function;
      }

      pConstantInfo->nPersistentPolicy = static_PSESwitchGetPolicy(&sPersistentPolicy, nSlotIdentifier);
      pConstantInfo->nVolatilePolicy = static_PSESwitchGetPolicy(&sVolatilePolicy, nSlotIdentifier);
   }

   CMemoryCopy(pSEInfo, pConstantInfo, sizeof(tWSEInfoEx));

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverGetInfo: error %s returned", PUtilTraceError(nError));
      if(pSEInfo != null)
      {
         CMemoryFill(pSEInfo, 0, sizeof(tWSEInfoEx));
      }
   }

   return nError;
}

/* See header file */
void PSEDriverCreate(
            tSEInstance* pSEInstance )
{
   CMemoryFill( pSEInstance, 0, sizeof(tSEInstance) );

   pSEInstance->bSEInfoIsBuilt = W_FALSE;
}

/* See header file */
void PSEDriverResetData(
            tContext* pContext )
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );

   if(pSEInstance->bSEInfoIsBuilt != W_FALSE)
   {
      uint32_t nSlotIdentifier;

      for(nSlotIdentifier = 0; nSlotIdentifier < pSEInstance->nSeNumber; nSlotIdentifier++)
      {
         tSESlot* pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

#ifdef P_INCLUDE_SE_SECURITY
         /* NOTE: The two pointers are the same in case a Security Stack instance has been created */
         if((pSlot->pSecurityStackInstance != null) &&
            (pSlot->pSecurityStackInstance == pSlot->s7816Sm.pSmInstance))
         {
            PSecurityStackDestroyInstance(
                  pContext,
                  pSlot->pSecurityStackInstance );

            pSlot->pSecurityStackInstance = null;
         }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */

         if (pSlot->s7816Sm.pLowLevelSmInstance != null)
         {
#ifdef P_INCLUDE_SE_SECURITY
            if (pSlot->s7816Sm.pLowLevelSmInterface == &g_sPSeHalSmInterface)
            {
               PSeHalSmDestroyInstance(pContext, (tPSeHalSmInstance*)pSlot->s7816Sm.pLowLevelSmInstance);
               pSlot->s7816Sm.pSmInstance = (tP7816SmInstance*)null;
            }
            else
#endif /* P_INCLUDE_SE_SECURITY */
            {
               P7816SmDestroyInstance(pContext, pSlot->s7816Sm.pLowLevelSmInstance);
               pSlot->s7816Sm.pLowLevelSmInstance = (tP7816SmInstance*)null;
            }
         }
      }

      CMemoryFree(pSEInstance->pSlotArray);
   }

   CMemoryFill( pSEInstance, 0, sizeof(tSEInstance) );

   pSEInstance->bSEInfoIsBuilt = W_FALSE;
}

/* See header file */
void PSEDriverDestroy(
            tSEInstance* pSEInstance )
{
   if ( pSEInstance != null )
   {
      CMemoryFree(pSEInstance->pSlotArray);

      CMemoryFill( pSEInstance, 0, sizeof(tSEInstance) );

      pSEInstance->bSEInfoIsBuilt = W_FALSE;
   }
}

/* Destroy the monitor end-of-transaction event registry */
static uint32_t static_PSEDestroyMonitorEndOfTransaction(
            tContext* pContext,
            void* pObject )
{
   tSESlot* pSlot = P_HANDLE_GET_STRUCTURE(tSESlot, pObject, sMonitorEndOfTransaction.sObjectHeaderRegistry);

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_EOT_REGITRATION) != 0);

   PDFCDriverFlushCall(pSlot->sMonitorEndOfTransaction.pHandlerDriverCC);

   pSlot->sMonitorEndOfTransaction.pUserInstance = null;

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_EOT_REGITRATION;

   return P_HANDLE_DESTROY_DONE;
}

/* monitor end-of-transaction registry type */
tHandleType g_sSEMonitorEndOfTransaction = { static_PSEDestroyMonitorEndOfTransaction, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_SE_MONITOR_EOT (&g_sSEMonitorEndOfTransaction)

/* Receive the result of the AID list processing */
static void static_PSEMonitorEndOfTransactionProcessAIDListCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEMonitorEndOfTransactionProcessAIDListCompleted: receiving error %s", PUtilTraceError(nError));
      goto return_result;
   }

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_EOT_REGITRATION) == 0)
   {
      goto return_result;
   }

/* The Security stack should not filter the AID at this level */
/*  ---> Removing the code */
#if 0
/* #ifdef P_INCLUDE_SE_SECURITY */
   /* Filter the AID list according to the access rights */
   if(pSlot->pSecurityStackInstance != null)
   {
      uint32_t nNewLength = pSlot->sMonitorEndOfTransaction.nLastAidListLength;
      uint32_t nPosition = 0;
      while(nPosition < nNewLength)
      {
         uint32_t nAidLength = pSlot->sMonitorEndOfTransaction.aLastAidList[nPosition];
         CDebugAssert(nAidLength != 0);
         CDebugAssert(nPosition + 1 + nAidLength <= pSlot->sMonitorEndOfTransaction.nLastAidListLength);
         CDebugAssert(pSlot->sMonitorEndOfTransaction.pUserInstance != null);

         if(PSecurityStackCheckAidAccess(
               pContext,
               pSlot->pSecurityStackInstance,
               pSlot->sMonitorEndOfTransaction.pUserInstance,
               &pSlot->sMonitorEndOfTransaction.aLastAidList[nPosition + 1], nAidLength,
               null, 0 ) != W_SUCCESS)
         {
            if(nPosition + 1 + nAidLength < nNewLength)
            {
               CMemoryMove(
                  &pSlot->sMonitorEndOfTransaction.aLastAidList[nPosition],
                  &pSlot->sMonitorEndOfTransaction.aLastAidList[nPosition + 1 + nAidLength],
                  nNewLength - (nPosition + 1 + nAidLength));
            }

            nNewLength -= 1 + nAidLength;
         }
         else
         {
            nPosition += 1 + nAidLength;
         }
      }

      pSlot->sMonitorEndOfTransaction.nLastAidListLength = nNewLength;
   }
#endif /* #if 0 */

   if(pSlot->sMonitorEndOfTransaction.nLastAidListLength != 0)
   {
      PDFCDriverPostCC3( pSlot->sMonitorEndOfTransaction.pHandlerDriverCC,
         pSlot->sConstantInfo.nSlotIdentifier,
         pSlot->sMonitorEndOfTransaction.nCardProtocols );
   }

return_result:
#ifdef P_INCLUDE_GET_TRANSACTION_AID
   if((pSlot->nPendingFlags & (P_SE_FLAG_PENDING_SET_POLICY | P_SE_FLAG_PENDING_GET_TRANSACTION_AID)) == (P_SE_FLAG_PENDING_SET_POLICY | P_SE_FLAG_PENDING_GET_TRANSACTION_AID))
   {
      /* Post the queued SetPolicy callback */
      PDFCPostContext2( &pSlot->sOperationQueuedCallback, pSlot->nOperationQueuedError);
   }

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_GET_TRANSACTION_AID;
#else
   return;
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
}

#ifdef P_INCLUDE_GET_TRANSACTION_AID
#ifdef P_INCLUDE_SE_SECURITY
static void static_PSEMonitorEndOfTransactionSecurityStackProcessAIDListCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   if(nError != W_SUCCESS)
   {
      static_PSEMonitorEndOfTransactionProcessAIDListCompleted(pContext, pCallbackParameter, nError);
      return;
   }

   CDebugAssert(pSlot->pSecurityStackInstance != null);

   static_PSEMonitorUpdateAclAndCallProcessAIDListCompleted(pContext, pSlot);
}
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
#endif /* P_INCLUDE_GET_TRANSACTION_AID */

static void static_PSEMonitorEndOfTransactionEventReceived(
         tContext* pContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         const uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nNALMessageReceptionCounter)
{
   tSEInstance* pSEInstance = (tSEInstance*)pCallbackParameter;
   tSESlot* pSlot = null;
   uint32_t nCardProtocols = 0, nSlotIdentifier = 0;
   W_ERROR nError = W_SUCCESS;

   if((nEventIdentifier != NAL_EVT_SE_CARD_EOT)||(pBuffer == null)||(nLength < 3))
   {
      PDebugError("static_PSEMonitorEndOfTransactionEventReceived: Protocol error");
      /* Protocol error: can't do anything about it */
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_function;
   }

   nSlotIdentifier = pBuffer[0];
   nCardProtocols = PNALReadCardProtocols(&pBuffer[1]);

   PDebugTrace("static_PSEMonitorEndOfTransactionEventReceived(slot #%d, %s)",
      nSlotIdentifier, PUtilTraceCardProtocol(pContext, nCardProtocols));

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("static_PSEMonitorEndOfTransactionEventReceived: Protocol error in the slot value");
      /* Protocol error: can't do anything about it */
      nError = W_ERROR_NFC_HAL_COMMUNICATION;
      goto return_function;
   }

   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   /* Copy the AID value if any */
   if(nLength > 3)
   {
      nLength -= 3;
      if(nLength > P_SE_MAX_AID_LIST_LENGTH)
      {
         PDebugError("static_PSEMonitorEndOfTransactionEventReceived: list of AID too large, erased");
         nLength = 0;
      }
      pSlot->sMonitorEndOfTransaction.nLastAidListLength = nLength;
      CMemoryCopy(pSlot->sMonitorEndOfTransaction.aLastAidList, &pBuffer[3], nLength);
   }
   else
   {
      pSlot->sMonitorEndOfTransaction.nLastAidListLength = 0;
   }

   pSlot->sMonitorEndOfTransaction.nCardProtocols = nCardProtocols;

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_JUPITER) != 0)
   {
#ifndef P_INCLUDE_GET_TRANSACTION_AID
      /* Check if Mifare is possible */
      if ((nCardProtocols == W_NFCC_PROTOCOL_CARD_ISO_14443_4_A) && ((pSlot->sConstantInfo.nVolatilePolicy & W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC) != 0))
      {
         /* Add Mifare only for PJupiterProcessAIDList */
         nCardProtocols |= W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC;
      }
#endif /* not P_INCLUDE_GET_TRANSACTION_AID */

      /* Specific operation for the jupiter SE */
      nError = PJupiterProcessAIDList(
            pContext, nSlotIdentifier, nCardProtocols,
            pSlot->sMonitorEndOfTransaction.aLastAidList,
            &pSlot->sMonitorEndOfTransaction.nLastAidListLength);

#ifdef P_INCLUDE_GET_TRANSACTION_AID
      if(nError == W_ERROR_OPERATION_PENDING)
      {
         /* Asynchronous GetTransactionAID operation pending */

         if ((pSlot->nPendingFlags & (P_SE_FLAG_PENDING_OPERATION | P_SE_FLAG_PENDING_SET_POLICY)) == (P_SE_FLAG_PENDING_OPERATION | P_SE_FLAG_PENDING_SET_POLICY))
         {
            /* EndOfTransaction operation is queued after PJupiterSetPolicy */
            pSlot->nPendingFlags |= P_SE_FLAG_PENDING_END_OF_TRANSACTION_EVENT;
            return;
         }

         pSlot->nPendingFlags |= P_SE_FLAG_PENDING_GET_TRANSACTION_AID;

         /* Get SE Transaction AID */
         PJupiterGetTransactionAID(
               pContext, nSlotIdentifier,
               pSlot->sMonitorEndOfTransaction.aLastAidList, P_SE_MAX_AID_LIST_LENGTH,
               &pSlot->sMonitorEndOfTransaction.nLastAidListLength,
               static_PSEMonitorEndOfTransactionProcessAIDListCompleted,   /* NB: ACL will be reloaded when the SE connection is opened before reading the transaction hitory */
               pSlot);

         return;
      }
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
   }

return_function:

   /* Filter the AID and send the event */
#ifdef P_INCLUDE_GET_TRANSACTION_AID
#ifdef P_INCLUDE_SE_SECURITY
   if ((pSlot->sMonitorEndOfTransaction.nLastAidListLength > P_7816SM_MIN_AID_LENGTH) && (pSlot->pSecurityStackInstance != null))
   {
      static_PSEMonitorEndOfTransactionSecurityStackProcessAIDListCompleted(pContext, pSlot, nError);
   }
   else
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
   {
   static_PSEMonitorEndOfTransactionProcessAIDListCompleted(pContext, pSlot, nError);
   }
}

/* See client API specification */
W_ERROR PSEMonitorEndOfTransaction(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            tPBasicGenericEventHandler2* pHandler,
            void* pHandlerParameter,
            W_HANDLE* phEventRegistry)
{
   W_ERROR nError = W_SUCCESS;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot;

   PDebugTrace("PSEMonitorEndOfTransaction()");

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEMonitorEndOfTransaction: bad NFC Controller mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   if((phEventRegistry == null) /* || (pHandler == null) @todo: fix autogen */)
   {
      PDebugError("PSEMonitorEndOfTransaction: bad parameters");
      return W_ERROR_BAD_PARAMETER;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEMonitorEndOfTransaction: Wrong SE slot identifier");
      return W_ERROR_BAD_PARAMETER;
      goto return_error;
   }
   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   /* Check the presence of SE and the EOT detection capability */
   if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_END_OF_TRANSACTION_NOTIFICATION) == 0)
   {
      PDebugWarning("PSEMonitorEndOfTransaction: End of transaction event not supported");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   /* This flag is needed to protect the operation against re-entrantcy */
   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_EOT_REGITRATION) != 0)
   {
      PDebugError("PSEMonitorEndOfTransaction: service already used");
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   if((nError = PHandleRegister(pContext,
            &pSlot->sMonitorEndOfTransaction.sObjectHeaderRegistry,
            P_HANDLE_TYPE_SE_MONITOR_EOT, phEventRegistry)) != W_SUCCESS)
   {
      PDebugError("PSEMonitorEndOfTransaction: error registering the monitor object");
      goto return_error;
   }

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pHandler, pHandlerParameter,
      &pSlot->sMonitorEndOfTransaction.pHandlerDriverCC );

   pSlot->sMonitorEndOfTransaction.pUserInstance = PContextGetCurrentUserInstance(pContext);

   pSlot->nPendingFlags |= P_SE_FLAG_PENDING_EOT_REGITRATION;

   return W_SUCCESS;

return_error:

   PDebugError("PSEMonitorEndOfTransaction: return the error %s", PUtilTraceError(nError));

   *phEventRegistry = W_NULL_HANDLE;
   return nError;
}

/* See Client API Specification */
uint32_t PSEGetTransactionAID(
                  tContext* pContext,
                  uint32_t nSlotIdentifier,
                  uint8_t* pBuffer,
                  uint32_t nBufferLength)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot;
   uint32_t nLength;

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEGetTransactionAID: bad NFC Controller mode");
      return 0;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEGetTransactionAID: Wrong SE slot identifier");
      return 0;
   }
   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   nLength = pSlot->sMonitorEndOfTransaction.nLastAidListLength;

   if((pBuffer != null) && (nLength <= nBufferLength))
   {
      CMemoryCopy(pBuffer, pSlot->sMonitorEndOfTransaction.aLastAidList, nLength);
   }

   return nLength;
}

/* Returns the slot with the specified HAL identifier, null if not found */
tSESlot* PSEDriverGetSlotFromHalIdentifier(
            tContext* pContext,
            uint32_t nHalSlotIdentifier)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   uint32_t nSlotIdentifier;

   for(nSlotIdentifier = 0; nSlotIdentifier < pSEInstance->nSeNumber; nSlotIdentifier++)
   {
      tSESlot* pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];
      if(pSlot->nHalSlotIdentifier == nHalSlotIdentifier)
      {
         return pSlot;
      }
   }

   return null;
}

/* Destroy the monitor hot-plug event registry */
static uint32_t static_PSEDestroyMonitorHotPlug(
            tContext* pContext,
            void* pObject )
{
   tSESlot* pSlot = P_HANDLE_GET_STRUCTURE(tSESlot, pObject, sMonitorHotPlugEvents.sObjectHeaderRegistry);

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_HOTPLUG_REGISTRATION) != 0);

   PDFCDriverFlushCall(pSlot->sMonitorHotPlugEvents.pHandlerDriverCC);

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_HOTPLUG_REGISTRATION;

   return P_HANDLE_DESTROY_DONE;
}

/* monitor hot-plug registry type */
tHandleType g_sSEMonitorHotPlug = { static_PSEDestroyMonitorHotPlug, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_SE_MONITOR_HOTPLUG (&g_sSEMonitorHotPlug)


/* See header file */
void PSEDriverNotifyHotPlug(
            tContext* pContext,
            uint32_t nHalSlotIdentifier,
            bool_t bIsPresent)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot;

   PDebugTrace("PSEDriverNotifyHotPlug(slot #%d, flag=%d)",
      nHalSlotIdentifier, bIsPresent);

   if(pSEInstance->bSEInfoIsBuilt == W_FALSE)
   {
      PDebugWarning("PSEDriverNotifyHotPlug: SE info not built yet, ignoring the message");
      return;
   }

   pSlot = PSEDriverGetSlotFromHalIdentifier(pContext, nHalSlotIdentifier);

   if(pSlot == null)
   {
      PDebugError("PSEDriverNotifyHotPlug: Protocol error in the slot value");
      /* Protocol error: can't do anything about it */
      return;
   }

   pSlot->sStatus.bIsPresent = bIsPresent;

   /* @todo - Should we reset the ACL of the Security Stack here? */
#ifdef P_INCLUDE_SE_SECURITY
   if ((bIsPresent == W_FALSE) && (pSlot->pSecurityStackInstance != null))
   {
      PSecurityStackNotifySecureElementReset(pContext, pSlot->pSecurityStackInstance);
   }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_HOTPLUG_REGISTRATION) != 0)
   {
      PDFCDriverPostCC3( pSlot->sMonitorHotPlugEvents.pHandlerDriverCC,
         pSlot->sConstantInfo.nSlotIdentifier,
         (bIsPresent == W_FALSE)?W_SE_EVENT_REMOVED:W_SE_EVENT_INSERTED );
   }
}

/* See client API specification */
W_ERROR PSEMonitorHotPlugEvents(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            tPBasicGenericEventHandler2* pHandler,
            void* pHandlerParameter,
            W_HANDLE* phEventRegistry)
{
   W_ERROR nError = W_SUCCESS;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot;

   PDebugTrace("PSEMonitorHotPlugEvents()");

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEMonitorHotPlugEvents: bad NFC Controller mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   if((phEventRegistry == null) /* || (pHandler == null) @todo: fix autogen */)
   {
      PDebugError("PSEMonitorHotPlugEvents: bad parameters");
      return W_ERROR_BAD_PARAMETER;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEMonitorHotPlugEvents: Wrong SE slot identifier");
      return W_ERROR_BAD_PARAMETER;
      goto return_error;
   }
   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   /* Check the hot-plug detection capability */
   if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_HOT_PLUG) == 0)
   {
      PDebugWarning("PSEMonitorHotPlugEvents: Hot-plug event not supported");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   /* This flag is needed to protect the operation against re-entrantcy */
   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_HOTPLUG_REGISTRATION) != 0)
   {
      PDebugError("PSEMonitorHotPlugEvents: service already used");
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   if((nError = PHandleRegister(pContext,
            &pSlot->sMonitorHotPlugEvents.sObjectHeaderRegistry,
            P_HANDLE_TYPE_SE_MONITOR_HOTPLUG, phEventRegistry)) != W_SUCCESS)
   {
      PDebugError("PSEMonitorHotPlugEvents: error registering the monitor object");
      goto return_error;
   }

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pHandler, pHandlerParameter,
      &pSlot->sMonitorHotPlugEvents.pHandlerDriverCC );

   pSlot->nPendingFlags |= P_SE_FLAG_PENDING_HOTPLUG_REGISTRATION;

   return W_SUCCESS;

return_error:

   PDebugError("PSEMonitorHotPlugEvents: return the error %s", PUtilTraceError(nError));

   *phEventRegistry = W_NULL_HANDLE;
   return nError;
}

static void static_PSESetSESwitchPositionCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSESetSESwitchPositionCompleted()");

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_SET_POLICY) != 0);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSESetSESwitchPositionCompleted: return the error %s", PUtilTraceError(nError));
   }

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_SET_POLICY;

   PDFCDriverPostCC2( pSlot->pSetPolicyDriverCC, nError );
}

static void static_PSESetPolicyCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSESetPolicyCompleted()");

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_SET_POLICY) != 0);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSESetPolicyCompleted: return the error %s", PUtilTraceError(nError));
      pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_SET_POLICY;
      PDFCDriverPostCC2( pSlot->pSetPolicyDriverCC, nError );
      return;
   }

   if ((pSlot->nNewSePolicyStorageType & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      /* Volatile policy has been set when the SE connection has been closed */
      PNFCControllerSetSESwitchPosition(
            pContext, W_NFCC_STORAGE_PERSISTENT, &pSlot->sNewSePolicy,
            static_PSESetSESwitchPositionCompleted, pSlot);
   }
   else
   {
      /* Volatile policy has been set when the SE connection has been closed */
      pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_SET_POLICY;
      PDFCDriverPostCC2( pSlot->pSetPolicyDriverCC, nError );
   }
}

/* Callback for the function PNFCControllerSetSwpAccessPolicy() */
static void static_PSESetSwpAccessPolicyCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   CDebugAssert((pSlot->nPendingFlags & (P_SE_FLAG_PENDING_SET_SWP_POLICY_VOLATILE | P_SE_FLAG_PENDING_SET_SWP_POLICY_PERSISTENT)) != 0);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSESetSwpAccessPolicyCompleted: return the error %s", PUtilTraceError(nError));
   }
   else
   {
      if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_SET_SWP_POLICY_PERSISTENT) != 0)
      {
         pSlot->sConstantInfo.nPersistentPolicy = pSlot->nNewSwpPolicy;
      }
      if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_SET_SWP_POLICY_VOLATILE) != 0)
      {
         pSlot->sConstantInfo.nVolatilePolicy = pSlot->nNewSwpPolicy;
      }
   }

   pSlot->nPendingFlags &= ~(P_SE_FLAG_PENDING_SET_SWP_POLICY_VOLATILE | P_SE_FLAG_PENDING_SET_SWP_POLICY_PERSISTENT);

   PDFCDriverPostCC2( pSlot->pSetPolicyDriverCC, nError );
}

/* See header file */
void PSEDriverSetPolicy(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            uint32_t nStorageType,
            uint32_t nProtocols,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   tDFCDriverCCReference pErrorCC;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   W_ERROR nError = W_SUCCESS;
   uint32_t nIndex;
   tSESlot* pSlot;

   PDebugTrace("PSEDriverSetPolicy( nSlotIdentifier=%d, nStorageType=%d, nProtocols=%08x)",
      nSlotIdentifier, nStorageType, nProtocols);

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEDriverSetPolicy: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto send_event;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   /* Check the arguments */
   if((nStorageType != W_NFCC_STORAGE_VOLATILE)
   && (nStorageType != W_NFCC_STORAGE_PERSISTENT)
   && (nStorageType != (W_NFCC_STORAGE_PERSISTENT | W_NFCC_STORAGE_VOLATILE)))
   {
      PDebugError("PSEDriverSetPolicy: Bad storage parameters");
      nError = W_ERROR_BAD_PARAMETER;
      goto send_event;
   }

   /* check the validity of the nProtocols parameter */
   if((nProtocols | W_NFCC_PROTOCOL_CARD_ALL | W_NFCC_PROTOCOL_READER_ALL)
      != (W_NFCC_PROTOCOL_CARD_ALL | W_NFCC_PROTOCOL_READER_ALL))
   {
      PDebugError("PSEDriverSetPolicy: Bad SE policy parameter");
      nError = W_ERROR_BAD_PARAMETER;
      goto send_event;
   }

   /* Checks the slot identifier */
   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEDriverSetPolicy: Bad slot identifier");
      nError = W_ERROR_BAD_PARAMETER;
      goto send_event;
   }

   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

/* //jim delete for reader/p2p/card mode change 
   if((pSlot->nConstantProtocols & nProtocols) != nProtocols)
   {
      PDebugError("PSEDriverSetPolicy: Bad SE policy parameter");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto send_event;
   }
*/
   if(PSeHalIsSwpSlot(pSlot->nHalSlotIdentifier) != W_FALSE)
   {
      if((pSlot->nPendingFlags & (P_SE_FLAG_PENDING_SET_SWP_POLICY_VOLATILE | P_SE_FLAG_PENDING_SET_SWP_POLICY_PERSISTENT)) != 0)
      {
         PDebugError("PSEDriverSetPolicy: operation already active");
         nError = W_ERROR_BAD_STATE;
         goto send_event;
      }

      pSlot->nNewSwpPolicy = nProtocols;

      if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
      {
         pSlot->nPendingFlags |= P_SE_FLAG_PENDING_SET_SWP_POLICY_VOLATILE;
      }
      if((nStorageType & W_NFCC_STORAGE_PERSISTENT) != 0)
      {
         pSlot->nPendingFlags |= P_SE_FLAG_PENDING_SET_SWP_POLICY_PERSISTENT;
      }

      PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pSlot->pSetPolicyDriverCC );

      PNFCControllerSetSwpAccessPolicy(
            pContext,
            nStorageType,
            nProtocols,
            static_PSESetSwpAccessPolicyCompleted,
            pSlot);

      return;
   }

   if(PSeHalIsProprietarySlot(pSlot->nHalSlotIdentifier) == W_FALSE)
   {
      PDebugError("PSEDriverSetPolicy: A standalone SE has no policy to be set");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto send_event;
   }

   /* check the validity of the nProtocols parameter */
   if((nProtocols | W_NFCC_PROTOCOL_CARD_ALL) != W_NFCC_PROTOCOL_CARD_ALL)
   {
      PDebugError("PSEDriverSetPolicy: Bad SE policy parameter");
      nError = W_ERROR_BAD_PARAMETER;
      goto send_event;
   }

   /* This flag is needed to protect the operation against re-entrantcy */
   for(nIndex = 0; nIndex < pSEInstance->nSeNumber; nIndex++)
   {
      /* Check if a communication is pending */
      if((pSEInstance->pSlotArray[nIndex].nPendingFlags & P_SE_FLAG_PENDING_SET_POLICY) != 0)
      {
         PDebugError("PSEDriverSetPolicy: operation already active");
         nError = W_ERROR_BAD_STATE;
         goto send_event;
      }

      /* Special case */
      if(((pSEInstance->pSlotArray[nIndex].nPendingFlags & P_SE_FLAG_PENDING_JUPITER) != 0)
      && ((pSEInstance->pSlotArray[nIndex].nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0))
      {
         PDebugError("PSEDriverSetPolicy: internal communication already active");
         nError = W_ERROR_BAD_STATE;
         goto send_event;
      }
   }

   if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      for(nIndex = 0; nIndex < pSEInstance->nSeNumber; nIndex++)
      {
         /* Check if a communication is pending */
         if((pSEInstance->pSlotArray[nIndex].nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0)
         {
            nStorageType &= ~W_NFCC_STORAGE_VOLATILE;

            if(nProtocols == 0)
            {
               if((pSEInstance->sAfterComPolicy.nSlotIdentifier != nSlotIdentifier)
               || (pSEInstance->sAfterComPolicy.nSESwitchPosition == P_SE_SWITCH_OFF))
               {
                  PDebugTrace("PSEDriverSetPolicy: The SE #%d is already OFF", nSlotIdentifier);
                  goto check_next2;
               }

               pSEInstance->sAfterComPolicy.nSlotIdentifier = 0;
               pSEInstance->sAfterComPolicy.nSESwitchPosition = P_SE_SWITCH_OFF;
               pSEInstance->sAfterComPolicy.nSEProtocolPolicy = 0;
            }
            else
            {
               pSEInstance->sAfterComPolicy.nSlotIdentifier = nSlotIdentifier;
               pSEInstance->sAfterComPolicy.nSESwitchPosition = P_SE_SWITCH_RF_INTERFACE;
               pSEInstance->sAfterComPolicy.nSEProtocolPolicy = nProtocols;
            }

            /* Send a success event */
            goto check_next2;
         }
      }
   }

check_next2:

   if(nStorageType != 0)
   {
      pSlot->nNewSePolicyStorageType = nStorageType;

      if(nProtocols == 0)
      {
         pSlot->sNewSePolicy.nSlotIdentifier = 0;
         pSlot->sNewSePolicy.nSESwitchPosition = P_SE_SWITCH_OFF;
         pSlot->sNewSePolicy.nSEProtocolPolicy = 0;
      }
      else
      {
         pSlot->sNewSePolicy.nSlotIdentifier = nSlotIdentifier;
         pSlot->sNewSePolicy.nSESwitchPosition = P_SE_SWITCH_RF_INTERFACE;
         pSlot->sNewSePolicy.nSEProtocolPolicy = nProtocols;
      }

      nError = PNFCControllerCheckSESwitchPosition(pContext, nStorageType, &pSlot->sNewSePolicy);
      if(nError != W_SUCCESS)
      {
         PDebugError("PSEDriverSetPolicy: PNFCControllerCheckCardPolicies has returned the error %s", PUtilTraceError(nError));
         goto send_event;
      }

      pSlot->nPendingFlags |= P_SE_FLAG_PENDING_SET_POLICY;

      PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pSlot->pSetPolicyDriverCC );

            PJupiterSetPolicy(
                        pContext,
                        pSlot->sConstantInfo.nSlotIdentifier,
                        nStorageType, nProtocols,
                        static_PSESetPolicyCompleted, pSlot);

      if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
      {
         /* Set Volatile Policy after Jupiter SE communication */
         if(nProtocols == 0)
         {
            pSEInstance->sAfterComPolicy.nSlotIdentifier = 0;
            pSEInstance->sAfterComPolicy.nSESwitchPosition = P_SE_SWITCH_OFF;
            pSEInstance->sAfterComPolicy.nSEProtocolPolicy = 0;
         }
         else
         {
            pSEInstance->sAfterComPolicy.nSlotIdentifier = nSlotIdentifier;
            pSEInstance->sAfterComPolicy.nSESwitchPosition = P_SE_SWITCH_RF_INTERFACE;
            pSEInstance->sAfterComPolicy.nSEProtocolPolicy = nProtocols;
         }
      }

      return;
   }

send_event:

   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverSetPolicy: Error %s returned", PUtilTraceError(nError));
   }

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pErrorCC );

   PDFCDriverPostCC2( pErrorCC, nError );
}


/* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

         OPEN HOST CONNECTION

   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */


/* Callback invoked when the SE switch is set back to the original position */
static void static_PSEOpenConnectionSendError(
         tContext* pContext,
         tSESlot* pSlot)
{
   PDebugTrace("static_PSEOpenConnectionSendError()");

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0);

   PReaderDriverEnableAllNonSEListeners(pContext);

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_COMMUNICATION;

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER) != 0)
   {
      PDFCDriverPostCC3( pSlot->pOpenConnectionDriverCC,
         W_NULL_HANDLE, pSlot->nOpenConnectionFirstError);
   }
   else
   {
      PDFCPostContext3(&(pSlot->sOpenConnectionCC),
         W_NULL_HANDLE, pSlot->nOpenConnectionFirstError);
   }
}


/* Callback invoked when the SE switch is set back to the original position */
static void static_PSEOpenConnectionSetBackSESwitchPositionCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEOpenConnectionSetBackSESwitchPositionCompleted()");

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEOpenConnectionSetBackSESwitchPositionCompleted: Error %s returned",
         PUtilTraceError(nError));

      if(pSlot->nOpenConnectionFirstError == W_SUCCESS)
      {
         pSlot->nOpenConnectionFirstError = nError;
      }
   }

   static_PSEOpenConnectionSendError(pContext, pSlot);
}

/* Set the switch back */
static void static_PSEOpenConnectionSetBackSESwitchPosition(
            tContext* pContext,
            tSEInstance* pSEInstance,
            tSESlot* pSlot,
            W_ERROR nError )
{
   PDebugTrace("static_PSEOpenConnectionSetBackSESwitchPosition()");

   if(pSlot->nOpenConnectionFirstError == W_SUCCESS)
   {
      pSlot->nOpenConnectionFirstError = nError;
   }

   PNFCControllerSetSESwitchPosition(pContext,
      W_NFCC_STORAGE_VOLATILE, &pSEInstance->sAfterComPolicy,
      static_PSEOpenConnectionSetBackSESwitchPositionCompleted, pSlot);
}

/**
 * @brief   Destroy callback function.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 **/
static void static_PSEDestroyConnectionCallback(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEDestroyConnectionCallback()");

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0);

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_COMMUNICATION;

   PReaderDriverEnableAllNonSEListeners(pContext);

   PDFCPostContext2(&pSlot->sDestroyCallback, nError);
}

/**
 * @brief   Destroys a SE connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PSEDestroyConnectionAsync(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            void* pObject )
{
   tSESlot* pSlot = (tSESlot*)pObject;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );

   PDebugTrace("static_PSEDestroyConnectionAsync");

   PDebugTrace("static_PSEDestroyConnectionAsync: pSlot->hDriverConnection is now W_NULL_HANDLE");
   pSlot->hDriverConnection = W_NULL_HANDLE;

   if(pSlot->bFirstDestroyCalled == W_FALSE)
   {
      /* Postpone the execution of the destroy sequence after the closing of the reader mode */
      pSlot->bFirstDestroyCalled = W_TRUE;

      return P_HANDLE_DESTROY_LATER;
   }

   if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_RF) != 0)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pSlot->sDestroyCallback);

      PNFCControllerSetSESwitchPosition(pContext,
         W_NFCC_STORAGE_VOLATILE, &pSEInstance->sAfterComPolicy,
         static_PSEDestroyConnectionCallback, pSlot);

      return P_HANDLE_DESTROY_PENDING;
   }
   else
   {
      /* If the communication is pending, reset the flag */
      pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_COMMUNICATION;

      return P_HANDLE_DESTROY_DONE;
   }
}

/**
 * @brief   Gets the SE connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 **/
static uint32_t static_PSEGetPropertyNumber(
            tContext* pContext,
            void* pObject
             )
{
   return 1;
}

/**
 * @brief   Gets the SE connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PSEGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   pPropertyArray[0] = W_PROP_SECURE_ELEMENT;
   return W_TRUE;
}

/**
 * @brief   Checkes the SE connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
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

/* Handle registry SE type */
tHandleType g_sSEConnection = {  null,
                                 static_PSEDestroyConnectionAsync,
                                 static_PSEGetPropertyNumber,
                                 static_PSEGetProperties,
                                 static_PSECheckProperties,
                                 null, null, null, null };

#define P_HANDLE_TYPE_SE_CONNECTION (&g_sSEConnection)

static void static_PSeDriver7816SmDataCompletion(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nData,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDFCDriverPostCC3( pSlot->s7816Sm.pCallbackDriverCC, nData, nError );
}

static void static_PSeDriver7816SmCompletion(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDFCDriverPostCC2( pSlot->s7816Sm.pCallbackDriverCC, nError );
}

/* 7816 SM Interface */
W_ERROR PSeDriver7816SmOpenChannel(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint32_t nType,
            const uint8_t* pAID,
            uint32_t nAIDLength)
{
   tSESlot* pSlot;

   if((PHandleGetObject(pContext, hDriverConnection, P_HANDLE_TYPE_SE_CONNECTION, (void**)&pSlot) != W_SUCCESS)
   || (pSlot == null))
   {
      PDebugError("PSeDriver7816SmOpenLogicalChannel: Bad handle");
      return W_ERROR_BAD_HANDLE;
   }

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->s7816Sm.pCallbackDriverCC );

#ifdef P_INCLUDE_SE_SECURITY
   if (pSlot->pSecurityStackInstance != null)
   {
      CDebugAssert(pSlot->pUserInstance != null);
      (void)PSecurityStackSetCurrentUserInstance(pContext, pSlot->pSecurityStackInstance, pSlot->pUserInstance);
   }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */

   return pSlot->s7816Sm.pSmInterface->pOpenChannel(
                  pContext, pSlot->s7816Sm.pSmInstance,
                  static_PSeDriver7816SmDataCompletion, pSlot,
                  nType,
                  pAID, nAIDLength);
}

/* 7816 SM Interface */
W_ERROR PSeDriver7816SmCloseChannel(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            uint32_t nChannelReference,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   tSESlot* pSlot;

   if((PHandleGetObject(pContext, hDriverConnection, P_HANDLE_TYPE_SE_CONNECTION, (void**)&pSlot) != W_SUCCESS)
   || (pSlot == null))
   {
      PDebugError("PSeDriver7816SmCloseChannel: Bad handle");
      return W_ERROR_BAD_HANDLE;
   }

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->s7816Sm.pCallbackDriverCC );

   /*
   if (pSlot->pSecurityStackInstance != null)
   {
      CDebugAssert(pSlot->pUserInstance != null);
      (void)PSecurityStackSetCurrentUserInstance(pContext, pSlot->pSecurityStackInstance, pSlot->pUserInstance);
   }
   */

   return pSlot->s7816Sm.pSmInterface->pCloseChannel(
                  pContext, pSlot->s7816Sm.pSmInstance,
                  nChannelReference,
                  static_PSeDriver7816SmCompletion, pSlot);
}

/* 7816 SM Interface */
W_ERROR PSeDriver7816SmExchangeApdu(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            uint32_t nChannelReference,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pSendApduBuffer,
            uint32_t nSendApduBufferLength,
            uint8_t* pReceivedApduBuffer,
            uint32_t nReceivedApduBufferMaxLength)
{
   tSESlot* pSlot;

   if((PHandleGetObject(pContext, hDriverConnection, P_HANDLE_TYPE_SE_CONNECTION, (void**)&pSlot) != W_SUCCESS)
   || (pSlot == null))
   {
      PDebugError("PSeDriver7816SmExchangeApdu: Bad handle");
      return W_ERROR_BAD_HANDLE;
   }

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->s7816Sm.pCallbackDriverCC );

#ifdef P_INCLUDE_SE_SECURITY
   if (pSlot->pSecurityStackInstance != null)
   {
      CDebugAssert(pSlot->pUserInstance != null);
      (void)PSecurityStackSetCurrentUserInstance(pContext, pSlot->pSecurityStackInstance, pSlot->pUserInstance);
   }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */

   return pSlot->s7816Sm.pSmInterface->pExchangeApdu(
                  pContext, pSlot->s7816Sm.pSmInstance,
                  nChannelReference,
                  static_PSeDriver7816SmDataCompletion, pSlot,
                  pSendApduBuffer, nSendApduBufferLength,
                  pReceivedApduBuffer, nReceivedApduBufferMaxLength);
}

/* 7816 SM Interface */
W_ERROR PSeDriver7816SmGetData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            uint32_t nChannelReference,
            uint32_t nType,
            uint8_t* pBuffer,
            uint32_t nBufferMaxLength,
            uint32_t* pnActualLength)
{
   tSESlot* pSlot;

   if((PHandleGetObject(pContext, hDriverConnection, P_HANDLE_TYPE_SE_CONNECTION, (void**)&pSlot) != W_SUCCESS)
   || (pSlot == null))
   {
      PDebugError("PSeDriver7816SmGetData: Bad handle");
      return W_ERROR_BAD_HANDLE;
   }

   /*
   if (pSlot->pSecurityStackInstance != null)
   {
      CDebugAssert(pSlot->pUserInstance != null);
      (void)PSecurityStackSetCurrentUserInstance(pContext, pSlot->pSecurityStackInstance, pSlot->pUserInstance);
   }
   */

   return pSlot->s7816Sm.pSmInterface->pGetData(
                  pContext, pSlot->s7816Sm.pSmInstance,
                  nChannelReference, nType,
                  pBuffer, nBufferMaxLength,
                  pnActualLength);
}

#ifdef P_INCLUDE_SE_SECURITY
/* Callback for the ACL loading function */
static void static_PSEOpenConnectionLoadAclCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   /* Change error code into success */
   nError = W_SUCCESS;

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER) != 0)
   {
      PDFCDriverPostCC3( pSlot->pOpenConnectionDriverCC,
         pSlot->hDriverConnection, nError);
   }
   else
   {
      PDFCPostContext3(&(pSlot->sOpenConnectionCC),
         pSlot->hDriverConnection, nError);
   }
}
#endif

#define P_SE_SIZE_OF_HISTORYCAL_BYTES  8

static void static_PSEStoreATR(tSESlot* pSlot,
                               uint8_t * pHistorycalBytes,
                               uint8_t  nLength)
{
   uint8_t nXor;
   uint32_t i;

   CDebugAssert(nLength != 0);
   CDebugAssert(pHistorycalBytes != null);
   CDebugAssert(pSlot != null);

   pSlot->sStatus.nAtrLength = 4 + nLength + 1;

   pSlot->sStatus.aAtrBuffer[0] = 0x3B;
   pSlot->sStatus.aAtrBuffer[1] = 0x80 | nLength /* ATQB data */;
   pSlot->sStatus.aAtrBuffer[2] = 0x80;
   pSlot->sStatus.aAtrBuffer[3] = 0x01;

   /* Copy the historical bytes */
   CMemoryCopy(&pSlot->sStatus.aAtrBuffer[4],
                pHistorycalBytes,
                nLength);

   /* Calculate the Exclusive-OR, starting at "T0" position (skip header 0x3B) */

   for ( i=1, nXor=0 ; i < (uint32_t)(pSlot->sStatus.nAtrLength -1) ; i++ )
   {
      nXor ^= pSlot->sStatus.aAtrBuffer[i];
   }

   pSlot->sStatus.aAtrBuffer[pSlot->sStatus.nAtrLength -1] = nXor;

   PDebugTrace("static_PSEStoreATR: ATR");
   PDebugTraceBuffer( pSlot->sStatus.aAtrBuffer, pSlot->sStatus.nAtrLength );
}

static void static_PSEOpenConnectionDetectionHandler(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nDriverProtocol,
            W_HANDLE hDriverConnection,
            uint32_t nLength,
            bool_t bCardApplicationMatch )
{
   W_ERROR nError;
   uint8_t aHistory[P_SE_SIZE_OF_HISTORYCAL_BYTES];

   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEOpenConnectionDetectionHandler()");

   PHandleClose(pContext, pSlot->hOpenConnectionListenner);
   pSlot->hOpenConnectionListenner = W_NULL_HANDLE;

   CDebugAssert(hDriverConnection != W_NULL_HANDLE);
   CDebugAssert(nLength <= P_SE_DETECTION_BUFFER_LENGTH);

   pSlot->nOpenConnectionDetectionBufferLength = nLength;
   pSlot->nDriverProtocol = nDriverProtocol;

   /* Deactivate timer here */
   PMultiTimerCancel(pContext, TIMER_T11_SE_WATCHDOG);

   /* Add a secure element component to the connection object */
   if ( ( nError = PHandleAddHeir(
                        pContext,
                        hDriverConnection,
                        pSlot,
                        P_HANDLE_TYPE_SE_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("static_PSEOpenConnectionDetectionHandler: cannot build the connection");
      goto send_error;
   }

   /* Make Hystorical byte for ATR */
   /* Copy ATQB[5] - ATQB[11] */
   CMemoryCopy( aHistory,
                &pSlot->aOpenConnectionDetectionBuffer[4],
                7);

   /* Copy MBLI CID */
   aHistory[7] = pSlot->aOpenConnectionDetectionBuffer[11];

   static_PSEStoreATR(pSlot,
                      aHistory,
                      P_SE_SIZE_OF_HISTORYCAL_BYTES);


   /* Continue */
   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER) != 0)
   {
#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)
      if ( ( nError = PHandleSetUserInstance(
                           pContext,
                           hDriverConnection,
                           pSlot->pUserInstance) ) != W_SUCCESS )
      {
         PDebugError("static_PSEOpenConnectionDetectionHandler: cannot force the user instance for the handle");
         goto send_error;
      }
#endif/* P_CONFIG_DRIVER */
   }

   /* ACL must be reloaded when a connection is opened from user or from an end of transaction event */
   if (((pSlot->nPendingFlags & (P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER | P_SE_FLAG_PENDING_EOT_REGITRATION)) != 0) &&
       ((pSlot->nPendingFlags & P_SE_FLAG_PENDING_SET_POLICY) == 0))
   {
      CDebugAssert(pSlot->hDriverConnection == W_NULL_HANDLE);
      pSlot->hDriverConnection = hDriverConnection;
      PDebugTrace("static_PSEOpenConnectionDetectionHandler: pSlot->hDriverConnection is now 0x%08X", pSlot->hDriverConnection);

#ifdef P_INCLUDE_SE_SECURITY
      if(pSlot->pSecurityStackInstance != null)
      {
         PSecurityStackLoadAcl(pContext,
            pSlot->pSecurityStackInstance,
            static_PSEOpenConnectionLoadAclCompleted, pSlot );
         return;

      }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
   }

   /* Send event */
   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER) != 0)
   {
      PDFCDriverPostCC3( pSlot->pOpenConnectionDriverCC,
         hDriverConnection, W_SUCCESS);
   }
   else
   {
      PDFCPostContext3(&(pSlot->sOpenConnectionCC),
         hDriverConnection, W_SUCCESS);
   }

   return;

send_error:

   (void)PReaderDriverWorkPerformed(
         pContext, hDriverConnection, W_FALSE, W_TRUE );

   /* Send the error */
   static_PSEOpenConnectionSetBackSESwitchPosition(pContext, pSEInstance, pSlot, nError);
}

/* Watchdog handler is a simple wrapper to send a timeout error */
static void static_PSEOpenConnectionWatchdogHandler(
               tContext* pContext,
               void* pCallbackParameter)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEOpenConnectionWatchdogHandler()");

   PHandleClose(pContext, pSlot->hOpenConnectionListenner);
   pSlot->hOpenConnectionListenner = W_NULL_HANDLE;

   static_PSEOpenConnectionSetBackSESwitchPosition(pContext, pSEInstance, pSlot, W_ERROR_TIMEOUT);
}

/* Callback invoked when the SE switch is set to reader position */
static void static_PSEOpenConnectionSetSESwitchPositionCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;
   uint32_t nProtocol = 0;
   tNFCControllerSEPolicy sVolatilePolicy;

   PDebugTrace("static_PSEOpenConnectionSetSESwitchPositionCompleted()");

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEOpenConnectionSetSESwitchPositionCompleted: Error %s returned by PNFCControllerSetSESwitchPosition()",
         PUtilTraceError(nError));

      if(pSlot->nOpenConnectionFirstError == W_SUCCESS)
      {
         pSlot->nOpenConnectionFirstError = nError;
      }

      static_PSEOpenConnectionSendError(pContext, pSlot);
      return;
   }

   nError = PNFCControllerGetSESwitchPosition(pContext, null, &sVolatilePolicy, null, null);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEOpenConnectionSetSESwitchPositionCompleted: Error %s returned by PNFCControllerGetSESwitchPosition()",
         PUtilTraceError(nError));

      static_PSEOpenConnectionSetBackSESwitchPosition(pContext, pSEInstance, pSlot, nError);
      return;
   }

   CDebugAssert( (sVolatilePolicy.nSESwitchPosition == P_SE_SWITCH_FORCED_HOST_INTERFACE)
                  || (sVolatilePolicy.nSESwitchPosition == P_SE_SWITCH_HOST_INTERFACE));

   /* Get the SE reader protocol */
   nProtocol = pSEInstance->pSlotArray[sVolatilePolicy.nSlotIdentifier].nConstantProtocols & W_NFCC_PROTOCOL_READER_ALL;

   pSlot->hOpenConnectionListenner = W_NULL_HANDLE;

   nError = PReaderDriverRegisterInternal(
            pContext,
            static_PSEOpenConnectionDetectionHandler, pSlot,
            pSlot->bOpenConnectionForce ? W_PRIORITY_SE_FORCED : W_PRIORITY_SE,
            nProtocol, 0,
            pSlot->aOpenConnectionDetectionBuffer, P_SE_DETECTION_BUFFER_LENGTH,
            &pSlot->hOpenConnectionListenner,
            W_FALSE);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEOpenConnectionSetSESwitchPositionCompleted: Error %s returned by PReaderDriverRegisterInternal()",
         PUtilTraceError(nError));

      static_PSEOpenConnectionSetBackSESwitchPosition(pContext, pSEInstance, pSlot, nError);
      return;
   }

   PMultiTimerSet(pContext, TIMER_T11_SE_WATCHDOG, P_SE_WATCHDOG_TIMEOUT,
         static_PSEOpenConnectionWatchdogHandler, pSlot );
}

void static_PSEOpenConnectionStopAllActiveDetectionCompleted(
            tContext* pContext,
            void * pCallbackParameter)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEOpenConnectionStopAllActiveDetectionCompleted()");

   PNFCControllerSetSESwitchPosition(pContext,
      W_NFCC_STORAGE_VOLATILE, &pSEInstance->sNewPolicy,
      static_PSEOpenConnectionSetSESwitchPositionCompleted, pSlot);
}

#ifdef P_INCLUDE_SE_SECURITY
static void static_PSEOpenConnectionGetInfoCallbackFunction(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nHalSlotIdentifier,
            uint32_t nHalSessionIdentifier,
            uint32_t nActualAtrLength,
            W_ERROR nError )
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;
   W_HANDLE hDriverConnection = W_NULL_HANDLE;

   if(nError == W_SUCCESS)
   {
      if(nHalSessionIdentifier != 0)
      {
         PDebugTrace("static_PSEOpenConnectionGetInfoCallbackFunction: Card detected on slot %d", nHalSlotIdentifier);
         pSlot->sStatus.bIsPresent = W_TRUE;

         pSlot->sStatus.nAtrLength = nActualAtrLength;

         if(pSlot->nHalSessionIdentifier != nHalSessionIdentifier)
         {
            PDebugTrace("static_PSEOpenConnectionGetInfoCallbackFunction: New card detected on slot %d", nHalSlotIdentifier);

            pSlot->nHalSessionIdentifier = nHalSessionIdentifier;
         }
      }
      else
      {
         PDebugTrace("static_PSEOpenConnectionGetInfoCallbackFunction: No card detected on slot %d", nHalSlotIdentifier);
         pSlot->sStatus.bIsPresent = W_FALSE;
      }
   }
   else
   {
      PDebugError("static_PSEOpenConnectionGetInfoCallbackFunction: Error during detection on slot %d", nHalSlotIdentifier);
      pSlot->sStatus.bIsPresent = W_FALSE;
   }

   if(pSlot->sStatus.bIsPresent == W_FALSE)
   {
      pSlot->sStatus.nAtrLength = 0;
      pSlot->nHalSessionIdentifier = 0;
      nError = W_ERROR_TIMEOUT;
      goto return_result;
   }

   if ( ( nError = PHandleRegister(
                        pContext,
                        pSlot,
                        P_HANDLE_TYPE_SE_CONNECTION,
                        &hDriverConnection ) ) != W_SUCCESS )
   {
      PDebugError("static_PSEOpenConnectionGetInfoCallbackFunction: cannot build the connection");
      hDriverConnection = W_NULL_HANDLE;
      goto return_result;
   }

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER) != 0)
   {
#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)
      if ( ( nError = PHandleSetUserInstance(
                           pContext,
                           hDriverConnection,
                           pSlot->pUserInstance) ) != W_SUCCESS )
      {
         PDebugError("static_PSEOpenConnectionGetInfoCallbackFunction: cannot force the user instance for the handle");
         goto return_result;
      }
#endif/* P_CONFIG_DRIVER */
   }

   pSlot->hDriverConnection = hDriverConnection;
   PDebugTrace("static_PSEOpenConnectionGetInfoCallbackFunction: pSlot->hDriverConnection is now 0x%08X", pSlot->hDriverConnection);

#ifdef P_INCLUDE_SE_SECURITY
   if(pSlot->pSecurityStackInstance != null)
   {
      PSecurityStackLoadAcl(pContext,
         pSlot->pSecurityStackInstance,
         static_PSEOpenConnectionLoadAclCompleted, pSlot );
      return;
   }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */

return_result:

   if(nError != W_SUCCESS)
   {
      pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_COMMUNICATION;
   }

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER) != 0)
   {
      PDFCDriverPostCC3( pSlot->pOpenConnectionDriverCC,
         hDriverConnection, nError);
   }
   else
   {
      PDFCPostContext3(&(pSlot->sOpenConnectionCC),
         hDriverConnection, nError);
   }
}
#endif

/** Initialize a Secure Element */
static void static_PSEOpenConnection(
                  tContext* pContext,
                  uint32_t nSlotIdentifier,
                  bool_t bForce,
                  bool_t bFromUser,
                  tPBasicGenericHandleCallbackFunction* pCallback,
                  void* pCallbackParameter )
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   W_ERROR nError = W_SUCCESS;
   tSESlot* pSlot;
   tDFCCallbackContext sErrorCC;
   tDFCDriverCCReference pErrorDriverCC;
   uint32_t nIndex;

   PDebugTrace("static_PSEOpenConnection()");

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("static_PSEOpenConnection: Wrong slot identifier");
      nError = W_ERROR_BAD_PARAMETER;
      goto send_event;
   }

   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   /* Check the presence of SE and the communication capability */
   if((pSlot->sStatus.bIsPresent == W_FALSE) ||
      ((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION) == 0))
   {
      PDebugError("static_PSEOpenConnection: No SE or communication not supported");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto send_event;
   }

#ifdef P_INCLUDE_SE_SECURITY
   if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_RF) != 0)
   {
#else
      CDebugAssert((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_RF) != 0);
#endif /* P_INCLUDE_SE_SECURITY */

      /* This flag is needed to protect the operation against re-entrantcy */
      for(nIndex = 0; nIndex < pSEInstance->nSeNumber; nIndex++)
      {
         if(((pSEInstance->pSlotArray[nIndex].sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_RF) != 0)
         && ((pSEInstance->pSlotArray[nIndex].nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0))
         {
            PDebugError("static_PSEOpenConnection: communication already active");
            nError = W_ERROR_EXCLUSIVE_REJECTED;
            goto send_event;
         }
      }

      if(bFromUser != W_FALSE)
      {
         /* This flag is needed to protect the operation against re-entrantcy */
         for(nIndex = 0; nIndex < pSEInstance->nSeNumber; nIndex++)
         {
            if(((pSEInstance->pSlotArray[nIndex].sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_RF) != 0)
            && ((pSEInstance->pSlotArray[nIndex].nPendingFlags & P_SE_FLAG_PENDING_JUPITER) != 0)
            && ((pSEInstance->pSlotArray[nIndex].nPendingFlags & P_SE_FLAG_PENDING_SET_POLICY) != 0))
            {
               PDebugError("static_PSEOpenConnection: internal communication already active");
               nError = W_ERROR_EXCLUSIVE_REJECTED;
               goto send_event;
            }
         }
      }

      nError = PNFCControllerGetSESwitchPosition(pContext,
         null, null, null, &pSEInstance->sAfterComPolicy);
      if(nError != W_SUCCESS)
      {
         PDebugError("static_PSEOpenConnection: error returned by PNFCControllerGetSESwitchPosition()");
         goto send_event;
      }

      pSEInstance->sNewPolicy.nSESwitchPosition = (bForce != W_FALSE)?P_SE_SWITCH_FORCED_HOST_INTERFACE:P_SE_SWITCH_HOST_INTERFACE;
      pSEInstance->sNewPolicy.nSlotIdentifier = nSlotIdentifier;
      pSEInstance->sNewPolicy.nSEProtocolPolicy = pSEInstance->sAfterComPolicy.nSEProtocolPolicy;

      pSlot->nOpenConnectionFirstError = W_SUCCESS;
      pSlot->bFirstDestroyCalled = W_FALSE;
      pSlot->bOpenConnectionForce = bForce;

      PReaderDriverDisableAllNonSEListeners(pContext,
            static_PSEOpenConnectionStopAllActiveDetectionCompleted,
            pSlot);

#ifdef P_INCLUDE_SE_SECURITY
   }
   else
   {
      CDebugAssert((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_SE_HAL) != 0);

      /* This flag is needed to protect the operation against re-entrantcy */
      if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0)
      {
         PDebugError("static_PSEOpenConnection: communication already active");
         nError = W_ERROR_EXCLUSIVE_REJECTED;
         goto send_event;
      }

      PSeHalGetInfo(
         pContext,
         static_PSEOpenConnectionGetInfoCallbackFunction, pSlot,
         pSlot->nHalSlotIdentifier,
         pSlot->sStatus.aAtrBuffer,
         sizeof(pSlot->sStatus.aAtrBuffer) );
   }
#endif /* P_INCLUDE_SE_SECURITY */

   pSlot->nPendingFlags |= P_SE_FLAG_PENDING_COMMUNICATION;

   if(bFromUser != W_FALSE)
   {
      PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->pOpenConnectionDriverCC );

      pSlot->pUserInstance = PContextGetCurrentUserInstance(pContext);

      pSlot->nPendingFlags |= P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER;
   }
   else
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &(pSlot->sOpenConnectionCC));

      pSlot->pUserInstance = null;

      pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_COMMUNICATION_FROM_USER;
   }

   return;

send_event:

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEOpenConnection: Sending error %s", PUtilTraceError(nError));
   }

   if(bFromUser != W_FALSE)
   {
      PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter, &pErrorDriverCC );
      PDFCDriverPostCC3( pErrorDriverCC,
         W_NULL_HANDLE, nError );
   }
   else
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sErrorCC);
      PDFCPostContext3(&sErrorCC, W_NULL_HANDLE, nError);
   }
}

/* See header file */
void PSEDriverOpenConnection(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            bool_t bForce,
            tPBasicGenericHandleCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   static_PSEOpenConnection(
                  pContext, nSlotIdentifier, bForce, W_TRUE,
                  pCallback, pCallbackParameter );
}

/* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

         INITIALIZATION

   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */


/* Forward declaration */
static void static_PSEInitializeNext(
            tContext* pContext,
            W_ERROR nError);

/* Callback for the close safe function */
static void static_PSEInitializeCloseSafeCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEInitializeCloseSafeCompleted: Error %s received", PUtilTraceError(nError));
   }

   if(pSlot->sIntialization.nLastError != W_SUCCESS)
   {
      nError = pSlot->sIntialization.nLastError;
   }

   pSlot->sIntialization.nLastError = W_SUCCESS;

   static_PSEInitializeNext(pContext, nError);
}

/* Callback for the apply policy function */
static void static_PSEInitializeResetSecurityStack(
            tContext* pContext,
            tSESlot* pSlot)
{
   CDebugAssert(pSlot->sOperation.bCloseConnection == W_FALSE);

   PHandleCloseSafe(
      pContext,
      pSlot->hDriverConnection,
      static_PSEInitializeCloseSafeCompleted, pSlot);
}

/* Callback for the apply policy function */
static void static_PSEInitializeApplyPolicyConnectionCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEInitializeApplyPolicyConnectionCompleted()");

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEInitializeApplyPolicyConnectionCompleted: Error %s received",
         PUtilTraceError(nError));

      if(pSlot->sIntialization.nLastError == W_SUCCESS)
      {
         pSlot->sIntialization.nLastError = nError;
      }
   }
   else
   {
      pSlot->nPendingFlags |= P_SE_FLAG_PENDING_JUPITER;
   }

   static_PSEInitializeResetSecurityStack(pContext, pSlot);
}

/* Callback for the PJupiterGetPolicy */
static void static_PSEInitializeGetPolicyCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   /* Change Current Policy before applying */
   if(((pSlot->sIntialization.nPersistentPolicy & W_NFCC_PROTOCOL_CARD_ISO_14443_4_A) != 0x00)
   && ((pSlot->sIntialization.nPersistentPolicy & W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC) != 0x00)
   && ((pNFCController->sPolicyMonitor.sPersistent.sSEPolicy.nSEProtocolPolicy & W_NFCC_PROTOCOL_CARD_ISO_14443_4_A) != 0x00))
   {
      /* check the equality between the nSlotIdentifier and the identifier contained in the policy monitor */
      if(pNFCController->sPolicyMonitor.sPersistent.sSEPolicy.nSlotIdentifier == pSlot->sConstantInfo.nSlotIdentifier)
      {
         /* Modify all stored policies */
         pNFCController->sPolicyMonitor.sPersistent.sSEPolicy.nSEProtocolPolicy |= W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC;
         pNFCController->sPolicyMonitor.sVolatile.sSEPolicy.nSEProtocolPolicy  = pNFCController->sPolicyMonitor.sPersistent.sSEPolicy.nSEProtocolPolicy;


         pNFCController->sPolicyMonitor.sNewPersistent = pNFCController->sPolicyMonitor.sPersistent;
         pNFCController->sPolicyMonitor.sNewVolatile = pNFCController->sPolicyMonitor.sVolatile;

         pSEInstance->sAfterComPolicy = pNFCController->sPolicyMonitor.sPersistent.sSEPolicy;

      }
   }

   /* Jupiter is only for proprietary SE */
   PJupiterSetPolicy(
            pContext,
            pSlot->sConstantInfo.nSlotIdentifier,
            W_NFCC_STORAGE_BOTH, pNFCController->sPolicyMonitor.sPersistent.sSEPolicy.nSEProtocolPolicy,
            static_PSEInitializeApplyPolicyConnectionCompleted, pSlot);
}

/* Callback for the load ACL function */
static void static_PSEInitializeLoadAclCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   pSlot->sIntialization.nLastError = nError;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEInitializeLoadAclCompleted: Error %s received", PUtilTraceError(nError));

      if(pSlot->sIntialization.nLastError == W_SUCCESS)
      {
         pSlot->sIntialization.nLastError = nError;
      }
   }

   if(PSeHalIsProprietarySlot(pSlot->nHalSlotIdentifier) != W_FALSE)
   {
      PJupiterGetPolicy(
               pContext,
               pSlot->sConstantInfo.nSlotIdentifier,
               &pSlot->sIntialization.nPersistentPolicy,
               &pSlot->sIntialization.nVolatilePolicy,
               static_PSEInitializeGetPolicyCompleted, pSlot);
   }
   else if(nError != W_SUCCESS)
   {
      CDebugAssert(pSlot->sOperation.bCloseConnection == W_FALSE);

      PHandleCloseSafe(
         pContext,
         pSlot->hDriverConnection,
         static_PSEInitializeCloseSafeCompleted, pSlot);
   }
   else
   {
      static_PSEInitializeResetSecurityStack(pContext, pSlot);
   }
}

/* Callback for the SE initalization */
static void static_PSEInitializeOpenConnectionCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_HANDLE hConnection,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEInitializeOpenConnectionCompleted: Error %s received", PUtilTraceError(nError));
      static_PSEInitializeNext(pContext, nError);
      return;
   }

   pSlot->hDriverConnection = hConnection;
   PDebugTrace("static_PSEInitializeOpenConnectionCompleted: pSlot->hDriverConnection is now 0x%08X", pSlot->hDriverConnection);

#ifdef P_INCLUDE_SE_SECURITY
   if((pSlot->pSecurityStackInstance != null) && (pSlot->sStatus.bIsPresent != W_FALSE))
   {
      PSecurityStackLoadAcl(pContext,
         pSlot->pSecurityStackInstance,
         static_PSEInitializeLoadAclCompleted, pSlot );
   }
   else
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
   {
      static_PSEInitializeLoadAclCompleted(pContext, pSlot, W_SUCCESS);
   }
}

static void static_PSEInitializeGetInfoCallbackFunction(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nHalSlotIdentifier,
            uint32_t nHalSessionIdentifier,
            uint32_t nActualAtrLength,
            W_ERROR nError )
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   pSlot->sStatus.nAtrLength = 0;
   pSlot->nHalSessionIdentifier = 0;

   if(nError == W_ERROR_FEATURE_NOT_SUPPORTED)
   {
      /* HAL not supported, nothing to do */
      nError = W_SUCCESS;
   }
   else if(nError == W_SUCCESS)
   {
      pSlot->sStatus.nAtrLength = nActualAtrLength;
      pSlot->nHalSessionIdentifier = nHalSessionIdentifier;
      if(nHalSessionIdentifier != 0)
      {
         PDebugTrace("static_PSEInitializeGetInfoCallbackFunction: Card detected on slot %d", nHalSlotIdentifier);
         pSlot->sStatus.bIsPresent = W_TRUE;
      }
      else
      {
         PDebugTrace("static_PSEInitializeGetInfoCallbackFunction: No card detected on slot %d", nHalSlotIdentifier);
         pSlot->sStatus.bIsPresent = W_FALSE;
      }
   }
   else
   {
      PDebugError("static_PSEInitializeGetInfoCallbackFunction: Error during detection on slot %d", nHalSlotIdentifier);
      pSlot->sStatus.bIsPresent = W_FALSE;
   }

   if(pSlot->sStatus.bIsPresent == W_FALSE)
   {
      static_PSEInitializeNext(pContext, nError);
      return;
   }

   if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_COMMUNICATION_VIA_RF) != 0)
   {
      static_PSEOpenConnection(
         pContext,
         pSEInstance->nSEIndentifierBeingInitialized, W_TRUE, W_FALSE,
         static_PSEInitializeOpenConnectionCompleted, pSlot);
   }
   else
   {
      W_HANDLE hConnection;

      if ( ( nError = PHandleRegister(
                           pContext,
                           pSlot,
                           P_HANDLE_TYPE_SE_CONNECTION,
                           &hConnection ) ) != W_SUCCESS )
      {
         PDebugError("static_PSEInitializeGetInfoCallbackFunction: cannot build the connection");
         hConnection = W_NULL_HANDLE;
      }
      else
      {
         pSlot->nPendingFlags |= P_SE_FLAG_PENDING_COMMUNICATION;
      }

      static_PSEInitializeOpenConnectionCompleted(
            pContext, pSlot, hConnection, nError);
   }
}

/* Callback for the next initalization */
static void static_PSEInitializeNext(
            tContext* pContext,
            W_ERROR nError)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEInitializeNext: Error %s received for SE #%d",
         PUtilTraceError(nError), pSEInstance->nSEIndentifierBeingInitialized);
      PDebugWarning("static_PSEInitializeNext: The error is ignored but the access to the SE #%d is locked.",
         pSEInstance->nSEIndentifierBeingInitialized);
   }

   pSEInstance->nSEIndentifierBeingInitialized++;

   if(pSEInstance->nSEIndentifierBeingInitialized < pSEInstance->nSeNumber)
   {
      tSESlot* pSlot = &pSEInstance->pSlotArray[pSEInstance->nSEIndentifierBeingInitialized];

#ifdef P_INCLUDE_SE_SECURITY
      if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_SE_HAL) != 0)
      {
         PSeHalGetInfo(
            pContext,
            static_PSEInitializeGetInfoCallbackFunction, pSlot,
            pSlot->nHalSlotIdentifier,
            pSlot->sStatus.aAtrBuffer,
            sizeof(pSlot->sStatus.aAtrBuffer) );
      }
      else
#endif /* P_INCLUDE_SE_SECURITY */
      {
         tDFCCallbackContext sCallbackContext;

         PDFCFillCallbackContext( pContext,
            (tDFCCallback*)static_PSEInitializeGetInfoCallbackFunction, pSlot,
            &sCallbackContext );

         PDFCPostContext5( &sCallbackContext, pSlot->nHalSlotIdentifier, 0, 0, W_ERROR_FEATURE_NOT_SUPPORTED );
      }

      return;
   }

   if (pSEInstance->nSEIndentifierBeingInitialized == pSEInstance->nSeNumber)
   {
      PDFCPostContext2( &pSEInstance->sInitializeCallback, nError );
   }
   else
   {
      PDebugError("static_PSEDriverInitializeNext : spurious call after completion callback has been posted");
   }
}

/* See header file */
void PSEDriverInitialize(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   W_ERROR nError;

   PDebugTrace("PSEDriverInitialize()");

   PSEDriverResetData(pContext);

   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &pSEInstance->sInitializeCallback);

   pSEInstance->nSEIndentifierBeingInitialized = (uint32_t)-1;

   nError = static_PSEBuildInfo(pContext, pSEInstance);
   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverInitialize: Error %s returned by static_PSEBuildInfo()", PUtilTraceError(nError));
   }

   static_PSEInitializeNext(pContext, nError);
}

/* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

         OPERATION FUNCTIONS

   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */


/** See the specification tPBasicExchangeData */
static void static_PSEOperationExchangeApdu(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation )
{
   tSESlot* pSlot;
   W_ERROR nError;
   tDFCCallbackContext sErrorCallback;

   PDebugTrace("static_PSEOperationExchangeApdu()");

   if((PHandleGetObject(pContext, hDriverConnection, P_HANDLE_TYPE_SE_CONNECTION, (void**)&pSlot) != W_SUCCESS)
   || (pSlot == null))
   {
      PDebugError("static_PSEOperationExchangeApdu: Bad handle");
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   CDebugAssert(pReaderToCardBuffer != null);
   CDebugAssert(nReaderToCardBufferLength <= sizeof(pSlot->sOperation.aReaderToCardBuffer));

   nError = pSlot->s7816Sm.pLowLevelSmInterface->pExchangeApdu(pContext,
      pSlot->s7816Sm.pLowLevelSmInstance,
      pSlot->sOperation.nChannelReference,
      pCallback, pCallbackParameter,
      /* C-APDU */
      pReaderToCardBuffer, nReaderToCardBufferLength,
      /* R-APDU */
      pCardToReaderBuffer, nCardToReaderBufferMaxLength);

   if (nError != W_ERROR_OPERATION_PENDING)
   {
      goto return_error;
   }

   return;

return_error:

   PDebugError("static_PSEOperationExchangeApdu: returning %s", PUtilTraceError(nError));

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &sErrorCallback );

   PDFCPostContext3( &sErrorCallback, 0, nError );
}

/* Destroy connection callback */
static void static_PSEOperationDestroyConnection(
            tContext* pContext,
            void* pParameter,
            W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pParameter;

   PDebugTrace("static_PSEOperationDestroyConnection()");

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_OPERATION) != 0);
   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_OPERATION;

   /* Already reset in the connection destroy function */
   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) == 0);

   if(pSlot->sOperation.nCurrentError == W_SUCCESS)
   {
      pSlot->sOperation.nCurrentError = nError;
   }

   pSlot->hDriverConnection = W_NULL_HANDLE;
   PDebugTrace("static_PSEOperationDestroyConnection: pSlot->hDriverConnection is now W_NULL_HANDLE");

   PDFCPostContext2( &pSlot->sOperation.sOperationCallback,
      pSlot->sOperation.nCurrentError );
}

#ifdef P_INCLUDE_GET_TRANSACTION_AID
static void static_PSEGetTransactionAIDAfterLoadAclCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;
   uint32_t nSlotIdentifier;

   for (nSlotIdentifier = 0; nSlotIdentifier < P_SE_HAL_MAXIMUM_SE_NUMBER; nSlotIdentifier++)
   {
      if(pSEInstance->pSlotArray[nSlotIdentifier].nHalSlotIdentifier == pSlot->nHalSlotIdentifier)
      {
         break;
      }
   }
   CDebugAssert(nSlotIdentifier < P_SE_HAL_MAXIMUM_SE_NUMBER);

   pSlot->nPendingFlags |= P_SE_FLAG_PENDING_GET_TRANSACTION_AID;

   /* Get SE Transaction AID */
   PJupiterGetTransactionAID(
         pContext, nSlotIdentifier,
         pSlot->sMonitorEndOfTransaction.aLastAidList, P_SE_MAX_AID_LIST_LENGTH,
         &pSlot->sMonitorEndOfTransaction.nLastAidListLength,
         static_PSEMonitorEndOfTransactionProcessAIDListCompleted,
         pSlot);

   /* The pending event has been processing */
   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_END_OF_TRANSACTION_EVENT;
   /* Restore the saved value */
   pSlot->sOperation.bCloseConnection = pSlot->bOperationQueuedCloseConnection;
}
#endif /* P_INCLUDE_GET_TRANSACTION_AID */

static void static_PSEOperationCloseChannelCompleted(
   tContext* pContext,
   void* pCallbackParameter,
   W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEOperationCloseChannelCompleted()");

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEOperationCloseChannelCompleted: returning error %s", PUtilTraceError(nError));
      if(pSlot->sOperation.nCurrentError == W_SUCCESS)
      {
         pSlot->sOperation.nCurrentError = nError;
      }
   }

   pSlot->sOperation.nChannelReference = P_7816SM_NULL_CHANNEL;

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_OPERATION) == 0)
   {
      PDFCPostContext2( &pSlot->sOperation.sOperationCallback, pSlot->sOperation.nCurrentError );
   }
   else
   {
#ifdef P_INCLUDE_GET_TRANSACTION_AID
      if ((pSlot->nPendingFlags & (P_SE_FLAG_PENDING_SET_POLICY | P_SE_FLAG_PENDING_END_OF_TRANSACTION_EVENT)) == (P_SE_FLAG_PENDING_SET_POLICY | P_SE_FLAG_PENDING_END_OF_TRANSACTION_EVENT))
      {
         /* EndOfTransaction operation is queued after PJupiterSetPolicy */

         /* Save SetPolicy callback and result */
         pSlot->sOperationQueuedCallback = pSlot->sOperation.sOperationCallback;
         pSlot->nOperationQueuedError = pSlot->sOperation.nCurrentError;
         pSlot->bOperationQueuedCloseConnection = pSlot->sOperation.bCloseConnection;

#ifdef P_INCLUDE_SE_SECURITY
         PSecurityStackLoadAcl(pContext, pSlot->pSecurityStackInstance, static_PSEGetTransactionAIDAfterLoadAclCompleted, pSlot);
#else
         static_PSEGetTransactionAIDAfterLoadAclCompleted(pContext, pSlot, W_SUCCESS);
#endif
      }
      else
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
      if (pSlot->sOperation.bCloseConnection != W_FALSE)
      {
         pSlot->sOperation.bCloseConnection = W_FALSE;

         PHandleCloseSafe(pContext, pSlot->hDriverConnection,
            static_PSEOperationDestroyConnection, pSlot);
      }
      else
      {
         pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_OPERATION;
         PDFCPostContext2( &pSlot->sOperation.sOperationCallback, pSlot->sOperation.nCurrentError );
      }
   }
}

static void static_PSEOperationCloseLogicalChannel(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tSESlot* pSlot)
{
   if (pSlot->sOperation.nChannelReference != P_7816SM_NULL_CHANNEL)
   {
      W_ERROR nError = pSlot->s7816Sm.pLowLevelSmInterface->pCloseChannel(pContext,
         pSlot->s7816Sm.pLowLevelSmInstance,
         pSlot->sOperation.nChannelReference,
         static_PSEOperationCloseChannelCompleted,
         pSlot);

      if (nError != W_ERROR_OPERATION_PENDING)
      {
         static_PSEOperationCloseChannelCompleted(pContext, pSlot, nError);
      }
   }
   else
   {
      static_PSEOperationCloseChannelCompleted(pContext, pSlot, W_SUCCESS);
   }
}

static void static_PSEOperationCompletion(
   tContext* pContext,
   void* pCallbackParameter,
   W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEOperationCompletion()");

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEOperationCompletion: returning error %s",
         PUtilTraceError(nError));

      if(pSlot->sOperation.nCurrentError == W_SUCCESS)
      {
         pSlot->sOperation.nCurrentError = nError;
      }
   }

   static_PSEOperationCloseLogicalChannel(
            pContext, pSlot->hDriverConnection, pSlot);
}

static void static_PSEDriverOperationOpenLogicalChannelCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nChannelReference,
                  W_ERROR nResult );

static void static_PSEOperationOpenConnectionCompleted(
   tContext* pContext,
   void* pCallbackParameter,
   W_HANDLE hDriverConnection,
   W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEOperationOpenConnectionCompleted()");

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PSEOperationOpenConnectionCompleted: error received");
      goto return_error;
   }

   pSlot->hDriverConnection = hDriverConnection;
   PDebugTrace("static_PSEOperationOpenConnectionCompleted: pSlot->hDriverConnection is now 0x%08X", pSlot->hDriverConnection);

   nError = pSlot->s7816Sm.pLowLevelSmInterface->pOpenChannel(pContext,
      pSlot->s7816Sm.pLowLevelSmInstance,
      static_PSEDriverOperationOpenLogicalChannelCompleted,
      pSlot,
      ((pSlot->nPendingFlags & P_SE_FLAG_PENDING_OPERATION_LOGICAL_CHANNEL) == 0 ? W_7816_CHANNEL_TYPE_BASIC : W_7816_CHANNEL_TYPE_LOGICAL),
      pSlot->sOperation.pAidBuffer,
      pSlot->sOperation.nAidLength);

   if (nError != W_ERROR_OPERATION_PENDING)
   {
      goto return_error;
   }

   return;

return_error:

   PDebugError("static_PSEOperationOpenConnectionCompleted: returning error %s",
      PUtilTraceError(nError));

   if (pSlot->sOperation.bCloseConnection != W_FALSE)
   {
      pSlot->sOperation.bCloseConnection = W_FALSE;

      if (pSlot->hDriverConnection != W_NULL_HANDLE)
      {
         PHandleCloseSafe(pContext, pSlot->hDriverConnection,
            static_PSEOperationDestroyConnection, pSlot);
      }
   }

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_COMMUNICATION;
   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_OPERATION;

   PDFCPostContext2( &pSlot->sOperation.sOperationCallback, nError );
}

static void static_PSEDriverOperationOpenLogicalChannelCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nChannelReference,
                  W_ERROR nResult )
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEDriverOperationOpenLogicalChannelCompleted()");

   if(nResult != W_SUCCESS)
   {
      PDebugError("static_PSEDriverOperationOpenLogicalChannelCompleted: error received");
      goto return_error;
   }

   CDebugAssert(pSlot->sOperation.nChannelReference == P_7816SM_NULL_CHANNEL);
   pSlot->sOperation.nChannelReference = nChannelReference;

   /* Simulate SELECT success */
   pSlot->sOperation.aCardToReaderBuffer[0] = 0x90;
   pSlot->sOperation.aCardToReaderBuffer[1] = 0x00;

   pSlot->sOperation.pFunction(
            pContext, pSlot->sOperation.pInstance,
            static_PSEOperationExchangeApdu,
            pSlot->hDriverConnection,
            pSlot->sOperation.aCardToReaderBuffer, 2,
            static_PSEOperationCompletion, pSlot );
   return;

return_error:

   PDebugError("static_PSEDriverOperationOpenLogicalChannelCompleted: returning error %s",
      PUtilTraceError(nResult));

   if(pSlot->sOperation.nCurrentError == W_SUCCESS)
   {
      pSlot->sOperation.nCurrentError = nResult;
   }

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_OPERATION) == 0)
   {
      pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_COMMUNICATION;

      PDFCPostContext2( &pSlot->sOperation.sOperationCallback, nResult );
   }
   else
   {
      if (pSlot->sOperation.bCloseConnection != W_FALSE)
      {
         pSlot->sOperation.bCloseConnection = W_FALSE;

         PHandleCloseSafe(pContext, pSlot->hDriverConnection,
            static_PSEOperationDestroyConnection, pSlot);
      }
   }
}

/* See header file */
void PSEDriverOperation(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            bool_t bCreateLogicalChannel,
            const uint8_t* pAidBuffer,
            uint32_t nAidLength,
            tPSEPerformAppletOperation* pFunction,
            void* pInstance,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter )
{
   W_ERROR nError = W_SUCCESS;
   tSESlot* pSlot;
   tDFCCallbackContext sErrorCallback;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );

   PDebugTrace("PSEDriverOperation()");

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEDriverOperation: wrong slot identifier");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];
   CDebugAssert(pSlot != null);

   if (pSlot->hDriverConnection != W_NULL_HANDLE)
   {
      if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) == 0)
      {
         PDebugError("PSEDriverOperation: communication is not active");
         nError = W_ERROR_BAD_STATE;
         goto return_error;
      }
   }
   else
   {
      if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0)
      {
            PDebugError("PSEDriverOperation: communication is already active");
            nError = W_ERROR_BAD_STATE;
            goto return_error;
      }

      CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_OPERATION) == 0);
      pSlot->nPendingFlags |= P_SE_FLAG_PENDING_OPERATION;
   }

   if(bCreateLogicalChannel != W_FALSE)
   {
      pSlot->nPendingFlags |= P_SE_FLAG_PENDING_OPERATION_LOGICAL_CHANNEL;
   }
   else
   {
      pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_OPERATION_LOGICAL_CHANNEL;
   }

   /* Store the operation information */
   pSlot->sOperation.pAidBuffer = pAidBuffer;
   pSlot->sOperation.nAidLength = nAidLength;
   pSlot->sOperation.pFunction = pFunction;
   pSlot->sOperation.pInstance = pInstance;
   pSlot->sOperation.nCurrentError = W_SUCCESS;

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &pSlot->sOperation.sOperationCallback );

   CDebugAssert(pSlot->sOperation.nChannelReference == P_7816SM_NULL_CHANNEL);
   pSlot->sOperation.nChannelReference = P_7816SM_NULL_CHANNEL;

   if (pSlot->hDriverConnection == W_NULL_HANDLE)
   {
      PDebugTrace("PSEDriverOperation: pSlot->hDriverConnection = W_NULL_HANDLE)");

      pSlot->sOperation.bCloseConnection = W_TRUE;

      static_PSEOpenConnection(
                  pContext,
                  nSlotIdentifier,
                  W_TRUE,
                  W_FALSE,
                  static_PSEOperationOpenConnectionCompleted, pSlot );
   }
   else
   {
      PDebugTrace("PSEDriverOperation: pSlot->hDriverConnection = 0x%08X", pSlot->hDriverConnection);

      pSlot->sOperation.bCloseConnection = W_FALSE;

      nError = pSlot->s7816Sm.pLowLevelSmInterface->pOpenChannel(pContext,
         pSlot->s7816Sm.pLowLevelSmInstance,
         static_PSEDriverOperationOpenLogicalChannelCompleted,
         pSlot,
         ((bCreateLogicalChannel == W_FALSE) ? W_7816_CHANNEL_TYPE_BASIC : W_7816_CHANNEL_TYPE_LOGICAL),
         pAidBuffer,
         nAidLength);

      if (nError != W_ERROR_OPERATION_PENDING)
      {
         goto return_error;
      }
   }

   return;

return_error:

   PDebugError("PSEDriverOperation: returning %s", PUtilTraceError(nError));

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &sErrorCallback );

   PDFCPostContext2( &sErrorCallback, nError );
}

static void static_PSEGetStatusCompletion(
            tContext* pContext,
            tSESlot* pSlot,
            uint32_t nSwpLineStatus,
            W_ERROR nError )
{
   bool_t bIsPresent;

   if(nError != W_SUCCESS)
   {
      PDebugError("PSEDriverGetStatus: Error %s returned", PUtilTraceError(nError));
      nSwpLineStatus = W_SE_SWP_STATUS_NO_CONNECTION;
      bIsPresent = W_FALSE;
   }
   else
   {
      CDebugAssert(pSlot != null);
      bIsPresent = pSlot->sStatus.bIsPresent;
   }

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0);
   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_COMMUNICATION;

   PDFCDriverPostCC4( pSlot->sStatus.pCallbackDriverCC, bIsPresent, nSwpLineStatus, nError );
}

/* Receive the result of the get parameter */
static void static_PSEGetStatusNalCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         W_ERROR nError)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;
   uint32_t nSWPLinkStatus = W_SE_SWP_STATUS_NO_CONNECTION;

   if(nError == W_SUCCESS)
   {
      if(nLength != 1)
      {
         PDebugError("static_PSEGetStatusNalCompleted: Error of length");
         nLength = W_ERROR_NFC_HAL_COMMUNICATION;
      }
      else
      {
         switch(pSlot->sStatus.aNalParameterBuffer[0])
         {
            case NAL_UICC_SWP_NO_SE:
               nSWPLinkStatus = W_SE_SWP_STATUS_NO_SE;
               break;
            case NAL_UICC_SWP_DOWN:
               nSWPLinkStatus = W_SE_SWP_STATUS_DOWN;
               break;
            case NAL_UICC_SWP_BOOTING:
               nSWPLinkStatus = W_SE_SWP_STATUS_INITIALIZATION;
               break;
            case NAL_UICC_SWP_ERROR:
               nSWPLinkStatus = W_SE_SWP_STATUS_ERROR;
               break;
            case NAL_UICC_SWP_ACTIVE:
               nSWPLinkStatus = W_SE_SWP_STATUS_ACTIVE;
               break;
            default:
               PDebugError("static_PSEGetStatusNalCompleted: Error of value");
               nLength = W_ERROR_NFC_HAL_COMMUNICATION;
               break;
         }
      }
   }

   static_PSEGetStatusCompletion(pContext, pSlot, nSWPLinkStatus, nError );
}

static void static_PSEGetStatusGetInfoCallbackFunction(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nHalSlotIdentifier,
            uint32_t nHalSessionIdentifier,
            uint32_t nActualAtrLength,
            W_ERROR nError )
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   pSlot->sStatus.nAtrLength = 0;
   pSlot->nHalSessionIdentifier = 0;

   if(nError == W_ERROR_FEATURE_NOT_SUPPORTED)
   {
      /* HAL not supported, nothing to do */
      nError = W_SUCCESS;
   }
   else if(nError == W_SUCCESS)
   {
      pSlot->sStatus.nAtrLength = nActualAtrLength;
      pSlot->nHalSessionIdentifier = nHalSessionIdentifier;
      if(nHalSessionIdentifier != 0)
      {
         PDebugTrace("static_PSEGetStatusGetInfoCallbackFunction: Card detected on slot %d", nHalSlotIdentifier);
         pSlot->sStatus.bIsPresent = W_TRUE;
      }
      else
      {
         PDebugTrace("static_PSEGetStatusGetInfoCallbackFunction: No card detected on slot %d", nHalSlotIdentifier);
         pSlot->sStatus.bIsPresent = W_FALSE;
      }
   }
   else
   {
      PDebugError("static_PSEGetStatusGetInfoCallbackFunction: Error during detection on slot %d", nHalSlotIdentifier);
      pSlot->sStatus.bIsPresent = W_FALSE;
   }

   /* Check if the slot is connected to SWP */
   if(PSeHalIsSwpSlot(pSlot->nHalSlotIdentifier) != W_FALSE)
   {
      PNALServiceGetParameter(
         pContext,
         NAL_SERVICE_UICC,
         &pSlot->sStatus.sNalOperation,
         NAL_PAR_UICC_SWP,
         pSlot->sStatus.aNalParameterBuffer, sizeof(pSlot->sStatus.aNalParameterBuffer),
         static_PSEGetStatusNalCompleted, pSlot );
   }
   else
   {
      static_PSEGetStatusCompletion(pContext, pSlot, W_SE_SWP_STATUS_NO_CONNECTION, nError );
   }
}

/* See WSEGetStatus() in the API */
void PSEDriverGetStatus(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            tPSEGetStatusCompleted* pCallback,
            void* pCallbackParameter )
{
   W_ERROR nError = W_SUCCESS;
   tDFCDriverCCReference pErrorCC;
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = null;

   PDebugTrace("PSEDriverGetStatus( nSlotIdentifier=%d", nSlotIdentifier);

   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEDriverGetStatus: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto send_error;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEDriverGetStatus: Bad slot identifier");
      nError = W_ERROR_BAD_PARAMETER;
      goto send_error;
   }

   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_COMMUNICATION) != 0)
   {
      PDebugError("PSEDriverGetStatus: operation already active");
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto send_error;
   }

   pSlot->nPendingFlags |= P_SE_FLAG_PENDING_COMMUNICATION;

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->sStatus.pCallbackDriverCC );

#ifdef P_INCLUDE_SE_SECURITY
   if((pSlot->sConstantInfo.nCapabilities & W_SE_FLAG_SE_HAL) != 0)
   {
      PSeHalGetInfo(
         pContext,
         static_PSEGetStatusGetInfoCallbackFunction, pSlot,
         pSlot->nHalSlotIdentifier,
         pSlot->sStatus.aAtrBuffer,
         sizeof(pSlot->sStatus.aAtrBuffer) );
   }
   else
#endif /* P_INCLUDE_SE_SECURITY */
   {
      static_PSEGetStatusGetInfoCallbackFunction(pContext, pSlot, pSlot->nHalSlotIdentifier,
         0, 0, W_ERROR_FEATURE_NOT_SUPPORTED);
   }

   return;

send_error:

   PDebugError("PSEDriverGetStatus: Error %s returned", PUtilTraceError(nError));

   PDFCDriverFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &pErrorCC );

   PDFCDriverPostCC4( pErrorCC, W_FALSE, W_SE_SWP_STATUS_NO_CONNECTION, nError );
}

/* See Header file */
W_ERROR PSEDriverGetAtr(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            uint8_t* pAtrBuffer,
            uint32_t nAtrBufferLength,
            uint32_t* pnAtrLength )
{
   tSESlot* pSlot;
   W_ERROR nError = PHandleGetObject(pContext, hDriverConnection, P_HANDLE_TYPE_SE_CONNECTION, (void**)&pSlot);
   PDebugTrace("PSEDriverGetAtr()");

   if ( (nError != W_SUCCESS) || (pSlot == null) )
   {
      PDebugError("PSEDriverGetAtr: bad handle value");
      return nError;
   }

   if ( (pAtrBuffer == null) || (pnAtrLength == null) )
   {
      PDebugError("PSEDriverGetAtr: bad parameter");
      return W_ERROR_BAD_PARAMETER;
   }

   if(nAtrBufferLength > pSlot->sStatus.nAtrLength)
   {
      nAtrBufferLength = pSlot->sStatus.nAtrLength;
   }

   CMemoryCopy(pAtrBuffer, pSlot->sStatus.aAtrBuffer, nAtrBufferLength);
   *pnAtrLength = nAtrBufferLength;

   /* @todo: Compute the ATR for the SE on DCLB ISO 14443 A or B */

   return W_SUCCESS;
}

/** @see tPNALServiceSendEventCompleted */

static void static_PSEActivateSWPLineCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         W_ERROR nError,
         uint32_t nReceptionCounter)
{
   tSESlot* pSlot = (tSESlot*)pCallbackParameter;

   PDebugTrace("static_PSEActivateSWPLineCompleted()");

   CDebugAssert((pSlot->nPendingFlags & P_SE_FLAG_PENDING_ACTIVATE_SWP) != 0);

   pSlot->nPendingFlags &= ~P_SE_FLAG_PENDING_ACTIVATE_SWP;

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PSEActivateSWPLineCompleted %s", PUtilTraceError(nError));
   }

   /* Does nothing */
}

static W_ERROR static_PSEActivateSWPLine(
            tContext* pContext,
            tSESlot* pSlot)
{
   if(PSeHalIsSwpSlot(pSlot->nHalSlotIdentifier) == W_FALSE)
   {
      PDebugError("static_PSEActivateSWPLine: this SE is not on SWP");
      return W_ERROR_BAD_PARAMETER;
   }

   if((pSlot->nPendingFlags & P_SE_FLAG_PENDING_ACTIVATE_SWP) == 0)
   {
      pSlot->nPendingFlags |= P_SE_FLAG_PENDING_ACTIVATE_SWP;

      /* Request the establishment of the SWP communication */
      PNALServiceExecuteCommand(
         pContext,
         NAL_SERVICE_UICC,
         &pSlot->sActivateSWPLineOperation,
         NAL_CMD_UICC_START_SWP,
         null, 0,
         null, 0,
         static_PSEActivateSWPLineCompleted,
         pSlot );
   }
   else
   {
      PDebugError("static_PSEActivateSWPLine: Activation already in progress");
   }

   return W_SUCCESS;
}

/* See header file */
void PSEDriverNotifyStkRefresh(
         tContext* pContext,
         uint32_t nHalSlotIdentifier,
         uint32_t nCommand,
         const uint8_t* pRefreshFileList,
         uint32_t nRefreshFileListLength)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot;

   if(pSEInstance->bSEInfoIsBuilt == W_FALSE)
   {
      PDebugWarning("PSEDriverNotifyStkRefresh: SE info not built yet, ignoring the message");
      return;
   }

   PDebugTrace("PSEDriverNotifyStkRefresh( nHalSlotIdentifier=%d", nHalSlotIdentifier);

   pSlot = PSEDriverGetSlotFromHalIdentifier(pContext, nHalSlotIdentifier);
   if(pSlot != null)
   {
#ifdef P_INCLUDE_SE_SECURITY
      if(pSlot->pSecurityStackInstance != null)
      {
         PSecurityStackNotifyStkRefresh(
            pContext,
            pSlot->pSecurityStackInstance,
            nCommand,
            pRefreshFileList, nRefreshFileListLength);
      }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
   }
   else
   {
      PDebugError("PSEDriverNotifyStkRefresh: slot not found");
   }
}

/* See header file */
void PSEDriverNotifyStkActivateSwp(
            tContext* pContext,
            uint32_t nHalSlotIdentifier)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot;

   if(pSEInstance->bSEInfoIsBuilt == W_FALSE)
   {
      PDebugWarning("PSEDriverNotifyStkActivateSwp: SE info not built yet, ignoring the message");
      return;
   }

   PDebugTrace("PSEDriverNotifyStkActivateSwp( nHalSlotIdentifier=%d", nHalSlotIdentifier);

   pSlot = PSEDriverGetSlotFromHalIdentifier(pContext, nHalSlotIdentifier);
   if(pSlot != null)
   {
      (void)static_PSEActivateSWPLine(pContext, pSlot);
   }
   else
   {
      PDebugError("PSEDriverNotifyStkActivateSwp: slot not found");
   }
}

/* See header file */
W_ERROR PSEDriverActivateSwpLine(
            tContext* pContext,
            uint32_t nSlotIdentifier)
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = null;

   PDebugTrace("PSEDriverActivateSwpLine( nSlotIdentifier=%d", nSlotIdentifier);

   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEDriverActivateSwpLine: bad NFC Controller mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEDriverActivateSwpLine: Bad slot identifier");
      return W_ERROR_BAD_PARAMETER;
   }

   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   return static_PSEActivateSWPLine(pContext, pSlot);
}

#ifdef P_INCLUDE_SE_SECURITY
/* See header file */
W_ERROR PSEDriverImpersonateAndCheckAidAccess(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            const uint8_t* pAIDBuffer,
            uint32_t nAIDLength,
            const uint8_t* pImpersonationDataBuffer,
            uint32_t nImpersonationDataBufferLength )
{
   tSEInstance* pSEInstance = PContextGetSEInstance( pContext );
   tSESlot* pSlot = null;

   PDebugTrace("PSEDriverImpersonateAndCheckAidAccess( nSlotIdentifier=%d", nSlotIdentifier);

   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PSEDriverImpersonateAndCheckAidAccess: bad NFC Controller mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   CDebugAssert(pSEInstance->bSEInfoIsBuilt != W_FALSE);

   if(nSlotIdentifier >= pSEInstance->nSeNumber)
   {
      PDebugError("PSEDriverImpersonateAndCheckAidAccess: Bad slot identifier");
      return W_ERROR_BAD_PARAMETER;
   }

   if((pAIDBuffer == null) || (nAIDLength < P_7816SM_MIN_AID_LENGTH) ||  (nAIDLength > P_7816SM_MAX_AID_LENGTH)
   || (pImpersonationDataBuffer == null) || (nImpersonationDataBufferLength == 0))
   {
      PDebugError("PSEDriverImpersonateAndCheckAidAccess: Bad parameter");
      return W_ERROR_BAD_PARAMETER;
   }

   pSlot = &pSEInstance->pSlotArray[nSlotIdentifier];

   if(pSlot->sStatus.bIsPresent == W_FALSE)
   {
      PDebugError("PSEDriverImpersonateAndCheckAidAccess: No SE in the slot");
      return W_ERROR_BAD_STATE;
   }

   if(pSlot->pSecurityStackInstance != null)
   {
      return PSecurityStackCheckAidAccess(
         pContext,
         pSlot->pSecurityStackInstance,
         PContextGetCurrentUserInstance(pContext), /* Use current caller user instance */
         pAIDBuffer, nAIDLength,
         pImpersonationDataBuffer, nImpersonationDataBufferLength);
   }

   return W_SUCCESS;
}
#else

/* Dummy function to allow proper link of the solution even if P_INCLUDE_SE_SECURITY is not defined
   (autogen limitation workaround */

/* See header file */
W_ERROR PSEDriverImpersonateAndCheckAidAccess(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            const uint8_t* pAIDBuffer,
            uint32_t nAIDLength,
            const uint8_t* pImpersonationDataBuffer,
            uint32_t nImpersonationDataBufferLength )
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

#endif /* #ifdef P_INCLUDE_SE_SECURITY */

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

