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

#define P_MODULE  P_MODULE_DEC( NAL_BIND )

#include "nfc_hal_binding.h"

/******************************************************************************************************************
*                                         Definition
*******************************************************************************************************************/

#define P_NFC_HAL_BINDING_WAKE_UP_SERVICE_REGISTER             0x01
#define P_NFC_HAL_BINDING_WAKE_UP_START_FROM_WAKEUP            0x02
#define P_NFC_HAL_BINDING_WAKE_UP_RESTORE_POLICY               0x02
#define P_NFC_HAL_BINDING_WAKE_UP_RESTORE_CARD_DETECT          0x03
#define P_NFC_HAL_BINDING_WAKE_UP_RESTORE_ACTIVATED_READERS    0x04
#define P_NFC_HAL_BINDING_WAKE_UP_COMPLETED                    0x05

#define P_NFC_HAL_BINDING_STANDBY_RF_WAKE                      0x01

/******************************************************************************************************************
*                                        Functions declaration
*******************************************************************************************************************/

/**
 *  Function called when the HCI2_CMD_MGT_STANDBY has been sent.
 *
 */
static void static_PNALBindingExecuteCmdStandbyCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nReceptionCounter)
{
   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if ((nError == W_SUCCESS) && (nStatusCode == 0))
   {
      /* Standby command succeeded, switch to low power state */
      pBindingContext->nHCIMode = W_NFCC_MODE_STANDBY;

      /* After entering standby mode, check if there's not any NFC HAL command queued during the
         SWITCHING_TO_LOW_POWER_STATE. If any, wake up the MR */

      if (pBindingContext->pNALWriteCallbackFunction != null)
      {
         PNALBindingWakeUp(pBindingContext, null);
      }
   }
   else
   {
     PNALDebugError("static_PNALBindingExecuteCmdStandbyCompleted : nError %d - nStatusCode %d", nError, nStatusCode);

      /* The standby command failed, consider we are still in active mode
         (there's no need to perform wake up) - call wake up machine to perform post wake up operations */

      pBindingContext->nNextWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_COMPLETED;
      PNALBindingWakeUpMachine(pBindingContext);
   }
}

static void static_PNALBindingStoreStandbyParameterCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   uint8_t* pHCISendBuffer = pBindingContext->aNALBindingNALService[NAL_SERVICE_ADMIN].aNALOperationBuffer;

   if ((nError == W_SUCCESS) && (nStatusCode == ETSI_ERR_ANY_OK))
   {
      /* Send HCI2_CMD_MGT_STANDBY to enter low power mode */
      pHCISendBuffer[0] = P_NFC_HAL_BINDING_STANDBY_RF_WAKE;

      PHCIServiceExecuteCommand(
            pBindingContext,
            P_HCI_SERVICE_PIPE_MANAGEMENT,
            PNALBindingGetOperation(pBindingContext),
            HCI2_CMD_MGT_STANDBY,
            pHCISendBuffer,
            0x01,
            pBindingContext->aHCIResponseBuffer,
            sizeof(pBindingContext->aHCIResponseBuffer),
            static_PNALBindingExecuteCmdStandbyCompleted,
            null,
            W_TRUE );
   }
   else
   {
      PNALDebugError("static_PNALBindingStoreStandbyParameterCompleted : unable to store the standby parameters");
      pBindingContext->nHCIMode = W_NFCC_MODE_ACTIVE;
   }
}

/**
 * Initiates SWITCH_TO_STANDBY transition
 */

static void static_PNALBindingEnterLowPowerMode(
   tNALBindingContext * pBindingContext)
{
   uint8_t* pHCISendBuffer = pBindingContext->aNALBindingNALService[NAL_SERVICE_ADMIN].aNALOperationBuffer;
   uint32_t nHCISendBufferLength = 0;

   bool_t bIsCardLockedCurrent;
   uint16_t nUICCCardPolicyCurrent;
   uint32_t nSESwitchPositionCurrent;
   uint32_t nSESlotIdentifierCurrent;
   uint16_t nSECardPolicyCurrent;
   uint16_t nHCIPolicy;
   uint8_t  nHCIMode;
   uint8_t nResult;

   /* Enter the switching to low power state */
   pBindingContext->nHCIMode = W_NFCC_MODE_SWITCH_TO_STANDBY;

   nResult = PNALProtocolParse_NAL_PAR_POLICY(pBindingContext->aParam_NAL_PAR_POLICY, NAL_POLICY_SIZE,
                                                null, &bIsCardLockedCurrent, &nUICCCardPolicyCurrent, null,
                                                &nSESwitchPositionCurrent, &nSESlotIdentifierCurrent, &nSECardPolicyCurrent, null);

   if (nResult == NAL_RES_OK)
   {
      pHCISendBuffer[nHCISendBufferLength++] = P_HCI_SERVICE_INSTANCE;
      pHCISendBuffer[nHCISendBufferLength++] = 4;
      pHCISendBuffer[nHCISendBufferLength++] = HCI2_PAR_INSTANCES_STANDBY_MCARD_GRANTED_TO_SIM;
      pHCISendBuffer[nHCISendBufferLength++] = 2;

      static_PNALBindingGetHCICardProtocolCapabilities(&nHCIPolicy, nUICCCardPolicyCurrent);
      static_PNALBindingWriteUint16ToHCIBuffer(nHCIPolicy, &pHCISendBuffer[nHCISendBufferLength]);
      nHCISendBufferLength+=2;

      pHCISendBuffer[nHCISendBufferLength++] = P_HCI_SERVICE_PIPE_MANAGEMENT;
      pHCISendBuffer[nHCISendBufferLength++] = 3;
      pHCISendBuffer[nHCISendBufferLength++] = HCI2_PAR_MGT_STANDBY_RF_LOCK_CARD;
      pHCISendBuffer[nHCISendBufferLength++] = 1;
      pHCISendBuffer[nHCISendBufferLength++] = bIsCardLockedCurrent ? HCI2_VAL_LOCKED : HCI2_VAL_UNLOCKED;

      pHCISendBuffer[nHCISendBufferLength++] = P_HCI_SERVICE_SE;
      pHCISendBuffer[nHCISendBufferLength++] = 4;
      pHCISendBuffer[nHCISendBufferLength++] = HCI2_PAR_SE_SETTINGS_STANDBY;
      pHCISendBuffer[nHCISendBufferLength++] = 2;

      static_PNALBindingGetHCICardProtocolCapabilities(&nHCIPolicy, nSECardPolicyCurrent);

      switch (nSESwitchPositionCurrent)
      {
         default:     /* can not occur, just to avoid warning */
         case NAL_POLICY_FLAG_SE_OFF: /* OFF */
            nHCIMode = 0x80;
            nHCIPolicy = 0; /* Force the filter to 0 */
            break;

         case NAL_POLICY_FLAG_RF_INTERFACE: /* RF Interface */
            nHCIMode = 0x10;
            break;

         case NAL_POLICY_FLAG_FORCED_HOST_INTERFACE: /* Forced Host Interface */
            nHCIMode = 0x03;
            break;

         case NAL_POLICY_FLAG_HOST_INTERFACE: /* Host Interface */
            nHCIMode = 0x01;
            break;
      }

      pHCISendBuffer[nHCISendBufferLength++] = nHCIMode;
      pHCISendBuffer[nHCISendBufferLength++] = (uint8_t) nHCIPolicy;

      PHCIServiceExecuteMultiServiceCommand(
         pBindingContext,
         PNALBindingGetOperation(pBindingContext),
         P_HCI_SERVICE_CMD_SET_PROPERTIES,
         pHCISendBuffer,
         nHCISendBufferLength,
         pBindingContext->aHCIResponseBuffer,
         sizeof(pBindingContext->aHCIResponseBuffer),
         static_PNALBindingStoreStandbyParameterCompleted,
         null,
         W_TRUE);
   }
   else
   {
      PNALDebugError("static_PNALBindingEnterLowPowerMode : unable to build standby parameters");
      pBindingContext->nHCIMode = W_NFCC_MODE_ACTIVE;
   }
}

/**
  * HCI inactivity event processing
  *
  * Called when HCI2_EVT_MGT_HCI_INACTIVITY is received
  **/

static void static_PNALBindingInactivityTimeout(
   tNALBindingContext * pBindingContext,
   void * pCallbackParameter)
{
   bool_t bIsReaderLocked;

   PNALDebugTrace("static_PNALBindingInactivityTimeout : timer elapsed");

   /* The standby mode is not supported in raw mode */

   if (pBindingContext->bRawMode != W_FALSE)
   {
      PNALDebugTrace("static_PNALBindingInactivityTimeout : in RAW mode : ignored");
      return;
   }

   /* The standby mode will be entered only when we are in W_NFCC_MODE_ACTIVE */

   if ((pBindingContext->nHCIMode != W_NFCC_MODE_ACTIVE) || (pBindingContext->nMode != W_NFCC_MODE_ACTIVE))
   {
      PNALDebugTrace("static_PNALBindingInactivityTimeout : not in ACTIVE state : ignored");
      return;
   }

   /* The standby mode will be entered only if

      - no reader mode activated or reader RF is locked
      - user expressly requested to enter low power mode
   */

   PNALProtocolParse_NAL_PAR_POLICY(pBindingContext->aParam_NAL_PAR_POLICY, sizeof(pBindingContext->aParam_NAL_PAR_POLICY),
      &bIsReaderLocked, null, null, null, null, null, null, null);

   if ( (pBindingContext->bIsLowPowerRequested == W_FALSE) &&
        (pBindingContext->nLastNALReaderProtocol != 0x0000) &&
        (bIsReaderLocked == W_FALSE) )

   {
      PNALDebugTrace("PNALBindingInactivityTimeout : user did not requested low power mode, and at least one reader mode is activated and the reader RF is not locked : ignored");
      return;
   }

   /* All conditions are met to enter low power mode */
   PNALDebugTrace("PNALBindingInactivityTimeout : entering low power mode...");

   static_PNALBindingEnterLowPowerMode(pBindingContext);
}

/**
*  Function called when an event has been received on service CHIP management
*
* @See definition of tPHCIServiceEventDataReceived
*/

NFC_HAL_INTERNAL void PNALBindingChipManagementEventDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t neventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter)
{
   PNALDebugTrace("PNALBindingChipManagementEventDataReceived");

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   switch (neventIdentifier)
   {
      case HCI2_EVT_MGT_HCI_INACTIVITY :

         /* A long period of inactivity occured at HCI level,
            we are ready to enter standby mode */

         static_PNALBindingInactivityTimeout(pBindingContext, null);
         break;

      case HCI2_EVT_MGT_HCI_ACTIVITY :

         /* HCI activity needs to be restored */
         PNALBindingWakeUp(pBindingContext, pCallbackParameter);
         break;

      case HCI2_EVT_MGT_HCI_WAKE_UP :

         /* The HCI link has been restarted */

         if (pBindingContext->nHCIMode == W_NFCC_MODE_STANDBY)
         {
            /* The MR has woke up due to field presence,
               return to mode active */

            pBindingContext->nHCIMode = W_NFCC_MODE_SWITCH_TO_ACTIVE;

            pBindingContext->nNextWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_START_FROM_WAKEUP;
            PNALBindingWakeUpMachine( pBindingContext);
         }
         else
         {
            if ((pBindingContext->bIsFirmwareUpdateStarted == W_FALSE) && (pBindingContext->nHCIMode != W_NFCC_MODE_BOOT_PENDING))
            {
               /* Spontaneous reset ? */
               PNALDebugError("PNALBindingChipManagementEventDataReceived : spontaneous reset of the chip");

               pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;
               pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_NFCC_ERROR;

               static_PNALBindingWriteUint32ToNALBuffer(NAL_EVT_NFCC_ERROR_SPONTANEOUS_RESET, & pBindingContext->pNALReceptionBuffer[2]);

               PNALBindingCallReadCallback(pBindingContext, 6, 0);
            }
         }

         /* other cases are ignored */

         break;

      default:
         PNALDebugError("PNALBindingChipManagementEventDataReceived:  neventIdentifier(0x%x) error: ", neventIdentifier);
         return;
   }
}

/**
*  Function called to initiate MR wakeup
*/

NFC_HAL_INTERNAL void PNALBindingWakeUp(
   tNALBindingContext *pBindingContext,
   void * pCallbackParameter)
{
   PNALDebugTrace("PNALBindingWakeUp");

   /* Enter the switching to active state */
   pBindingContext->nHCIMode = W_NFCC_MODE_SWITCH_TO_ACTIVE;

   pBindingContext->bIsResetPending = W_TRUE;
   pBindingContext->nResetType = NAL_SIGNAL_WAKEUP;

   PHCIServicePreReset(pBindingContext);
   CNALResetNFCController(pBindingContext->pPortingConfig, NAL_SIGNAL_WAKEUP);
}

static void static_PNALBindingWakeUpSetParameterCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            W_ERROR nError,
            uint8_t nStatusCode,
            uint32_t nReceptionCounter)
{
   PNALDebugTrace("static_PNALBindingWakeUpSetParameterCompleted nError %d", nError);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if (nError != W_SUCCESS)
   {
      PNALDebugError("static_PNALBindingWakeUpSetParameterCompleted : nError %d", nError);
   }

   PNALBindingWakeUpMachine(pBindingContext);
}

static void static_PNALBindingWakeUpDetectionReaderSendEventCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            W_ERROR nError,
            uint32_t nHCIMessageReceptionCounter )
{
   PNALDebugTrace("static_PNALBindingWakeUpDetectionReaderSendEventCompleted nError %d", nError);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   if (nError != W_SUCCESS)
   {
      PNALDebugTrace("static_PNALBindingWakeUpDetectionReaderSendEventCompleted nError %d", nError);
   }

   PNALBindingWakeUpMachine(pBindingContext);
}

/**
 *  Wake up state machine
 */

NFC_HAL_INTERNAL void PNALBindingWakeUpMachine(
                              tNALBindingContext *pBindingContext)
{
   uint8_t nProtocolIndex;

   pBindingContext->nCurrentWakeUpState = pBindingContext->nNextWakeUpState;

   PNALDebugTrace("PNALBindingWakeUpMachine : state %d", pBindingContext->nCurrentWakeUpState);

   switch (pBindingContext->nCurrentWakeUpState)
   {
      /* After a complete RESET, we have to restore service registrations (
         since they have been lost due to PHCIServiceConnect() operation */

      case P_NFC_HAL_BINDING_WAKE_UP_SERVICE_REGISTER :

         /* register for chip management events */
         PHCIServiceRegister(
            pBindingContext,
            P_HCI_SERVICE_PIPE_MANAGEMENT,
            P_EVT_REGISTER_TYPE,
            W_FALSE,
            PNALBindingGetOperation(pBindingContext),
            PNALBindingChipManagementEventDataReceived,
            PNALUtilConvertUint32ToPointer(NAL_SERVICE_ADMIN),
            W_TRUE );

         /* event register for generic reader service */
         PHCIServiceRegister(
            pBindingContext,
            P_HCI_SERVICE_MREAD,
            P_EVT_REGISTER_TYPE,
            W_FALSE,
            PNALBindingGetOperation(pBindingContext),
            PNALBindingReaderEventDataReceived,
            PNALUtilConvertUint32ToPointer(P_HCI_SERVICE_MREAD),
            W_TRUE );

         /* event register for generic card service */
         PHCIServiceRegister(
            pBindingContext,
            P_HCI_SERVICE_MCARD,
            P_EVT_REGISTER_TYPE,
            W_FALSE,
            PNALBindingGetOperation(pBindingContext),
            PNALBindingCardEventDataReceived,
            PNALUtilConvertUint32ToPointer(P_HCI_SERVICE_MCARD),
            W_TRUE );

         /* event register for card mode */
         for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
           {
              if(((g_aNALBindingCardProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT) != 0)&&
                 ((g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCIAvailableCardProtocol) != 0))
              {
                  PHCIServiceRegister(
                        pBindingContext,
                        g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                        P_EVT_REGISTER_TYPE,
                        W_FALSE,
                        PNALBindingGetOperation(pBindingContext),
                        PNALBindingCardEventDataReceived,
                        PNALUtilConvertUint32ToPointer(g_aNALBindingCardProtocolArray[nProtocolIndex].nNALServiceIdentifier),
                        W_TRUE );
             }
          }
          for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
          {
             if((g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCIAvailableReaderProtocol) != 0)
             {
                /* event register for Reader Mode */
                if((g_aNALBindingReaderProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT) != 0)
                {
                   PHCIServiceRegister(
                                pBindingContext,
                                g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                                P_EVT_REGISTER_TYPE,
                                W_FALSE,
                                PNALBindingGetOperation(pBindingContext),
                                PNALBindingReaderEventDataReceived,
                                PNALUtilConvertUint32ToPointer(g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier),
                                W_TRUE );

                   /* the reader start_discovery cmd is sent to one of NFCC opened gate reader */
                  pBindingContext->nOneOfOpenedReaderServiceId = g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier;
                }
            }
          }

          /* register for Connectivity Message events */
          PHCIServiceRegister(
                          pBindingContext,
                          P_HCI_SERVICE_UICC_CONNECTIVITY,
                          P_EVT_REGISTER_TYPE,
                          W_FALSE,
                          PNALBindingGetOperation(pBindingContext),
                          PNALBindingUICCEventDataReceived,
                          PNALUtilConvertUint32ToPointer(NAL_SERVICE_UICC),
                          W_TRUE );

         /* register for SE events */

         PHCIServiceRegister(
               pBindingContext,
               P_HCI_SERVICE_SE,
               P_EVT_REGISTER_TYPE,
               W_FALSE,
               PNALBindingGetOperation(pBindingContext),
               PNALBindingSEEventDataReceived,
               PNALUtilConvertUint32ToPointer(NAL_SERVICE_SECURE_ELEMENT),
               W_TRUE );

         pBindingContext->nNextWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_RESTORE_POLICY;

         /* no callback will be called : walk through */

      case P_NFC_HAL_BINDING_WAKE_UP_RESTORE_POLICY:
      {
         uint32_t nLen;

         PNALBindingBuildHCICommandGenericPolicy(pBindingContext, NAL_SERVICE_ADMIN, pBindingContext->aParam_NAL_PAR_POLICY, NAL_POLICY_SIZE,
                                                         pBindingContext->aHCISendBuffer, &nLen, W_FALSE, W_TRUE);

         if (nLen != 0) {
            pBindingContext->nNextWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_RESTORE_CARD_DETECT;

            PHCIServiceExecuteMultiServiceCommand(
               pBindingContext,
               & pBindingContext->sNALBindingWakeUpOperation,
               P_HCI_SERVICE_CMD_SET_PROPERTIES,
               pBindingContext->aHCISendBuffer,
               nLen,
               pBindingContext->aHCIResponseBuffer,
               sizeof(pBindingContext->aHCIResponseBuffer),
               static_PNALBindingWakeUpSetParameterCompleted,
               null,
               W_TRUE );

            break;
         }

         /* Explicit fall-through */
         pBindingContext->nCurrentWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_RESTORE_CARD_DETECT;
      }

      case P_NFC_HAL_BINDING_WAKE_UP_RESTORE_CARD_DETECT:
      {
         uint32_t nLen;

         PNALBindingBuildHCICommandPulse(pBindingContext, NAL_SERVICE_ADMIN, pBindingContext->aParam_NAL_PAR_DETECT_PULSE, sizeof(pBindingContext->aParam_NAL_PAR_DETECT_PULSE),
                                                         pBindingContext->aHCISendBuffer, &nLen);
         if (nLen != 0) {

            pBindingContext->nNextWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_RESTORE_ACTIVATED_READERS;

            PHCIServiceExecuteMultiServiceCommand(
               pBindingContext,
               & pBindingContext->sNALBindingWakeUpOperation,
               P_HCI_SERVICE_CMD_SET_PROPERTIES,
               pBindingContext->aHCISendBuffer,
               nLen,
               pBindingContext->aHCIResponseBuffer,
               sizeof(pBindingContext->aHCIResponseBuffer),
               static_PNALBindingWakeUpSetParameterCompleted,
               null,
               W_TRUE );

            break;
         }

         /* Explicit fall-through */
         pBindingContext->nCurrentWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_RESTORE_ACTIVATED_READERS;
      }

      case P_NFC_HAL_BINDING_WAKE_UP_RESTORE_ACTIVATED_READERS :
      {
         pBindingContext->nNextWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_COMPLETED;

         if(pBindingContext->nLastNALReaderProtocol != 0)
         {
            uint16_t nHCIReaderProtocolCapa;

            static_PNALBindingGetHCIReaderProtocolCapabilities(
                                          &nHCIReaderProtocolCapa,
                                          pBindingContext->nLastNALReaderProtocol);

            static_PNALBindingWriteUint16ToHCIBuffer(nHCIReaderProtocolCapa, pBindingContext->aHCISendBuffer);

            /* Enable all selected reader protocol */
            PHCIServiceSendEvent(
                        pBindingContext,
                        pBindingContext->nOneOfOpenedReaderServiceId,
                      & pBindingContext->sNALBindingWakeUpOperation,
                        HCI2_EVT_MREADER_DISCOVERY_START_SOME,
                        pBindingContext->aHCISendBuffer,
                        0x02,
                        static_PNALBindingWakeUpDetectionReaderSendEventCompleted,
                        0,
                        W_TRUE);
            break;
         }

         pBindingContext->nCurrentWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_COMPLETED;
      }
      /* Explicit fall-through */

      case P_NFC_HAL_BINDING_WAKE_UP_COMPLETED :

         /* Wake up completed */
         pBindingContext->nHCIMode = W_NFCC_MODE_ACTIVE;

         /* process any command / event pending at HCI service level */
         PHCIServiceKick(pBindingContext);

         /* process any command pending at the NFC HAL level */
         if (pBindingContext->pNALWriteCallbackFunction != null)
         {
            /* this is mandatory since its the only case of reentrancy of a write operation */
            tNALBindingWriteCompleted * pCallback = pBindingContext->pNALWriteCallbackFunction;
            pBindingContext->pNALWriteCallbackFunction = null;

            PNALBindingWrite(
               pBindingContext,
               pBindingContext->pNALWriteBuffer,
               pBindingContext->nNALWriteBufferLength,
               pCallback,
               pBindingContext->pNALWriteCallbackParameter);
         }
         break;
   }
}

