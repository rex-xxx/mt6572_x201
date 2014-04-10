/*
 * Copyright (c) 2011-2012 Inside Secure, All Rights Reserved.
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
#define P_MODULE  P_MODULE_DEC( SE_HAL )

#include "wme_context.h"

/* Mask for the channelId number */
#define P_SEHAL_CHANNELID_MASK 0x01F

/* Mask for the bit indicating that SELECT[application] is expected on this logcial channel */
#define P_SEHAL_CHANNELID_WAIT_FOR_SELECT 0x80

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * Finds the index of a slot.
 **/
static uint32_t static_PSeHalFindSlot(
         tPSeHalInstance* pSeHalInstance,
         uint32_t nHalSlotIdentifier)
{
   uint32_t nIndex;

   for(nIndex = 0; nIndex < P_SE_HAL_MAXIMUM_SE_NUMBER; nIndex++)
   {
      if(pSeHalInstance->aSlotArray[nIndex].nHalSlotIdentifier == nHalSlotIdentifier)
      {
         return nIndex;
      }
   }

   return P_SE_HAL_MAXIMUM_SE_NUMBER;
}

#ifdef P_INCLUDE_SE_SECURITY
/* See tCSeCallback */
static void static_PSeHalCallback(
         void* pCallbackParameter,
         uint32_t nHalSlotIdentifier,
         uint32_t nOperation,
         bool_t bSuccess,
         uint32_t nParam1,
         uint32_t nParam2)
{
   tContext* pContext = (tContext*)pCallbackParameter;
   tPSeHalInstance* pSeHalInstance;
   uint32_t nIndex;
   tDFCCallbackContext sCallbackContext;

   /* Lock the context to prevent re-entrancy issues */
   PContextLock(pContext);

   PDebugTrace("static_PSeHalCallback(%d, code=%d)", nHalSlotIdentifier, nOperation);

   pSeHalInstance = PContextGetSeHalInstance(pContext);

   if(pSeHalInstance->bIsInitialized == W_FALSE)
   {
      PDebugError("static_PSeHalCallback(): bad state, ignoring the event");
      goto return_function;
   }

   nIndex = static_PSeHalFindSlot(pSeHalInstance, nHalSlotIdentifier);

   if(nIndex == P_SE_HAL_MAXIMUM_SE_NUMBER)
   {
      PDebugError("static_PSeHalCallback(): wrong slot identifier, ignoring the event");
      goto return_function;
   }

   switch(nOperation)
   {
      case C_SE_OPERATION_GET_INFO:
         if(pSeHalInstance->aSlotArray[nIndex].pGetInfoCallback == null)
         {
            PDebugError("static_PSeHalCallback(): no get info pending");
            goto return_function;
         }

         if(pSeHalInstance->aSlotArray[nIndex].nGetInfoAtrBufferLength < nParam2)
         {
            PDebugError("static_PSeHalCallback(): ATR too long for th buffer");
            goto return_function;
         }

         PDFCFillCallbackContext(
            pContext,
            (tDFCCallback*)pSeHalInstance->aSlotArray[nIndex].pGetInfoCallback,
            pSeHalInstance->aSlotArray[nIndex].pGetInfoCallbackParameter,
            &sCallbackContext );

         pSeHalInstance->aSlotArray[nIndex].pGetInfoCallback = null;

         PDFCPostContext5(
            &sCallbackContext,
            pSeHalInstance->aSlotArray[nIndex].nHalSlotIdentifier,
            nParam1, nParam2,
            (bSuccess != W_FALSE)?W_SUCCESS:W_ERROR_TIMEOUT );
         break;
      case C_SE_OPERATION_OPEN:
      case C_SE_OPERATION_EXCHANGE:
      case C_SE_OPERATION_CLOSE:
         PSeHalSmNotifyOperationCompletion(pContext, pSeHalInstance->aSlotArray[nIndex].nHalSlotIdentifier, nOperation, bSuccess, nParam1, nParam2);
         break;
      case C_SE_NOTIFY_HOT_PLUG:
         PSEDriverNotifyHotPlug(pContext, nHalSlotIdentifier, bSuccess);
         break;
      case C_SE_NOTIFY_STK_ACTIVATE_SWP:
         PSEDriverNotifyStkActivateSwp(pContext, nHalSlotIdentifier);
         break;
      case C_SE_NOTIFY_STK_REFRESH:
         if((nParam1 != P_SE_STK_REFRESH_CODE_FILE_CHANGE_NOTIFICATION)
         && (nParam1 != P_SE_STK_REFRESH_CODE_UICC_RESET))
         {
            PDebugError("static_PSeHalCallback(): wrong code for the STK REFRESH command");
            goto return_function;
         }
         if(nParam2 > sizeof(pSeHalInstance->aRefreshFileList))
         {
            PDebugError("static_PSeHalCallback(): file list length too long for the STK REFRESH command");
            goto return_function;
         }
         PSEDriverNotifyStkRefresh(pContext, nHalSlotIdentifier, nParam1, pSeHalInstance->aRefreshFileList, nParam2);
         break;
      default:
         PDebugError("static_PSeHalCallback(): wrong operation code");
         goto return_function;
   }

return_function:

   /* Release the lock */
   PContextReleaseLock(pContext);
}
#endif /* P_INCLUDE_SE_SECURITY */

/* See header file */
const char* PSeHalTraceIdentifier(
         uint32_t nHalSlotIdentifier)
{
   switch(nHalSlotIdentifier)
   {
      case C_SE_SLOT_ID_STANDALONE_1:
         return "STD_1";
      case C_SE_SLOT_ID_STANDALONE_2:
         return "STD_2";
      case C_SE_SLOT_ID_STANDALONE_3:
         return "STD_3";
      case C_SE_SLOT_ID_STANDALONE_4:
         return "STD_4";
      case C_SE_SLOT_ID_SWP_1:
         return "SWP_1";
      case C_SE_SLOT_ID_SWP_2:
         return "SWP_2";
      case C_SE_SLOT_ID_SWP_3:
         return "SWP_3";
      case C_SE_SLOT_ID_SWP_4:
         return "SWP_4";
      case C_SE_SLOT_ID_PROPRIETARY_1:
         return "PROP_1";
      case C_SE_SLOT_ID_PROPRIETARY_2:
         return "PROP_2";
      case C_SE_SLOT_ID_PROPRIETARY_3:
         return "PROP_3";
      case C_SE_SLOT_ID_PROPRIETARY_4:
         return "PROP_4";
      default:
         return "UNKNOWN";
   }
}

/* See header file */
bool_t PSeHalIsProprietarySlot(
         uint32_t nHalSlotIdentifier)
{
   if((nHalSlotIdentifier >= C_SE_SLOT_ID_PROPRIETARY_1)
   && (nHalSlotIdentifier < C_SE_SLOT_ID_PROPRIETARY_1 + P_SE_HAL_MAXIMUM_NUMBER_PROPRIETARY_SE))
   {
      return W_TRUE;
   }

   return W_FALSE;
}

/* See header file */
bool_t PSeHalIsSwpSlot(
         uint32_t nHalSlotIdentifier)
{
   if((nHalSlotIdentifier >= C_SE_SLOT_ID_SWP_1)
   && (nHalSlotIdentifier < C_SE_SLOT_ID_SWP_1 + P_SE_HAL_MAXIMUM_NUMBER_SWP_SE))
   {
      return W_TRUE;
   }

   return W_FALSE;
}

/* See header file */
bool_t PSeHalIsStandaloneSlot(
         uint32_t nHalSlotIdentifier)
{
   if((nHalSlotIdentifier >= C_SE_SLOT_ID_STANDALONE_1)
   && (nHalSlotIdentifier < C_SE_SLOT_ID_STANDALONE_1 + P_SE_HAL_MAXIMUM_NUMBER_STANDALONE_SE))
   {
      return W_TRUE;
   }

   return W_FALSE;
}

