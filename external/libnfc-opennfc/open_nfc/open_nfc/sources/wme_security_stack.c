/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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
   Contains the implementation of the security stack functions
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( SECSTACK )

#include "wme_context.h"

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

#include "wme_security_stack_acl_manager.h"
#include "wme_security_stack_acl_processor.h"

/** @brief The list of the ACL processors to be checked one after the other */
extern tPSecurityStackAclProcessorInterface* g_PSecurityStackAclProcessors[];

/** @brief AID of the PKCS#15 application */
const uint8_t g_aSecurityStackPkcs15Aid[] = { 0xA0, 0x00, 0x00, 0x00, 0x63, 0x50, 0x4B, 0x43, 0x53, 0x2D, 0x31, 0x35 };

/** @brief Size of the AID of the PKCS#15 application */
const uint32_t g_nSecurityStackPkcs15AidSize = sizeof(g_aSecurityStackPkcs15Aid);

/** Structure used by the security stack for each Secure Element */
struct __tSecurityStackInstance
{
   /** The context that created this t7816SmInstance */
   tContext* pContext;
   /** The identifier of this Secure Element */
   uint32_t nHalSlotIdentifier;
   /** The default principal list */
   const tCSecurityDefaultPrincipal* pDefaultPrincipalList;
   /** The default principal number */
   uint32_t nDefaultPrincipalNumber;
   /** Flag indicating if the secure element is a UICC */
   bool_t bIsUicc;
   /** The update strategy */
   uint32_t nUpdateStrategy;
   /** Flag indicating whether the SE contains a PKCS#15 application */
   bool_t bPkcs15IsPresent;
   /** The current user instance */
   tUserInstance* pCurrentUserInstance;
   /** Data structure used to read the EF(DIR) file */
   struct {
      /** The buffer containing the APDU command sent to the SE */
      uint8_t aReaderToCardBuffer[20];
      /** The buffer containing the APDU response returned by the SE */
      uint8_t aCardToReaderBuffer[(256+2)+2]; /* 2 padding bytes */
      /** The number of EF(DIR) records */
      uint32_t nEfDirRecordCount;
      /** The 1-based next EF(DIR) record that must be read */
      uint32_t nEfDirRecordIndex;
      /** The path to the DF(PKCS15) directory */
      uint16_t aDfPkcs15Path[4];
      /** The number of path elements in the DF(PKCS15) directory */
      uint32_t nDfPkcs15PathCount;
      /** The 0-based next path element that must be selected */
      uint32_t nDfPkcs15PathIndex;
   } sReadEfDir;
   /** The current ACL Processor interface */
   tPSecurityStackAclProcessorInterface* pAclProcessorInterface;
   /** The current ACL Processor instance */
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance;
   struct {
      tPSecurityStackAclProcessorExchangeApduCallbackFunction* pCallback;
      void* pCallbackParameter1;
      void* pCallbackParameter2;
      uint8_t* pReceivedApduBuffer;
   } sAclProcessor;
   /** The current ACL (or null, if the ACL has not been read successfully) */
   tSecurityStackACL* pAcl;
   /** The ACL reading context */
   struct
   {
      /** The error to be returned (once the ACL has been read) */
      uint32_t nError;
      /** The current state of the state machine */
      uint32_t nState;
      /** The callback to be called (once the ACL has been read) */
      tDFCCallbackContext sCallbackContext;
   }  sReadAcl;
   /** The interface of the SE */
   tP7816SmInterface* pSeSmInterface;
   /** The instance of the SE ISO-7816 State Machine */
   tP7816SmInstance* pSeSmInstance;
   /** The logical used to communicate with the SE (P_7816SM_NULL_CHANNEL if none) */
   uint32_t nSeLogicalChannelReference;
   /** The data structure used when processing application selection with an empty AID */
   struct
   {
      uint32_t nAidLength;
      uint8_t aAid[16];
      tPBasicGenericDataCallbackFunction* pCallback;
      void* pCallbackParameter;
   }  sSelectDefaultAid;
};

/** @brief Checks whether a principal is a default principal */
static W_ERROR static_PSecurityStackCheckDefaultPrincipal(
            tContext* pContext,
            tUserInstance* pUserInstance,
            tSecurityStackInstance* pSecurityStackInstance,
            const uint8_t* pImpersonationDataBuffer,
            uint32_t nImpersonationDataBufferLength)
{
   uint32_t nIndex;

   if (pSecurityStackInstance->pDefaultPrincipalList != null)
   {
      for(nIndex = 0; nIndex < pSecurityStackInstance->nDefaultPrincipalNumber; nIndex++)
      {
         if(PSecurityManagerDriverCheckIdentity(
            pContext,
            pUserInstance,
            pSecurityStackInstance->nHalSlotIdentifier,
            pSecurityStackInstance->pDefaultPrincipalList[nIndex].pDefaultPrincipalBuffer,
            pSecurityStackInstance->pDefaultPrincipalList[nIndex].nDefaultPrincipalBufferLength,
            pImpersonationDataBuffer, nImpersonationDataBufferLength) == W_SUCCESS)
         {
            return W_SUCCESS;
         }
      }
   }

   return W_ERROR_SECURITY;
}

/**
 * @brief Callback function called when the hooked logical channel is closed.
 * This occurs if the FCI returned by the SELECT[AID=empty] command contains an application AID
 * that is not granted by the Security Stack.
 */
static void static_PSecurityStackHookSmCloseLogicalChannelCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  W_ERROR nResult)
{
   tP7816SmInstance* pInstance = (tP7816SmInstance*)pCallbackParameter;

   PDebugTrace("static_PSecurityStackHookSmCloseLogicalChannelCompleted()");

   pInstance->sSelectDefaultAid.pCallback(pContext,
      pInstance->sSelectDefaultAid.pCallbackParameter,
      P_7816SM_NULL_CHANNEL,
      W_ERROR_SECURITY);
}

/**
 * @brief Callback function called when the hooked logical channel is opened.
 * This occurs in case the answer to the SELECT[AID=empty] command needs to be checked
 * by the Security Stack.
 */
static void static_PSecurityStackHookSmOpenLogicalChannelCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nChannelReference,
                  W_ERROR nResult)
{
   tP7816SmInstance* pInstance = (tP7816SmInstance*)pCallbackParameter;

   PDebugTrace("static_PSecurityStackHookSmOpenLogicalChannelCompleted()");

   if (nResult == W_SUCCESS)
   {
      uint8_t aAid[P_SECSTACK_AID_MAX_LENGTH];
      uint32_t nAidLength = 0;

      /* Get the AID of the newly selected application */
      nResult = pInstance->pSeSmInterface->pGetData(
         pContext,
         pInstance->pSeSmInstance,
         nChannelReference,
         P_7816SM_DATA_TYPE_AID,
         aAid, sizeof(aAid),
         &nAidLength);

      if (nResult != W_SUCCESS)
      {
         PDebugTrace("static_PSecurityStackHookSmOpenLogicalChannelCompleted: Failed to get the Application AID");
         goto close_channel;
      }

      if (nAidLength == 0)
      {
         PDebugTrace("static_PSecurityStackHookSmOpenLogicalChannelCompleted: The Application AID is unknown");
      }

      /* Check that this was a real partial AID */
      if ((pInstance->sSelectDefaultAid.nAidLength != 0) /* Discard default AID */
      &&  (nAidLength != 0) /* Discard unknown AID */
      &&  (pInstance->sSelectDefaultAid.nAidLength < nAidLength)
      &&  (CMemoryCompare(pInstance->sSelectDefaultAid.aAid, aAid, pInstance->sSelectDefaultAid.nAidLength) != 0))
      {
         PDebugTrace("static_PSecurityStackHookSmOpenLogicalChannelCompleted: The partial AID passed to SELECT is not compatible with the AID returned in the FCI");
         goto close_channel;
      }

      if ((nAidLength == 0) /* Check whether a rule with all AIDs exists */
      ||  (pInstance->sSelectDefaultAid.nAidLength != nAidLength)
      ||  (CMemoryCompare(pInstance->sSelectDefaultAid.aAid, aAid, nAidLength) != 0))
      {
         /* Check the ACL (again) if the AID returned in the FCI was different from
            the AID passed to the SELECT command */

         nResult = PSecurityStackCheckAcl(pInstance->pContext,
            pInstance->nHalSlotIdentifier,
            pInstance->pCurrentUserInstance,
            pInstance->pAcl,
            aAid, nAidLength,
            (const uint8_t*)null,
            W_TRUE);

         if (nResult != W_SUCCESS)
         {
            PDebugError("static_PSecurityStackHookSmOpenLogicalChannelCompleted: PSecurityStackCheckAcl failed with error %s", PUtilTraceError(nResult));
            goto close_channel;
         }
      }
      else
      {
         PDebugTrace("static_PSecurityStackHookSmOpenLogicalChannelCompleted: The AID passed to SELECT and returned in the FCI are the same. No need to check the ACL again");
      }
   }

   pInstance->sSelectDefaultAid.pCallback(pContext,
      pInstance->sSelectDefaultAid.pCallbackParameter,
      nChannelReference,
      nResult);
   return;

close_channel: /* And notify W_ERROR_SECURITY to the caller */

   nResult = pInstance->pSeSmInterface->pCloseChannel(pContext,
      pInstance->pSeSmInstance,
      nChannelReference,
      static_PSecurityStackHookSmCloseLogicalChannelCompleted,
      pInstance);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      static_PSecurityStackHookSmCloseLogicalChannelCompleted(pContext, pInstance, nResult);
   }
}