/* See header file */
void PSeHalCreate(
         tContext* pContext,
         tPSeHalInstance* pSeHalInstance )
{
   PDebugTrace("PSeHalCreate()");

   if(pSeHalInstance != null)
   {
      uint32_t nIndex = 0;
      uint32_t nHalSlotIdentifier;

      CMemoryFill(pSeHalInstance, 0, sizeof(tPSeHalInstance));

#ifdef P_INCLUDE_SE_SECURITY
      pSeHalInstance->pSePorting = CSeCreate(static_PSeHalCallback, pContext, pSeHalInstance->aRefreshFileList, sizeof(pSeHalInstance->aRefreshFileList));
#endif /* #ifdef P_INCLUDE_SE_SECURITY */
      pSeHalInstance->bIsInitialized = W_TRUE;

      for(nHalSlotIdentifier = C_SE_SLOT_ID_STANDALONE_1;
      nHalSlotIdentifier < C_SE_SLOT_ID_STANDALONE_1 + P_SE_HAL_MAXIMUM_NUMBER_STANDALONE_SE;
      nHalSlotIdentifier++)
      {
         CDebugAssert(nIndex < P_SE_HAL_MAXIMUM_SE_NUMBER);
         pSeHalInstance->aSlotArray[nIndex].nSlotIdentifier = (uint32_t)-1;
         pSeHalInstance->aSlotArray[nIndex++].nHalSlotIdentifier = nHalSlotIdentifier;
      }

      for(nHalSlotIdentifier = C_SE_SLOT_ID_PROPRIETARY_1;
      nHalSlotIdentifier < C_SE_SLOT_ID_PROPRIETARY_1 + P_SE_HAL_MAXIMUM_NUMBER_PROPRIETARY_SE;
      nHalSlotIdentifier++)
      {
         CDebugAssert(nIndex < P_SE_HAL_MAXIMUM_SE_NUMBER);
         pSeHalInstance->aSlotArray[nIndex].nSlotIdentifier = (uint32_t)-1;
         pSeHalInstance->aSlotArray[nIndex++].nHalSlotIdentifier = nHalSlotIdentifier;
      }

      for(nHalSlotIdentifier = C_SE_SLOT_ID_SWP_1;
      nHalSlotIdentifier < C_SE_SLOT_ID_SWP_1 + P_SE_HAL_MAXIMUM_NUMBER_SWP_SE;
      nHalSlotIdentifier++)
      {
         CDebugAssert(nIndex < P_SE_HAL_MAXIMUM_SE_NUMBER);
         pSeHalInstance->aSlotArray[nIndex].nSlotIdentifier = (uint32_t)-1;
         pSeHalInstance->aSlotArray[nIndex++].nHalSlotIdentifier = nHalSlotIdentifier;
      }
   }
}

/* See header file */
void PSeHalDestroy(
         tPSeHalInstance* pSeHalInstance )
{
   PDebugTrace("PSeHalDestroy()");

   if(pSeHalInstance != null)
   {
#ifdef P_INCLUDE_SE_SECURITY
      CSeDestroy(pSeHalInstance->pSePorting);

      pSeHalInstance->pSePorting = null;
#endif /* #ifdef P_INCLUDE_SE_SECURITY */

      pSeHalInstance->bIsInitialized = W_FALSE;
   }
}

/* See CSeGetStaticInfo() */
bool_t PSeHalGetStaticInfo(
         tContext* pContext,
         uint32_t nSlotIdentifier,
         uint32_t nHalSlotIdentifier,
         uint32_t* pnCapabilities,
         uint32_t* pnSwpTimeout,
         uint8_t* pNameBuffer,
         uint32_t nNameBufferLength )
{
   tPSeHalInstance* pSeHalInstance = PContextGetSeHalInstance(pContext);
   uint32_t nCapabilities = 0;
   uint32_t nHalFlags = 0;
   uint32_t nActualNameLength = 0;
   bool_t bResult = W_FALSE;
   uint32_t nIndex;

   PDebugTrace("PSeHalGetStaticInfo SE#%d (hal-id=%s)",
               nSlotIdentifier, PSeHalTraceIdentifier(nHalSlotIdentifier));

   CDebugAssert(static_PSeHalFindSlot(pSeHalInstance, nHalSlotIdentifier) != P_SE_HAL_MAXIMUM_SE_NUMBER);

   nIndex = static_PSeHalFindSlot(pSeHalInstance, nHalSlotIdentifier);

   if(nIndex == P_SE_HAL_MAXIMUM_SE_NUMBER)
   {
      PDebugError("PSeHalGetStaticInfo(): wrong hal slot identifier");
      return W_FALSE;
   }
   CDebugAssert( ((pSeHalInstance->aSlotArray[nIndex].nSlotIdentifier == (uint32_t )-1) || \
                 (pSeHalInstance->aSlotArray[nIndex].nSlotIdentifier == nSlotIdentifier)));
   pSeHalInstance->aSlotArray[nIndex].nSlotIdentifier = nSlotIdentifier;

   if(pSeHalInstance->bIsInitialized == W_FALSE)
   {
      PDebugError("PSeHalGetStaticInfo(): bad state");
      return W_FALSE;
   }

#ifdef P_INCLUDE_SE_SECURITY
   if (pSeHalInstance->pSePorting != null)
   {
      bResult = CSeGetStaticInfo(
            pSeHalInstance->pSePorting,
            nHalSlotIdentifier,
            &nHalFlags,
            pnSwpTimeout,
            pNameBuffer,
            nNameBufferLength - 1,
            &nActualNameLength );

      if(bResult != W_FALSE)
      {
         pNameBuffer[nActualNameLength] = 0;
         PDebugTrace("PSeHalGetStaticInfo: SE#%d (hal-id=%s) flags=%02X name=%s", nSlotIdentifier, PSeHalTraceIdentifier(nHalSlotIdentifier), nHalFlags, pNameBuffer);
         nCapabilities = W_SE_FLAG_SE_HAL;
      }
      else
      {
         PDebugTrace("PSeHalGetStaticInfo: Error returned by PSeHalGetStaticInfo() for SE#%d (hal-id=%s), this error may be ok",
            nSlotIdentifier, PSeHalTraceIdentifier(nHalSlotIdentifier));
      }
   }
   else
   {
      PDebugTrace("PSeHalGetStaticInfo: pSePorting == null");
      bResult = W_FALSE;
   }

#endif /* #ifdef P_INCLUDE_SE_SECURITY */

   if(bResult == W_FALSE)
   {
      /* No slot supported by the HAL, or no HAL implementation */
      nHalFlags = 0;
      nActualNameLength = 0;
      nCapabilities = 0;
      *pnSwpTimeout = 0;

      /* Implement the default behavior: If information is requested on Proprietary slots or SWP slots, the slots shall exist  */

      if(PSeHalIsProprietarySlot(nHalSlotIdentifier) != W_FALSE)
      {
          CDebugAssert(nNameBufferLength > (5 + 1));
          nActualNameLength = 5;
          pNameBuffer[0] = 'S';
          pNameBuffer[1] = 'E';
          pNameBuffer[2] = ' ';
          pNameBuffer[3] = '#';
          pNameBuffer[4] = (uint8_t)('0' + nHalSlotIdentifier - C_SE_SLOT_ID_PROPRIETARY_1);

          bResult = W_TRUE;
      }
      else if(PSeHalIsSwpSlot(nHalSlotIdentifier) != W_FALSE)
      {
          CDebugAssert(nNameBufferLength > (7 + 1));
          nActualNameLength = 7;
          pNameBuffer[0] = 'U';
          pNameBuffer[1] = 'I';
          pNameBuffer[2] = 'C';
          pNameBuffer[3] = 'C';
          pNameBuffer[4] = ' ';
          pNameBuffer[5] = '#';
          pNameBuffer[6] = (uint8_t)('0' + nHalSlotIdentifier - C_SE_SLOT_ID_SWP_1);

          nHalFlags = C_SE_FLAG_UICC;
          *pnSwpTimeout = P_UICC_SWP_BOOT_TIMEOUT;

          bResult = W_TRUE;
      }
   }

   pNameBuffer[nActualNameLength] = 0;

   if(PSeHalIsSwpSlot(nHalSlotIdentifier) != W_FALSE)
   {
      nCapabilities |= W_SE_FLAG_SWP;
   }

   if((nHalFlags & C_SE_FLAG_REMOVABLE) != 0)
   {
      nCapabilities |= W_SE_FLAG_REMOVABLE;
   }
   if((nHalFlags & C_SE_FLAG_HOT_PLUG) != 0)
   {
      if((nHalFlags & C_SE_FLAG_REMOVABLE) == 0)
      {
         PDebugError("PSeHalGetStaticInfo(): Cannot send hot plug events from a non-removable slot");
         bResult = W_FALSE;
      }
      nCapabilities |= W_SE_FLAG_HOT_PLUG;
   }
   if((nHalFlags & C_SE_FLAG_UICC) != 0)
   {
      nCapabilities |= W_SE_FLAG_UICC;
   }
   if((nHalFlags & C_SE_FLAG_COMM_SUPPORT) != 0)
   {
      nCapabilities |= W_SE_FLAG_COMMUNICATION;
   }
   if((nHalFlags & C_SE_FLAG_COMM_SWP_SUPPORT) != 0)
   {
      if(PSeHalIsSwpSlot(nHalSlotIdentifier) == W_FALSE)
      {
         PDebugError("PSeHalGetStaticInfo(): Cannot communicate on SWP on a non-SWP slot");
         bResult = W_FALSE;
      }

      nCapabilities |= W_SE_FLAG_COMMUNICATION_VIA_SWP;
   }
   if((nHalFlags & C_SE_FLAG_STK_REFRESH_SUPPORT) != 0)
   {
      if((nHalFlags & C_SE_FLAG_UICC) == 0)
      {
         PDebugError("PSeHalGetStaticInfo(): STK RFFRESH is only supported for UICC");
         bResult = W_FALSE;
      }
      nCapabilities |= W_SE_FLAG_STK_REFRESH_SUPPORT;
   }

   *pnCapabilities = nCapabilities;

   return bResult;
}

#ifdef P_INCLUDE_SE_SECURITY
/* See CSeGetInfo() */
void PSeHalGetInfo(
         tContext* pContext,
         tPSeHalGetInfoCallbackFunction* pCallback,
         void* pCallbackParameter,
         uint32_t nHalSlotIdentifier,
         uint8_t* pAtrBuffer,
         uint32_t nAtrBufferLength )
{
   tPSeHalInstance* pSeHalInstance = PContextGetSeHalInstance(pContext);
   uint32_t nIndex = static_PSeHalFindSlot(pSeHalInstance, nHalSlotIdentifier);

   PDebugTrace("PSeHalGetInfo(%d)", nHalSlotIdentifier);

   if(pSeHalInstance->bIsInitialized == W_FALSE)
   {
      PDebugError("PSeHalGetInfo(): bad state");
      return;
   }

   if(nIndex == P_SE_HAL_MAXIMUM_SE_NUMBER)
   {
      PDebugError("PSeHalGetInfo(): bad identifier");
      return;
   }

   if(pSeHalInstance->aSlotArray[nIndex].pGetInfoCallback != null)
   {
      PDebugError("PSeHalGetInfo(): already pending");
      return;
   }

   pSeHalInstance->aSlotArray[nIndex].pGetInfoCallback = pCallback;
   pSeHalInstance->aSlotArray[nIndex].pGetInfoCallbackParameter = pCallbackParameter;
   pSeHalInstance->aSlotArray[nIndex].nGetInfoAtrBufferLength = nAtrBufferLength;

   CSeGetInfo(
         pSeHalInstance->pSePorting,
         nHalSlotIdentifier,
         pAtrBuffer,
         nAtrBufferLength );
}

/* See CSeTriggerStkPolling() */
void PSeHalTriggerStkPolling(
         tContext* pContext,
         uint32_t nHalSlotIdentifier)
{
   tPSeHalInstance* pSeHalInstance = PContextGetSeHalInstance(pContext);

   PDebugTrace("PSeHalTriggerStkPolling(%d)", nHalSlotIdentifier);

   if(pSeHalInstance->bIsInitialized == W_FALSE)
   {
      PDebugError("PSeHalTriggerStkPolling(): bad state");
      return;
   }

   if(static_PSeHalFindSlot(pSeHalInstance, nHalSlotIdentifier) == P_SE_HAL_MAXIMUM_SE_NUMBER)
   {
      PDebugError("PSeHalTriggerStkPolling(): bad identifier");
      return;
   }

   CSeTriggerStkPolling(
         pSeHalInstance->pSePorting,
         nHalSlotIdentifier );
}

#endif /* #ifdef P_INCLUDE_SE_SECURITY */

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