/** See tP7816SmOpenLogicalChannel */
static W_ERROR static_PSecurityStackSmOpenChannel(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint32_t nType,
                  const uint8_t* pAid,
                  uint32_t nAidLength)
{
   PDebugTrace("static_PSecurityStackSmOpenChannel()");

   if (pInstance == null)
   {
      PDebugError("static_PSecurityStackSmOpenChannel: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pCallback == null) && (nType != W_7816_CHANNEL_TYPE_RAW))
   {
      PDebugError("static_PSecurityStackSmOpenChannel: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nType != W_7816_CHANNEL_TYPE_RAW) && (nType != W_7816_CHANNEL_TYPE_BASIC) && (nType != W_7816_CHANNEL_TYPE_LOGICAL))
   {
      PDebugError("static_PSecurityStackSmOpenChannel: nType (%u) is incorrect", nType);
      return W_ERROR_BAD_PARAMETER;
   }

   switch(nType)
   {
      default:
      {
         PDebugError("static_PSecurityStackSmOpenChannel: nType (%u) is incorrect", nType);
         return W_ERROR_BAD_PARAMETER;
      }

      case W_7816_CHANNEL_TYPE_RAW:
      {
         /*
          * It is forbidden to open the raw channel of a UICC.
          */
         if (pInstance->bIsUicc != W_FALSE)
         {
            PDebugError("static_PSecurityStackSmOpenChannel: Forbidden to open the UICC raw channel");
            return W_ERROR_SECURITY;
         }

         /*
          * It is forbidden to open the raw channel of a Secure Element that contains a PKCS#15 application.
          */
         if (pInstance->bPkcs15IsPresent != W_FALSE)
         {
            PDebugError("static_PSecurityStackSmOpenChannel: Forbidden to open the raw channel (A PKCS#15 application is present)");
            return W_ERROR_SECURITY;
         }

         goto open_channel;
      }

      case W_7816_CHANNEL_TYPE_BASIC:
      {
         /*
          * It is forbidden to open the basic channel of a UICC.
          */
         if (pInstance->bIsUicc != W_FALSE)
         {
            PDebugError("static_PSecurityStackSmOpenChannel: Forbidden to open the UICC basic channel");
            return W_ERROR_SECURITY;
         }

         break;
      }

      case W_7816_CHANNEL_TYPE_LOGICAL:
      {
         /* No restriction */
         break;
      }

   }

   if (pInstance->bPkcs15IsPresent == W_FALSE)
   {
#ifdef P_NO_UICC_ACCESS_BY_DEFAULT
      if (pInstance->bIsUicc != W_FALSE)
      {
         /* Access is forbidden on UICC if there is no PKCS#15 application */
         PDebugError("static_PSecurityStackSmOpenChannel: Forbidden to send APDU command to UICC (No PKCS#15 application)");
         return W_ERROR_SECURITY;
      }
#endif /* P_NO_UICC_ACCESS_BY_DEFAULT */
   }
   else if (static_PSecurityStackCheckDefaultPrincipal(pContext, pInstance->pCurrentUserInstance, pInstance, (const uint8_t*)null, 0) == W_SUCCESS)
   {
      PDebugTrace("static_PSecurityStackSmOpenChannel: The caller is a default principal. Access is granted.");
      /* FALL THROUGH */
   }
   else if (pInstance->pAcl == (tSecurityStackACL*)null)
   {
      /* Access is forbidden if PKCS#15 application data are corrupted */
      PDebugError("static_PSecurityStackSmOpenChannel: Forbidden to send APDU command (PKCS#15 application is corrupted)");
      return W_ERROR_SECURITY;
   }
   else
   {
      /* Filter the AID against the ACL */
      W_ERROR nResult = PSecurityStackCheckAcl(pInstance->pContext,
         pInstance->nHalSlotIdentifier,
         pInstance->pCurrentUserInstance,
         pInstance->pAcl,
         pAid, nAidLength,
         (const uint8_t*)null,
         W_FALSE);

      if (nResult == W_ERROR_MISSING_INFO)
      {
         /* Remember the AID */
         pInstance->sSelectDefaultAid.nAidLength = nAidLength;
         if (nAidLength == 0)
         {
            CMemoryFill(pInstance->sSelectDefaultAid.aAid, 0, sizeof(pInstance->sSelectDefaultAid.aAid));
         }
         else
         {
            if (nAidLength > sizeof(pInstance->sSelectDefaultAid.aAid))
            {
               nAidLength = sizeof(pInstance->sSelectDefaultAid.aAid);
            }

            CMemoryCopy(pInstance->sSelectDefaultAid.aAid, pAid, nAidLength);
         }

         /* Install hook for processing the answer to the SELECT[AID=empty] command */
         pInstance->sSelectDefaultAid.pCallback = pCallback;
         pInstance->sSelectDefaultAid.pCallbackParameter = pCallbackParameter;

         pCallback = static_PSecurityStackHookSmOpenLogicalChannelCompleted;
         pCallbackParameter = pInstance;
      }
      else if (nResult != W_SUCCESS)
      {
         PDebugError("static_PSecurityStackSmOpenLogicalChannel: PSecurityStackCheckAcl returned %s", PUtilTraceError(nResult));
         return nResult;
      }
   }

open_channel:

   /* Forward processing to the SE */
   return pInstance->pSeSmInterface->pOpenChannel(
      pContext,
      pInstance->pSeSmInstance,
      pCallback,
      pCallbackParameter,
      nType,
      pAid,
      nAidLength);
}

/** See tP7816SmCloseChannel */
static W_ERROR static_PSecurityStackSmCloseLogicalChannel(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter)
{
   PDebugTrace("static_PSecurityStackSmCloseLogicalChannel()");

   if (pInstance == null)
   {
      PDebugError("static_PSecurityStackSmCloseLogicalChannel: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Forward processing to the SE */
   return pInstance->pSeSmInterface->pCloseChannel(
      pContext,
      pInstance->pSeSmInstance,
      nChannelReference,
      pCallback,
      pCallbackParameter);
}

/** See tP7816SmExchangeApdu */
static W_ERROR static_PSecurityStackSmExchangeApdu(
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
   PDebugTrace("static_PSecurityStackSmExchangeApdu");

   if (pInstance == null)
   {
      PDebugError("static_PSecurityStackSmExchangeApdu: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pSendApduBuffer == null)
   {
      PDebugError("static_PSecurityStackSmExchangeApdu: pSendApduBuffer == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nSendApduBufferLength < 4)
   {
      PDebugError("static_PSecurityStackSmExchangeApdu: nSendApduBufferLength < 4");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pInstance->bPkcs15IsPresent == W_FALSE)
   {
#ifdef P_NO_UICC_ACCESS_BY_DEFAULT
      if (pInstance->bIsUicc != W_FALSE)
      {
         /* Access is forbidden on UICC if there is no PKCS#15 application */
         PDebugError("static_PSecurityStackSmExchangeApdu: Forbidden to send APDU command to UICC (No PKCS#15 application)");
         return W_ERROR_SECURITY;
      }
#endif /* P_NO_UICC_ACCESS_BY_DEFAULT */
   }
   else
   {
      uint8_t aAid[P_SECSTACK_AID_MAX_LENGTH];
      uint32_t nAidLength = 0;
      W_ERROR nResult;

      /* Check that the APDU command is not sent on the basic logical channel of a UICC */
      if((pInstance->bIsUicc != W_FALSE)
      && (pInstance->pSeSmInterface->pGetData(pContext, pInstance->pSeSmInstance, nChannelReference, P_7816SM_DATA_TYPE_IS_BASIC_CHANNEL, (uint8_t*)null, 0, (uint32_t*)null) == W_SUCCESS))
      {
         PDebugError("static_PSecurityStackSmExchangeApdu: Forbidden to send APDU command on UICC basic logical channel");
         return W_ERROR_SECURITY;
      }

      /* Get the AID of the application selected on this logical channel */
      nResult = pInstance->pSeSmInterface->pGetData(
         pContext,
         pInstance->pSeSmInstance,
         nChannelReference,
         P_7816SM_DATA_TYPE_AID,
         aAid, sizeof(aAid),
         &nAidLength);

      if (nResult != W_SUCCESS)
      {
         PDebugError("static_PSecurityStackSmExchangeApdu: Failed to get selected application AID");
         return W_ERROR_SECURITY;
      }

      if (static_PSecurityStackCheckDefaultPrincipal(pContext, pInstance->pCurrentUserInstance, pInstance, (const uint8_t*)null, 0) == W_SUCCESS)
      {
         PDebugTrace("static_PSecurityStackSmExchangeApdu: The caller is a default principal. Access is granted.");
         /* FALL THROUGH */
      }
      else if (pInstance->pAcl == (tSecurityStackACL*)null)
      {
         /* Access is forbidden if PKCS#15 application data are corrupted */
         PDebugError("static_PSecurityStackSmExchangeApdu: Forbidden to send APDU command (PKCS#15 application is corrupted)");
         return W_ERROR_SECURITY;
      }
      else
      {
         const uint8_t* pSelectAid = aAid;
         uint32_t nSelectAidLength = nAidLength;

         if (PSecurityStackIsDefaultAidSupported(pInstance->pAcl) == W_SUCCESS)
         {
            if (pInstance->pSeSmInterface->pGetData(pContext,
               pInstance->pSeSmInstance,
               pInstance->nSeLogicalChannelReference,
               P_7816SM_DATA_TYPE_IS_DEFAULT_SELECTED,
               (uint8_t*)null, 0, (uint32_t*)null) == W_SUCCESS)
            {
               PDebugTrace("static_PSecurityStackSmExchangeApdu: The current application is a default-selected application.");

               pSelectAid = (const uint8_t*)null;
               nSelectAidLength = 0;
            }
         }

         /* Filter the APDU command against the ACL */
         nResult = PSecurityStackCheckAcl(pContext,
            pInstance->nHalSlotIdentifier,
            pInstance->pCurrentUserInstance,
            pInstance->pAcl,
            pSelectAid, nSelectAidLength,
            pSendApduBuffer,
            W_FALSE);

         if (nResult != W_SUCCESS)
         {
            CDebugAssert(nResult != W_ERROR_MISSING_INFO);
            PDebugError("static_PSecurityStackSmExchangeApdu: PSecurityStackCheckAcl returned %s", PUtilTraceError(nResult));
            return nResult;
         }
      }

      /*
       * In case the AID is not known (eg, SELECT[AID=empty] command and AID not available in the FCI),
       * we'll have aAid !=null  and nAidLength = 0, which shall be rejected by PSecurityStackCheckAcl
       */
      if ((pInstance->nUpdateStrategy == P_SECSTACK_UPDATE_MASTER) && (nAidLength != 0))
      {
         /* Delegate APDU filtering to the ACL Processor */
         if ((pInstance->pAclProcessorInstance != null) && (pInstance->pAclProcessorInterface->pFilterApdu != null))
         {
            (void)pInstance->pAclProcessorInterface->pFilterApdu(pInstance->pAclProcessorInstance,
               aAid, nAidLength, pSendApduBuffer, nSendApduBufferLength);
         }
      }
   }

   /* Forward processing to the SE */
   return pInstance->pSeSmInterface->pExchangeApdu(
      pContext,
      pInstance->pSeSmInstance,
      nChannelReference,
      pCallback,
      pCallbackParameter,
      pSendApduBuffer,
      nSendApduBufferLength,
      pReceivedApduBuffer,
      nReceivedApduBufferMaxLength);
}

/** See tP7816SmGetData */
static W_ERROR static_PSecurityStackSmGetData(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  uint32_t nType,
                  uint8_t* pBuffer,
                  uint32_t nBufferMaxLength,
                  uint32_t* pnActualLength)
{
   PDebugTrace("static_PSecurityStackSmGetData()");

   if (pInstance == null)
   {
      PDebugError("static_PSecurityStackSmGetData: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Forward processing to the SE */
   return pInstance->pSeSmInterface->pGetData(
      pContext,
      pInstance->pSeSmInstance,
      nChannelReference,
      nType,
      pBuffer,
      nBufferMaxLength,
      pnActualLength);
}

/** The Security Stack ISO-7816 State Machine interface*/
tP7816SmInterface g_PSecurityStackSmInterface =
{
   static_PSecurityStackSmOpenChannel,
   static_PSecurityStackSmCloseLogicalChannel,
   static_PSecurityStackSmExchangeApdu,
   static_PSecurityStackSmGetData
};


/* See header file */
tSecurityStackInstance* PSecurityStackCreateInstance(
      tContext* pContext,
      uint32_t nHalSlotIdentifier,
      tP7816SmInterface* pSeSmInterface,
      tP7816SmInstance* pSeSmInstance,
      const tCSecurityDefaultPrincipal* pDefaultPrincipalList,
      uint32_t nDefaultPrincipalNumber,
      uint32_t nUpdateStrategy,
      bool_t bIsUicc )
{
   tSecurityStackInstance* pSecurityStackInstance;

#if 0
   /*
    * @debug - HACK used to disable UICC specific checks (raw/basic channels cannot be opened)
    */
   PDebugWarning("***HACK used to disable UICC specific checks *** TO BE REMOVED ***");
   bIsUicc = W_FALSE;
#endif /* 0 */

   PDebugTrace("PSecurityStackCreateInstance(SE#%d)", nHalSlotIdentifier);

   if (pContext == null)
   {
      PDebugError("PSecurityStackCreateInstance: pContext == null");
      return (tSecurityStackInstance*)null;
   }

   if (pSeSmInterface == null)
   {
      PDebugError("PSecurityStackCreateInstance: pSeSmInterface == null");
      return (tSecurityStackInstance*)null;
   }

   if (pSeSmInstance == null)
   {
      PDebugError("PSecurityStackCreateInstance: pSeSmInstance == null");
      return (tSecurityStackInstance*)null;
   }

   switch(nUpdateStrategy)
   {
      default:
      {
         PDebugError("PSecurityStackCreateInstance: Unknown nUpdateStrategy");
         return (tSecurityStackInstance*)null;
      }

      case P_SECSTACK_UPDATE_MASTER:
         PDebugTrace("PSecurityStackCreateInstance: nUpdateStrategy is P_SECSTACK_UPDATE_MASTER");
         break;
      case P_SECSTACK_UPDATE_SLAVE_WITH_NOTIFICATION:
         PDebugTrace("PSecurityStackCreateInstance: nUpdateStrategy is P_SECSTACK_UPDATE_SLAVE_WITH_NOTIFICATION");
         break;
      case P_SECSTACK_UPDATE_SLAVE_NO_NOTIFICATION:
         PDebugTrace("PSecurityStackCreateInstance: nUpdateStrategy is P_SECSTACK_UPDATE_SLAVE_NO_NOTIFICATION");
         break;
   }

   if ((pSecurityStackInstance = (tSecurityStackInstance*)CMemoryAlloc(sizeof(tSecurityStackInstance) )) == null)
   {
      PDebugError("PSecurityStackCreateInstance: Cannot allocate the structure");
      return (tSecurityStackInstance*)null;
   }
   CMemoryFill( pSecurityStackInstance, 0, sizeof(tSecurityStackInstance) );

   pSecurityStackInstance->pContext = pContext;
   pSecurityStackInstance->nHalSlotIdentifier = nHalSlotIdentifier;
   pSecurityStackInstance->pSeSmInterface = pSeSmInterface;
   pSecurityStackInstance->pSeSmInstance = pSeSmInstance;
   pSecurityStackInstance->pDefaultPrincipalList = pDefaultPrincipalList;
   pSecurityStackInstance->nDefaultPrincipalNumber = nDefaultPrincipalNumber;
   pSecurityStackInstance->bIsUicc = bIsUicc;
   pSecurityStackInstance->nUpdateStrategy = nUpdateStrategy;

   return pSecurityStackInstance;
}

/** Frees the ACL Processor instance and the ACL associated with a Security Stack instance */
static void static_PSecurityStackFreeAcl
(
   tSecurityStackInstance* pSecurityStackInstance
)
{
   CDebugAssert(pSecurityStackInstance != null);

   if (pSecurityStackInstance->pAcl != null)
   {
      PSecurityStackAclDestroyInstance(pSecurityStackInstance->pAcl);
      pSecurityStackInstance->pAcl = (tSecurityStackACL*)null;
   }

   if (pSecurityStackInstance->pAclProcessorInstance != null)
   {
      CDebugAssert(pSecurityStackInstance->pAclProcessorInterface != null);
      pSecurityStackInstance->pAclProcessorInterface->pDestroyInstance(pSecurityStackInstance->pAclProcessorInstance);
      pSecurityStackInstance->pAclProcessorInterface = (tPSecurityStackAclProcessorInterface*)null;
      pSecurityStackInstance->pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)null;
   }
}

/* See header file */
void PSecurityStackDestroyInstance(
      tContext* pContext,
      tSecurityStackInstance* pSecurityStackInstance )
{
   PDebugTrace("PSecurityStackDestroyInstance()");

   if (pSecurityStackInstance != null)
   {
      static_PSecurityStackFreeAcl(pSecurityStackInstance);

      CMemoryFree(pSecurityStackInstance);
   }
}

/** See header file */
W_ERROR PSecurityStackSetCurrentUserInstance(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance,
            tUserInstance* pUserInstance)
{
   PDebugTrace("PSecurityStackSetCurrentUserInstance()");

   if (pContext == null)
   {
      PDebugError("PSecurityStackSetCurrentUserInstance: pContext == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pSecurityStackInstance == null)
   {
      PDebugError("PSecurityStackSetCurrentUserInstance: pSecurityStackInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   pSecurityStackInstance->pCurrentUserInstance = pUserInstance;
   return W_SUCCESS;
}

/** The event indicating that the state machine must be started */
#define P_EVENT_INITIAL                    0
/** The event indicating that the state machine has received the answer of the last command */
#define P_EVENT_COMMAND_EXECUTED           1
/* The status indicating that a command is pending */
#define P_STATUS_PENDING_EXECUTED          0x0200
/** The state machine is stopped */
#define P_STATE_IDLE                       0x0000
/** The state machine must send the MANAGE CHANNEL [open] command and the SELECT command to select the PKCS#15 application */
#define P_STATE_OPEN_CHANNEL_SELECT_PKCS15_APPLICATION        0x0001
/** The state machine must process the successful selection of the PKCS#15 application */
#define P_STATE_PROCESS_PKCS15_APPLICATION 0x0002
/** The state machine must process the reading of the ACL */
#define P_STATE_READ_ACL                   0x0003
/** The state machine must open the raw channel and select the MF */
#define P_STATE_SELECT_MF                  0x0010
/** The state machine must select the EF(DIR) file */
#define P_STATE_SELECT_EF_DIR              0x0011
/** The state machine must read the first record of the EF(DIR) file */
#define P_STATE_READ_FIRST_EF_DIR_RECORD   0x0012
/** The state machine must read the next record of the EF(DIR) file */
#define P_STATE_READ_NEXT_EF_DIR_RECORD    0x0013
/** The state machine must select the DF of the PKCS#15 application */
#define P_STATE_SELECT_DF_PKCS15           0x0014
/** The state machine must send the MANAGE CHANNEL [close] command */
#define P_STATE_MANAGE_CHANNEL_CLOSE       0x0005
/** The state machine must return a notification to the caller */
#define P_STATE_FINAL                      0x0006

/** Forward declaration */
static void static_PSecurityStackLoadAclStateMachine
(
   tContext* pContext,
   tSecurityStackInstance* pSecurityStackInstance,
   uint32_t nEvent,
   uint32_t nDataLength,
   W_ERROR nResult
);

/** Security stack ACL reading state machine helper function */
static void static_PSecurityStackLoadAclStateMachineOpenLogicalChannelCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nChannelReference,
                  W_ERROR nResult )
{
   tSecurityStackInstance* pSecurityStackInstance = (tSecurityStackInstance*)pCallbackParameter;

   CDebugAssert(pSecurityStackInstance->nSeLogicalChannelReference == P_7816SM_NULL_CHANNEL);
   pSecurityStackInstance->nSeLogicalChannelReference = nChannelReference;

   static_PSecurityStackLoadAclStateMachine(pContext, pSecurityStackInstance,
      P_EVENT_COMMAND_EXECUTED, 0, nResult);
}

/** Security stack ACL reading state machine helper function */
static void static_PSecurityStackLoadAclStateMachineOpenLogicalChannel(
                  tContext* pContext,
                  tSecurityStackInstance* pSecurityStackInstance,
                  const uint8_t* pAid,
                  uint32_t nAidLength)
{
   /* Send the MANAGE CHANNEL [open] and SELECT [AID] commands */
   W_ERROR nResult = pSecurityStackInstance->pSeSmInterface->pOpenChannel(
      pContext,
      pSecurityStackInstance->pSeSmInstance,
      static_PSecurityStackLoadAclStateMachineOpenLogicalChannelCompleted,
      pSecurityStackInstance,
      W_7816_CHANNEL_TYPE_LOGICAL,
      pAid, nAidLength);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      /* Beware! This is a re-entrant function call for the "LoadAcl State Machine"*/
      static_PSecurityStackLoadAclStateMachineOpenLogicalChannelCompleted(
         pContext,
         pSecurityStackInstance,
         P_7816SM_NULL_CHANNEL,
         nResult);

      return;
   }
}

/** Security stack ACL reading state machine helper function */
static void static_PSecurityStackLoadAclStateMachineExchangeApduCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nDataLength,
                  W_ERROR nResult )
{
   tSecurityStackInstance* pSecurityStackInstance = (tSecurityStackInstance*)pCallbackParameter;

   static_PSecurityStackLoadAclStateMachine(pContext, pSecurityStackInstance,
      P_EVENT_COMMAND_EXECUTED, nDataLength, nResult);
}

/** Security stack ACL reading state machine helper function */
static void static_PSecurityStackLoadAclStateMachineExchangeApdu(
                  tContext* pContext,
                  tSecurityStackInstance* pSecurityStackInstance,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength)
{
   /* send the APDU command */
   W_ERROR nResult = pSecurityStackInstance->pSeSmInterface->pExchangeApdu(
      pContext,
      pSecurityStackInstance->pSeSmInstance,
      pSecurityStackInstance->nSeLogicalChannelReference,
      static_PSecurityStackLoadAclStateMachineExchangeApduCompleted,
      pSecurityStackInstance,
      pSendApduBuffer,
      nSendApduBufferLength,
      pReceivedApduBuffer,
      nReceivedApduBufferMaxLength);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      /* Beware! This is a re-entrant function call */
      static_PSecurityStackLoadAclStateMachineExchangeApduCompleted(
         pContext,
         pSecurityStackInstance,
         0,
         nResult);
   }
}

/** @brief Sends an APDU command to select a file by its file identifier */
static void static_PSecurityStackSelectFile
(
   tContext* pContext,
   tSecurityStackInstance* pSecurityStackInstance,
   uint16_t nFid
)
{
   uint8_t* aReaderToCardBuffer = pSecurityStackInstance->sReadEfDir.aReaderToCardBuffer;

   aReaderToCardBuffer[0] = P_7816SM_CLA;
   aReaderToCardBuffer[1] = P_7816SM_INS_SELECT;
   aReaderToCardBuffer[2] = P_7816SM_P1_SELECT_FILE;
   aReaderToCardBuffer[3] = P_7816SM_P2_SELECT_FILE_WITH_FCP;
   aReaderToCardBuffer[4] = 0x02;
   aReaderToCardBuffer[5] = (uint8_t)(nFid >> 8);
   aReaderToCardBuffer[6] = (uint8_t)(nFid);

   static_PSecurityStackLoadAclStateMachineExchangeApdu(
      pContext,
      pSecurityStackInstance,
      aReaderToCardBuffer,
      7,
      pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer,
      sizeof(pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer));
}

/** @brief Sends an APDU command to read a file record */
static void static_PSecurityStackReadRecord
(
   tContext* pContext,
   tSecurityStackInstance* pSecurityStackInstance,
   uint32_t nRecordNo
)
{
   uint8_t* aReaderToCardBuffer = pSecurityStackInstance->sReadEfDir.aReaderToCardBuffer;

   aReaderToCardBuffer[0] = P_7816SM_CLA;
   aReaderToCardBuffer[1] = P_7816SM_INS_READ_RECORD;
   aReaderToCardBuffer[2] = (uint8_t)nRecordNo;
   aReaderToCardBuffer[3] = 0x04; /* Current file */
   aReaderToCardBuffer[4] = 0x00;

   static_PSecurityStackLoadAclStateMachineExchangeApdu(
      pContext,
      pSecurityStackInstance,
      aReaderToCardBuffer,
      5,
      pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer,
      sizeof(pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer));
}

static void static_PSecurityStackLoadAclStateMachineCloseLogicalChannelCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  W_ERROR nResult )
{
   tSecurityStackInstance* pSecurityStackInstance = (tSecurityStackInstance*)pCallbackParameter;

   CDebugAssert(pSecurityStackInstance->nSeLogicalChannelReference != P_7816SM_NULL_CHANNEL);
   pSecurityStackInstance->nSeLogicalChannelReference = P_7816SM_NULL_CHANNEL;

   static_PSecurityStackLoadAclStateMachine(pContext, pSecurityStackInstance,
      P_EVENT_COMMAND_EXECUTED, 0, nResult);
}

static void static_PSecurityStackLoadAclStateMachineCloseLogicalChannel(
                  tContext* pContext,
                  tSecurityStackInstance* pSecurityStackInstance)
{
   /* Send a MANAGE CHANNEL [close] command */
   W_ERROR nResult = pSecurityStackInstance->pSeSmInterface->pCloseChannel(
      pContext,
      pSecurityStackInstance->pSeSmInstance,
      pSecurityStackInstance->nSeLogicalChannelReference,
      static_PSecurityStackLoadAclStateMachineCloseLogicalChannelCompleted,
      pSecurityStackInstance);

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      /* Beware! This is a re-entrant function call */
      static_PSecurityStackLoadAclStateMachineCloseLogicalChannelCompleted(
         pContext,
         pSecurityStackInstance,
         nResult);

      return;
   }
}

/** @brief Extracts the number of EF(DIR) records from the answer to the SELECT EF(DIR) command */
static W_ERROR static_PSecurityStackExtractEfDirRecordCount(
                  const uint8_t* pFcpBuffer,
                  uint32_t nFcpLength,
                  uint32_t* pnEfDirRecordCount)
{
   uint32_t nError;
   tAsn1Parser sParser;
   tAsn1Parser sIncludedParser;

   /*
    * The answer to the SELECT [file] command contains a FCP that contains at least these fields:
    * 62 L                  FCP template
    *    82 05 xx-xx xx NN  File Descriptor (NN is the record count, 01 to FE)
    *    .. ..
    */

   nError = PAsn1InitializeParser(&sParser, pFcpBuffer, nFcpLength);
   if (nError != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackExtractEfDirRecordCount: Bad TLV format");
      return nError;
   }

   if(PAsn1GetTagValue(&sParser) != 0x62)
   {
      PDebugWarning("static_PSecurityStackExtractEfDirRecordCount: Bad '62' tag");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PAsn1ParseIncludedTlv( &sParser, &sIncludedParser);
   if (nError != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackExtractEfDirRecordCount: Bad included TLV format");
      return nError;
   }

   do
   {
      if (PAsn1GetTagValue(&sIncludedParser) == 0x82)
      {
         const uint8_t* pFileDescriptorBuffer;
         uint32_t nFileDescriptorLength;

         PAsn1GetPointerOnBinaryContent(&sIncludedParser, &pFileDescriptorBuffer, &nFileDescriptorLength);

         if (nFileDescriptorLength == 5)
         {
            *pnEfDirRecordCount = pFileDescriptorBuffer[4];
         }
         else
         {
            /* Let's try reading as many records as possible */
            *pnEfDirRecordCount = 255;
         }

         return W_SUCCESS;
      }
   } while(PAsn1MoveToNextTlv(&sIncludedParser) == W_SUCCESS);


   PDebugWarning("static_PSecurityStackExtractEfDirRecordCount: Tag '82' not found");
   return W_ERROR_BAD_PARAMETER;
}

/** @brief Extracts the EF(PKCS15) path, if any, from an EF(DIR) record */
static W_ERROR static_PSecurityStackExtractEfDirRecordPkcs15Path(
                  const uint8_t* pEfDirRecordBuffer,
                  uint32_t nEfDirRecordLength,
                  uint16_t* pDfPkcs15Path,
                  uint32_t nMaxDfPkcs15PathCount,
                  uint32_t* pnActualDfPkcs15PathCount)
{
   uint32_t nError;
   tAsn1Parser sParser;
   tAsn1Parser sIncludedParser;

   bool_t bAidFound = W_FALSE;

   /*
    * The contents of an EF(DIR) record contains at least these fields:
    * 61 L      Record template
    *    4F L   Application AID
    *    51 L   Path
    *    .. ..
    */

   nError = PAsn1InitializeParser(&sParser, pEfDirRecordBuffer, nEfDirRecordLength);
   if (nError != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackExtractEfDirRecordPkcs15Path: Bad TLV format");
      return nError;
   }

   if(PAsn1GetTagValue(&sParser) != 0x61)
   {
      PDebugWarning("static_PSecurityStackExtractEfDirRecordPkcs15Path: Bad '61' tag");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PAsn1ParseIncludedTlv( &sParser, &sIncludedParser);
   if (nError != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackExtractEfDirRecordPkcs15Path: Bad included TLV format");
      return nError;
   }

   do
   {
      if (PAsn1GetTagValue(&sIncludedParser) == 0x4F)
      {
         const uint8_t* pAidBuffer;
         uint32_t nAidLength;

         PAsn1GetPointerOnBinaryContent(&sIncludedParser, &pAidBuffer, &nAidLength);

         bAidFound = ((nAidLength == sizeof(g_aSecurityStackPkcs15Aid))
            && (CMemoryCompare(g_aSecurityStackPkcs15Aid, pAidBuffer, nAidLength) == 0));

         break;
      }
   } while(PAsn1MoveToNextTlv(&sIncludedParser) == W_SUCCESS);

   if (bAidFound == W_FALSE)
   {
      PDebugTrace("static_PSecurityStackExtractEfDirRecordPkcs15Path: Tag '4F' not found");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PAsn1ParseIncludedTlv( &sParser, &sIncludedParser);
   if (nError != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackExtractEfDirRecordPkcs15Path: Bad included TLV format");
      return nError;
   }

   do
   {
      if (PAsn1GetTagValue(&sIncludedParser) == 0x51)
      {
         const uint8_t* pPathBuffer;
         uint32_t nPathLength;

         PAsn1GetPointerOnBinaryContent(&sIncludedParser, &pPathBuffer, &nPathLength);

         if ((nPathLength == 0) || ((nPathLength % 2) != 0))
         {
            PDebugWarning("static_PSecurityStackExtractEfDirRecordPkcs15Path: Tag '51' is malformed");
            return W_ERROR_BAD_PARAMETER;
         }

         if (nPathLength > (nMaxDfPkcs15PathCount * 2))
         {
            PDebugWarning("static_PSecurityStackExtractEfDirRecordPkcs15Path: Tag '51' is too long");
            return W_ERROR_BAD_PARAMETER;
         }

         *pnActualDfPkcs15PathCount = 0;

         while(nPathLength > 0)
         {
            uint16_t nPathElement = (pPathBuffer[0] << 8) + pPathBuffer[1];
            pPathBuffer+=2;
            nPathLength-=2;

            pDfPkcs15Path[(*pnActualDfPkcs15PathCount)++] = nPathElement;
         }

         return W_SUCCESS;
      }
   } while(PAsn1MoveToNextTlv(&sIncludedParser) == W_SUCCESS);

   PDebugWarning("static_PSecurityStackExtractEfDirRecordPkcs15Path: Tag '51' not found");
   return W_ERROR_BAD_PARAMETER;
}

static void static_PSecurityStackAclProcessorExchangeApduCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nDataLength,
                  W_ERROR nResult)

{
   tSecurityStackInstance* pSecurityStackInstance = (tSecurityStackInstance*)pCallbackParameter;

   CDebugAssert(pSecurityStackInstance != null);

   pSecurityStackInstance->sAclProcessor.pCallback(
      pSecurityStackInstance->sAclProcessor.pCallbackParameter1,
      pSecurityStackInstance->sAclProcessor.pCallbackParameter2,
      nDataLength, nResult);
}

static W_ERROR static_PSecurityStackAclProcessorExchangeApdu(
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  tPSecurityStackAclProcessorExchangeApduCallbackFunction* pExchangeApduCallback,
                  void* pExchangeApduCallbackParameter1,
                  void* pExchangeApduCallbackParameter2,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength)
{
   tContext* pContext = (tContext*)pCallbackParameter1;
   tSecurityStackInstance* pSecurityStackInstance = (tSecurityStackInstance*)pCallbackParameter2;

   CDebugAssert(pContext != null);
   CDebugAssert(pSecurityStackInstance != null);

   pSecurityStackInstance->sAclProcessor.pCallback = pExchangeApduCallback;
   pSecurityStackInstance->sAclProcessor.pCallbackParameter1 = pExchangeApduCallbackParameter1;
   pSecurityStackInstance->sAclProcessor.pCallbackParameter2 = pExchangeApduCallbackParameter2;
   pSecurityStackInstance->sAclProcessor.pReceivedApduBuffer = pReceivedApduBuffer;

   return pSecurityStackInstance->pSeSmInterface->pExchangeApdu(
      pContext,
      pSecurityStackInstance->pSeSmInstance,
      pSecurityStackInstance->nSeLogicalChannelReference,
      static_PSecurityStackAclProcessorExchangeApduCompleted,
      pSecurityStackInstance,
      pSendApduBuffer,
      nSendApduBufferLength,
      pReceivedApduBuffer,
      nReceivedApduBufferMaxLength);
}

static void static_PSecurityStackAclProcessorReadAclCompleted(
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  tSecurityStackACL* pAcl,
                  W_ERROR nResult)
{
   tContext* pContext = (tContext*)pCallbackParameter1;
   tSecurityStackInstance* pSecurityStackInstance = (tSecurityStackInstance*)pCallbackParameter2;

   CDebugAssert(pContext != null);
   CDebugAssert(pSecurityStackInstance != null);

   if ((nResult == W_SUCCESS) && (pAcl == null))
   {
      /* The ACL has not changed since the last read operation - nothing to do */
      pSecurityStackInstance->sReadAcl.nError = W_SUCCESS;

      /* This enables APDU filtering (APDU commands shall be checked against the ACL) */
      CDebugAssert(pSecurityStackInstance->bPkcs15IsPresent != W_FALSE);
      CDebugAssert(pSecurityStackInstance->pAcl != null);
   }
   else if (nResult == W_SUCCESS)
   {
      /* The ACL has changed since the last read operation */
      pSecurityStackInstance->sReadAcl.nError = W_SUCCESS;

      /* Delete the previous ACL */
      if (pSecurityStackInstance->pAcl != null)
      {
         PSecurityStackAclDestroyInstance(pSecurityStackInstance->pAcl);
         pSecurityStackInstance->pAcl = (tSecurityStackACL*)null;
      }

      /* Remember the new ACL */
      pSecurityStackInstance->pAcl = pAcl;

      /* This enables APDU filtering (APDU commands shall be checked against the ACL) */
      pSecurityStackInstance->bPkcs15IsPresent = W_TRUE;
      CDebugAssert(pSecurityStackInstance->pAcl != null);

      PSecurityStackAclDumpContents(pSecurityStackInstance->pAcl);
   }
   else if (nResult == W_ERROR_ITEM_NOT_FOUND)
   {
      /* The PKCS15 application is not present in the SE */
      pSecurityStackInstance->sReadAcl.nError = nResult;

      /* Free ACL and destroy ACL Processor */
      static_PSecurityStackFreeAcl(pSecurityStackInstance);

      /* This disables APDU filtering (all APDU commands shall be accepted) */
      pSecurityStackInstance->bPkcs15IsPresent = W_FALSE;
      CDebugAssert(pSecurityStackInstance->pAcl == null);
   }
   else
   {
      /* An error occurred during the ACL reading (The PKCS15 application is present) */
      pSecurityStackInstance->sReadAcl.nError = nResult;
      CDebugAssert(pAcl == null);

      /* Free ACL and destroy ACL Processor */
      static_PSecurityStackFreeAcl(pSecurityStackInstance);

      /* This forces W_ERROR_SECURITY to be returned during APDU filtering */
      pSecurityStackInstance->bPkcs15IsPresent = W_TRUE;
      CDebugAssert(pSecurityStackInstance->pAcl == null);
   }

   static_PSecurityStackLoadAclStateMachine(pContext, pSecurityStackInstance,
      P_EVENT_COMMAND_EXECUTED, 0, nResult);
}

static W_ERROR static_PSecurityStackReadAcl(
   tContext* pContext,
   tSecurityStackInstance* pSecurityStackInstance,
   const uint8_t* pAnswerToSelectBuffer,
   uint32_t nAnswerToSelectLength,
   bool_t bIsSelectedByAid
)
{
   W_ERROR nResult;
   uint32_t nIndex;
   bool_t bAutoResetTouched = W_TRUE;

   if (pSecurityStackInstance->nUpdateStrategy == P_SECSTACK_UPDATE_SLAVE_NO_NOTIFICATION)
   {
      bAutoResetTouched = W_FALSE;

      /* Force the reading of the ACL */
      if (pSecurityStackInstance->pAclProcessorInstance != null)
      {
         PDebugLog("PSecurityStackLoadAcl: Strategy P_SECSTACK_UPDATE_SLAVE_NO_NOTIFICATION: The ACL may have changed.");
         goto read_acl;
      }
      else
      {
         static_PSecurityStackFreeAcl(pSecurityStackInstance);
      }
   }
   else if (pSecurityStackInstance->pAclProcessorInstance != null)
   {
      /* Check whether the Answer-to-select is still compatible with this ACL Processor */
      nResult = pSecurityStackInstance->pAclProcessorInterface->pUpdateInstance(
         pSecurityStackInstance->pAclProcessorInstance,
         pAnswerToSelectBuffer,
         nAnswerToSelectLength,
         bIsSelectedByAid);

      if (nResult == W_SUCCESS)
      {
         /* There is no need to re-read the ACL */
         PDebugLog("PSecurityStackLoadAcl: The ACL has not changed. No need to read it again.");

         /* Set the next state to be reached */
         pSecurityStackInstance->sReadAcl.nState = P_STATE_READ_ACL | P_STATUS_PENDING_EXECUTED;

         static_PSecurityStackAclProcessorReadAclCompleted(
            pContext,
            pSecurityStackInstance,
            (tSecurityStackACL*)null,
            W_SUCCESS);

         return W_ERROR_OPERATION_PENDING;
      }

      if (nResult == W_ERROR_BAD_STATE)
      {
         /* The ACL must be read again */
         PDebugLog("PSecurityStackLoadAcl: The ACL may have changed.");
         goto read_acl;
      }

      /* An unexpected error occurred. Free current ACL and try to find an ACL Processor */
      static_PSecurityStackFreeAcl(pSecurityStackInstance);
      /* FALL THROUGH */
   }

   CDebugAssert(pSecurityStackInstance->pAclProcessorInterface == null);
   CDebugAssert(pSecurityStackInstance->pAclProcessorInstance == null);

   /* Try to find an appropriate ACL processor */
   for(nIndex = 0; g_PSecurityStackAclProcessors[nIndex] != null; nIndex++)
   {
      tPSecurityStackAclProcessorInstance* pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)null;
      tPSecurityStackAclProcessorInterface* pAclProcessorInterface = g_PSecurityStackAclProcessors[nIndex];
      CDebugAssert(pAclProcessorInterface != null);

      nResult = pAclProcessorInterface->pCreateInstance(pAnswerToSelectBuffer, nAnswerToSelectLength, bIsSelectedByAid, bAutoResetTouched, &pAclProcessorInstance);
      if (nResult == W_SUCCESS)
      {
         pSecurityStackInstance->pAclProcessorInterface = pAclProcessorInterface;
         pSecurityStackInstance->pAclProcessorInstance = pAclProcessorInstance;
         break;
      }
   }

   if (pSecurityStackInstance->pAclProcessorInstance == null)
   {
      /* There is no appropriate ACL processor */
      PDebugLog("PSecurityStackLoadAcl: There is no suitable ACL processor.");
      return W_ERROR_ITEM_NOT_FOUND;
   }

read_acl:

   CDebugAssert(pSecurityStackInstance->pAclProcessorInterface != null);
   CDebugAssert(pSecurityStackInstance->pAclProcessorInstance != null);

   /* Set the next state to be reached */
   pSecurityStackInstance->sReadAcl.nState = P_STATE_READ_ACL | P_STATUS_PENDING_EXECUTED;

   /* Read next data */
   nResult = pSecurityStackInstance->pAclProcessorInterface->pReadAcl(
      pSecurityStackInstance->pAclProcessorInstance,
      static_PSecurityStackAclProcessorExchangeApdu,
      pContext,
      pSecurityStackInstance,
      static_PSecurityStackAclProcessorReadAclCompleted,
      pContext,
      pSecurityStackInstance,
      PNALServiceDriverGetCurrentTime(pContext));

   if ((nResult != W_SUCCESS) && (nResult != W_ERROR_OPERATION_PENDING))
   {
      static_PSecurityStackAclProcessorReadAclCompleted(
         pContext,
         pSecurityStackInstance,
         (tSecurityStackACL*)null,
         nResult);
   }
   return nResult;
}

static void static_PSecurityStackLoadAclStateMachine
(
   tContext* pContext,
   tSecurityStackInstance* pSecurityStackInstance,
   uint32_t nEvent,
   uint32_t nDataLength,
   W_ERROR nResult
)
{
   switch ( nEvent )
   {
      case P_EVENT_INITIAL:
         pSecurityStackInstance->sReadAcl.nError = W_SUCCESS;
         pSecurityStackInstance->sReadAcl.nState = P_STATE_OPEN_CHANNEL_SELECT_PKCS15_APPLICATION;
         break;

      case P_EVENT_COMMAND_EXECUTED:
         pSecurityStackInstance->sReadAcl.nState &= ~P_STATUS_PENDING_EXECUTED;
         break;
   }

#if 0
   /*
    * @debug - HACK used to simulate the absence of the PKCS#15 application
    */
   if ((pSecurityStackInstance->sReadAcl.nState == P_STATE_PROCESS_PKCS15_APPLICATION) && (nResult == W_SUCCESS))
   {
      PDebugWarning("***HACK used to simulate the absence of the PKCS#15 application *** TO BE REMOVED ***");
      pSecurityStackInstance->sReadAcl.nState = P_STATE_MANAGE_CHANNEL_CLOSE;
      pSecurityStackInstance->sReadAcl.nError = W_ERROR_ITEM_NOT_FOUND;
      nResult = W_SUCCESS;
   }
#endif /* 0 */

   if (nResult == W_ERROR_TIMEOUT) /* The SE is not responding */
   {
      goto notify_failure;
   }
   else if (nResult != W_SUCCESS)
   {
      /* Close the logical channel (if need be) */
      switch(pSecurityStackInstance->sReadAcl.nState)
      {
         case P_STATE_PROCESS_PKCS15_APPLICATION:
            /* The PKCS#15 application is not present in the SE */
            PDebugTrace("PSecurityStackLoadAcl: The PKCS#15 application is not present in SE#%d",
              pSecurityStackInstance->nHalSlotIdentifier
            );

            /* Setting this error code will delete the current ACL */
            pSecurityStackInstance->sReadAcl.nError = W_ERROR_SECURITY;

            /* Free current ACL Processor and ACL */
            static_PSecurityStackFreeAcl(pSecurityStackInstance);

            if (nResult == W_ERROR_FEATURE_NOT_SUPPORTED)
            {
               /* There is no available logical channel. No need to try to select the MF */
               pSecurityStackInstance->sReadAcl.nError = nResult;
               pSecurityStackInstance->sReadAcl.nState = P_STATE_FINAL;
               break;
            }

            /* The logical channel is already closed. Try to read the MF/EF(DIR)... */
            CDebugAssert(pSecurityStackInstance->nSeLogicalChannelReference == P_7816SM_NULL_CHANNEL);
            pSecurityStackInstance->sReadAcl.nState = P_STATE_SELECT_MF;
            break;

         case P_STATE_SELECT_EF_DIR:
            /* The PKCS#15 application is not present in the SE */
            PDebugTrace("PSecurityStackLoadAcl: The MF directory is not present in SE#%d",
              pSecurityStackInstance->nHalSlotIdentifier
            );

            /* There is no need to close the channel */
            pSecurityStackInstance->sReadAcl.nError = nResult;
            pSecurityStackInstance->sReadAcl.nState = P_STATE_FINAL;
            break;

         case P_STATE_READ_FIRST_EF_DIR_RECORD:
            PDebugTrace("PSecurityStackLoadAcl: The MF/EF(DIR) file is not present in SE#%d",
              pSecurityStackInstance->nHalSlotIdentifier
            );
            break;

         default:
            pSecurityStackInstance->sReadAcl.nError = nResult;
            pSecurityStackInstance->sReadAcl.nState = P_STATE_MANAGE_CHANNEL_CLOSE;
            break;
      }
   }

   switch(pSecurityStackInstance->sReadAcl.nState)
   {
      case P_STATE_OPEN_CHANNEL_SELECT_PKCS15_APPLICATION:
      {
         /* Set the next state to be reached */
         pSecurityStackInstance->sReadAcl.nState = P_STATE_PROCESS_PKCS15_APPLICATION | P_STATUS_PENDING_EXECUTED;

         /* Send the MANAGE CHANNEL [open] and SELECT [AID] commands */
         static_PSecurityStackLoadAclStateMachineOpenLogicalChannel(
            pContext,
            pSecurityStackInstance,
            g_aSecurityStackPkcs15Aid, sizeof(g_aSecurityStackPkcs15Aid));

         return;
      }

      case P_STATE_PROCESS_PKCS15_APPLICATION:
      {
         /* The PKCS#15 application is present! */

         uint8_t aAnswerToSelectBuffer[256+2];
         uint32_t nAnswerToSelectLength = 0;

         /* Get the Answer-to-select response data */
         nResult = pSecurityStackInstance->pSeSmInterface->pGetData(
            pContext,
            pSecurityStackInstance->pSeSmInstance,
            pSecurityStackInstance->nSeLogicalChannelReference,
            P_7816SM_DATA_TYPE_LAST_RESPONSE_APDU,
            aAnswerToSelectBuffer,
            sizeof(aAnswerToSelectBuffer),
            &nAnswerToSelectLength);

         if (nResult != W_SUCCESS)
         {
            PDebugError("static_PSecurityStackLoadAclStateMachine: Failed to get the PKCS#15 application FCI");

            /* Setting this error code will delete the current ACL */
            pSecurityStackInstance->sReadAcl.nError = W_ERROR_SECURITY;

            goto close_logical_channel;
         }

         nResult = static_PSecurityStackReadAcl(
            pContext,
            pSecurityStackInstance,
            aAnswerToSelectBuffer, nAnswerToSelectLength,
            W_TRUE);

         if (nResult != W_ERROR_OPERATION_PENDING)
         {
            goto close_logical_channel;
         }

         return;
      }

      case P_STATE_READ_ACL:
      {
         /* The ACL has been read successfully (or not!) */
         goto close_logical_channel;
      }

      case P_STATE_SELECT_MF:
      {
         PDebugTrace("static_PSecurityStackLoadAclStateMachine: Reading the MF/EF(DIR) file...");

         /* The PKCS#15 application cannot be selected by its AID */
         CDebugAssert(pSecurityStackInstance->nSeLogicalChannelReference == P_7816SM_NULL_CHANNEL);

         /* Set the next state to be reached */
         pSecurityStackInstance->sReadAcl.nState = P_STATE_SELECT_EF_DIR | P_STATUS_PENDING_EXECUTED;

         /* Open the a logical channel and select the MF */
         static_PSecurityStackLoadAclStateMachineOpenLogicalChannel(
            pContext,
            pSecurityStackInstance,
            g_a7816SmMfAid, g_n7816SmMfAidSize);

         return;
      }

      case P_STATE_SELECT_EF_DIR:
      {
         /* The answer to the SELECT [MF] command is not used */

         /* Set the next state to be reached */
         pSecurityStackInstance->sReadAcl.nState = P_STATE_READ_FIRST_EF_DIR_RECORD | P_STATUS_PENDING_EXECUTED;

         /* Select the EF(DIR) file */
         static_PSecurityStackSelectFile(pContext, pSecurityStackInstance, 0x2F00);
         return;
      }

      case P_STATE_READ_FIRST_EF_DIR_RECORD:
      {
         if ((nDataLength < 2)
             || (pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            nResult = W_ERROR_SECURITY;
            goto close_logical_channel;
         }

         /* Get the number of records within the EF(DIR) file */
         nResult = static_PSecurityStackExtractEfDirRecordCount(
            pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer, nDataLength,
            &pSecurityStackInstance->sReadEfDir.nEfDirRecordCount);

         if (nResult != W_SUCCESS)
         {
            nResult = W_ERROR_SECURITY;
            goto close_logical_channel;
         }

         /* Set the next state to be reached */
         pSecurityStackInstance->sReadAcl.nState = P_STATE_READ_NEXT_EF_DIR_RECORD | P_STATUS_PENDING_EXECUTED;

         /* Read the first record */
         static_PSecurityStackReadRecord(pContext, pSecurityStackInstance,
            pSecurityStackInstance->sReadEfDir.nEfDirRecordIndex = 1);
         return;
      }

      case P_STATE_READ_NEXT_EF_DIR_RECORD:
      {
         if ((nDataLength < 2)
             || (pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            nResult = W_ERROR_SECURITY;
            goto close_logical_channel;
         }

         /* Parse the EF(DIR) record to find the PKCS#15 entry path element */
         nResult = static_PSecurityStackExtractEfDirRecordPkcs15Path(
            pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer, nDataLength,
            pSecurityStackInstance->sReadEfDir.aDfPkcs15Path,
            sizeof(pSecurityStackInstance->sReadEfDir.aDfPkcs15Path)/sizeof(pSecurityStackInstance->sReadEfDir.aDfPkcs15Path[0]),
            &pSecurityStackInstance->sReadEfDir.nDfPkcs15PathCount);

         if (nResult == W_SUCCESS)
         {
            /* Set the next state to be reached */
            pSecurityStackInstance->sReadAcl.nState = P_STATE_SELECT_DF_PKCS15 | P_STATUS_PENDING_EXECUTED;

            /* Select the first element in the path */
            static_PSecurityStackSelectFile(pContext, pSecurityStackInstance,
               pSecurityStackInstance->sReadEfDir.aDfPkcs15Path[pSecurityStackInstance->sReadEfDir.nDfPkcs15PathIndex = 0]);
            return;
         }

         /* Have all records been read so far ? */
         if ((++pSecurityStackInstance->sReadEfDir.nEfDirRecordIndex) <= pSecurityStackInstance->sReadEfDir.nEfDirRecordCount)
         {
            /* Set the next state to be reached */
            pSecurityStackInstance->sReadAcl.nState = P_STATE_READ_NEXT_EF_DIR_RECORD | P_STATUS_PENDING_EXECUTED;

            /* Read the first record */
            static_PSecurityStackReadRecord(pContext, pSecurityStackInstance,
               pSecurityStackInstance->sReadEfDir.nEfDirRecordIndex);
            return;
         }

         nResult = W_ERROR_SECURITY;
         goto close_logical_channel;
      }

      case P_STATE_SELECT_DF_PKCS15:
      {
         if ((nDataLength < 2)
             || (pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            nResult = W_ERROR_SECURITY;
            goto close_logical_channel;
         }

         /* Have all path elements been selected so far? */
         if ((++pSecurityStackInstance->sReadEfDir.nDfPkcs15PathIndex) < pSecurityStackInstance->sReadEfDir.nDfPkcs15PathCount)
         {
            /* Set the next state to be reached */
            pSecurityStackInstance->sReadAcl.nState = P_STATE_SELECT_DF_PKCS15 | P_STATUS_PENDING_EXECUTED;

            /* Select the next path element */
            static_PSecurityStackSelectFile(pContext, pSecurityStackInstance,
               pSecurityStackInstance->sReadEfDir.aDfPkcs15Path[pSecurityStackInstance->sReadEfDir.nDfPkcs15PathIndex]);
            return;
         }

         /* The DF(PKCS15) directory is now selected. Read the ACL from it */
         nResult = static_PSecurityStackReadAcl(
            pContext,
            pSecurityStackInstance,
            pSecurityStackInstance->sReadEfDir.aCardToReaderBuffer, nDataLength,
            W_FALSE);

         if (nResult != W_ERROR_OPERATION_PENDING)
         {
            goto close_logical_channel;
         }

         return;
      }

close_logical_channel:

      case P_STATE_MANAGE_CHANNEL_CLOSE:
      {
         CDebugAssert(pSecurityStackInstance->nSeLogicalChannelReference != P_7816SM_NULL_CHANNEL);

         /* Set the next state to be reached */
         pSecurityStackInstance->sReadAcl.nState = P_STATE_FINAL | P_STATUS_PENDING_EXECUTED;

         /* Send a MANAGE CHANNEL [close] command */
         static_PSecurityStackLoadAclStateMachineCloseLogicalChannel(
            pContext,
            pSecurityStackInstance);

         return;
      }

      case P_STATE_FINAL:
      {
         /* Process answer to the MANAGE CHANNEL[close] command - Errors are ignored */

         nResult = pSecurityStackInstance->sReadAcl.nError;
         goto notify_result;
      }
   }

   CDebugAssert(W_FALSE);

   nResult = W_ERROR_SECURITY;
   /* FALL THROUGH */

notify_failure:
   CDebugAssert(nResult != W_SUCCESS);
   goto notify_result;
/*
notify_success:
   CDebugAssert(nResult == W_SUCCESS);
   goto notify_result;
*/
notify_result:

   if (nResult != W_SUCCESS)
   {
      static_PSecurityStackFreeAcl(pSecurityStackInstance);
   }

   PDebugTrace("PSecurityStackLoadAcl: Notifying result %s", PUtilTraceError(nResult));

   pSecurityStackInstance->sReadAcl.nState = P_STATE_IDLE;
   PDFCPostContext2(&pSecurityStackInstance->sReadAcl.sCallbackContext, nResult);
}

/* See header file */
void PSecurityStackLoadAcl(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter )
{
   tDFCCallbackContext sCallbackContext;
   uint32_t nError = W_SUCCESS;

   PDebugTrace("PSecurityStackLoadAcl()");

   if ((pSecurityStackInstance == null))
   {
      PDebugError("PSecurityStackLoadAcl: pSecurityStackInstance == null");

      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if (pSecurityStackInstance->sReadAcl.nState != P_STATE_IDLE)
   {
      PDebugError("PSecurityStackLoadAcl: An ACL loading is already ongoing");

      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   PDFCFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter,
      &pSecurityStackInstance->sReadAcl.sCallbackContext
   );

   /* Start reading the ACL */
   PDebugTrace("PSecurityStackLoadAcl: Starting to read ACL from SE#%d",
      pSecurityStackInstance->nHalSlotIdentifier);

   static_PSecurityStackLoadAclStateMachine(pContext, pSecurityStackInstance, P_EVENT_INITIAL, 0, W_SUCCESS);
   return;

return_error:

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
   PDFCPostContext2(&sCallbackContext, nError);
}

/* See header file */
void PSecurityStackNotifySecureElementReset(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance)
{
   PDebugTrace("PSecurityStackNotifySecureElementReset()");

   if (pSecurityStackInstance == null)
   {
      PDebugError("PSecurityStackNotifySecureElementReset: pSecurityStackInstance == null");
      return;
   }

   static_PSecurityStackFreeAcl(pSecurityStackInstance);
   pSecurityStackInstance->bPkcs15IsPresent = W_FALSE;

   PDebugTrace("PSecurityStackNotifySecureElementReset: The ACL of SE#%d has been deleted", pSecurityStackInstance->nHalSlotIdentifier);
}

/* See header file */
W_ERROR PSecurityStackCheckAidAccess(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance,
            tUserInstance* pUserInstance,
            const uint8_t* pAidBuffer,
            uint32_t nAidLength,
            const uint8_t* pImpersonationDataBuffer,
            uint32_t nImpersonationDataBufferLength )
{
   PDebugTrace("PSecurityStackCheckAidAccess()");

   if (pSecurityStackInstance == null)
   {
      PDebugError("PSecurityStackCheckAidAccess: pSecurityStackInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pSecurityStackInstance->bPkcs15IsPresent == W_FALSE)
   {
#ifdef P_NO_UICC_ACCESS_BY_DEFAULT
      if (pSecurityStackInstance->bIsUicc != W_FALSE)
      {
         /* Access is forbidden on UICC if there is no PKCS#15 application */
         PDebugError("PSecurityStackCheckAidAccess: Forbidden to send APDU command to UICC (No PKCS#15 application)");
         return W_ERROR_SECURITY;
      }
#endif /* P_NO_UICC_ACCESS_BY_DEFAULT */

      PDebugTrace("PSecurityStackCheckAidAccess: There is no PKCS#15 application. Access is granted");
      return W_SUCCESS;
   }

   if ((pSecurityStackInstance->pAclProcessorInstance == null) || (pSecurityStackInstance->pAcl == null))
   {
      PDebugTrace("PSecurityStackCheckAidAccess: There is a PKCS#15 application but the ACL is malformed. Access is denied");
      return W_ERROR_SECURITY;
   }

   if ((nAidLength != P_SECSTACK_MFID_LENGTH) &&
       ((nAidLength < P_SECSTACK_AID_MIN_LENGTH) ||
        (nAidLength > P_SECSTACK_AID_MAX_LENGTH)))
   {
      PDebugError("PSecurityStackCheckAidAccess: Malformed SELECT[AID] APDU command (AID is not 2 or in range 5..16)");
      return W_ERROR_SECURITY;
   }

   /* If the caller is the default principal, access is granted */
   if (static_PSecurityStackCheckDefaultPrincipal(pContext, pUserInstance, pSecurityStackInstance,
      pImpersonationDataBuffer, nImpersonationDataBufferLength) != W_SUCCESS)
   {
      /* Check AID against ACL */
      W_ERROR nError = PSecurityStackCheckAidAccessInternal(
         pContext, pSecurityStackInstance->nHalSlotIdentifier, pUserInstance, pSecurityStackInstance->pAcl,
         pAidBuffer, nAidLength,
         pImpersonationDataBuffer, nImpersonationDataBufferLength);

      if (nError != W_SUCCESS)
      {
         PDebugError("PSecurityStackCheckAidAccess: Access rejected");
         return W_ERROR_SECURITY;
      }
   }

   return W_SUCCESS;
}

/* See header file */
void PSecurityStackNotifyStkRefresh(
         tContext* pContext,
         tSecurityStackInstance* pSecurityStackInstance,
         uint32_t nCommand,
         const uint8_t* pRefreshFileList,
         uint32_t nRefreshFileListLength)
{
   W_ERROR nResult;

   PDebugTrace("PSecurityStackNotifyStkRefresh()");

   if (pContext == null)
   {
      PDebugError("PSecurityStackNotifyStkRefresh: pContext == null");
      return;
   }

   if (pSecurityStackInstance == null)
   {
      PDebugError("PSecurityStackNotifyStkRefresh: pSecurityStackInstance == null");
      return;
   }

   if (pSecurityStackInstance->bPkcs15IsPresent == W_FALSE)
   {
      PDebugTrace("PSecurityStackNotifyStkRefresh: There is no PKCS15 application on the SE");
      return;
   }

   if (pSecurityStackInstance->nUpdateStrategy != P_SECSTACK_UPDATE_SLAVE_WITH_NOTIFICATION)
   {
      PDebugTrace("PSecurityStackNotifyStkRefresh: The update strategy is not P_SECSTACK_UPDATE_SLAVE_WITH_NOTIFICATION");
      return;
   }

   if (pSecurityStackInstance->pAclProcessorInstance == null)
   {
      PDebugTrace("PSecurityStackNotifyStkRefresh: There is no ACL Processor");
      return;
   }

   if (pSecurityStackInstance->pAclProcessorInterface->pNotifyStkRefresh == null)
   {
      PDebugTrace("PSecurityStackNotifyStkRefresh: There is no ACL Processor function to process the STK REFRESH");
      return;
   }

   nResult = pSecurityStackInstance->pAclProcessorInterface->pNotifyStkRefresh(pSecurityStackInstance->pAclProcessorInstance,
      nCommand, pRefreshFileList, nRefreshFileListLength);

   if (nResult != W_SUCCESS)
   {
      PDebugError("PSecurityStackNotifyStkRefresh: ACL Processor pNotifyStkRefresh failed with %s", PUtilTraceError(nResult));
      return;
   }
}

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */
