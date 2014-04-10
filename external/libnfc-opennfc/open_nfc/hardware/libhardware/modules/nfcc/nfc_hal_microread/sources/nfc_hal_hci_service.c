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
   Contains the implementation of the HCI service functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( SERVI )

#include "nfc_hal_binding.h"

/* The number or HCI retries if the command is not recognized by the NFC Controller */
#define P_HCI_SERVICE_MAX_RETRY 0

/*******************************************************************************
    The service/pipe table
*******************************************************************************/
/*
  Constant pipe values used with NFC Controller

   NFC,  GATE_LMS,               null, GATE_LMS              0x00 HCI2_PIPE_ID_LMS
   NFC,  GATE_ADM,               null, GATE_ADM              0x01 HCI2_PIPE_ID_ADM
   NFC,  GATE_MGT,               HDS,  GATE_MGT              0x02 HCI2_PIPE_ID_MGT
   NFC,  GATE_OS,                HDS,  GATE_OS               0x03 HCI2_PIPE_ID_OS
   NFC,  GATE_LOOPBACK,          HDS,  GATE_LOOPBACK         0x04 HCI2_PIPE_ID_LOOPBACK
   NFC,  GATE_IDENTITY,          HDS,  GATE_IDENTITY         0x05 HCI2_PIPE_ID_HDS_IDT
   HDS,  GATE_LOOPBACK,          SIM,  GATE_LOOPBACK         0x06 HCI2_PIPE_ID_HDS_SIM_LOOPBACK
   HDS,  GATE_CONNECTIVITY,      SIM,  GATE_CONNECTIVITY     0x07 HCI2_PIPE_ID_HDS_SIM_CONNECTIVITY

   NFC,  GATE_MCARD_ISO_B,       HDS,  GATE_MCARD_ISO_B,     0x08 HCI2_PIPE_ID_MCARD_NFC_HDS_ISO_B
   NFC,  GATE_MCARD_BPRIME,      HDS,  GATE_MCARD_BPRIME,    0x09
   NFC,  GATE_MCARD_ISO_A,       HDS,  GATE_MCARD_ISO_A,     0x0A
   NFC,  GATE_MCARD_ISO_15_3,    HDS,  GATE_MCARD_ISO_15_3,  0x0B
   NFC,  GATE_MCARD_ISO_15_2,    HDS,  GATE_MCARD_ISO_15_2,  0x0C
   NFC,  GATE_MCARD_FELICA,      HDS,  GATE_MCARD_FELICA,    0x0D
   NFC,  GATE_MCARD_ISO_B_2,     HDS,  GATE_MCARD_ISO_B_2,   0x0E
   NFC,  GATE_MCARD_CUSTOM,      HDS,  GATE_MCARD_CUSTOM,    0x0F

   NFC,  GATE_MREAD_ISO_B,       HDS,  GATE_MREAD_ISO_B,     0x10 HCI2_PIPE_ID_HDS_MREAD_GENER_NFC_HDS_ISO_B
   NFC,  GATE_MREAD_NFC_T1,      HDS,  GATE_MREAD_NFC_T1,    0x11
   NFC,  GATE_MREAD_ISO_A,       HDS,  GATE_MREAD_ISO_A,     0x12
   NFC,  GATE_MREAD_ISO_15_3,    HDS,  GATE_MREAD_ISO_15_3,  0x13
   NFC,  GATE_MREAD_ISO_15_2,    HDS,  GATE_MREAD_ISO_15_2,  0x14
   NFC,  GATE_MREAD_FELICA,      HDS,  GATE_MREAD_FELICA,    0x15
   NFC,  GATE_MREAD_ISO_B_2,     HDS,  GATE_MREAD_ISO_B_2,   0x16
   NFC,  GATE_MREAD_CUSTOM,      HDS,  GATE_MREAD_CUSTOM,    0x17

*/

/* The syntax is the following
    bit 15: set if the service does not need to be open
    bit 14: set if the service is considered as open at boot time
    bit 13: set if the service accepts commands even in closed mode
    bit 7-0: pipe identifier
 */
#ifdef HCI_SWP
/* Service state are different in SIM->SWP comunication mode */
static const uint16_t g_aServicePipe[P_HCI_SERVICE_MAX_ID + 1] =
{
   0x01 | 0x6000,   /* 0x00   Administration             ( NFC Device <-> NFC Controller )  */
   0x02 | 0x6000,   /* 0x01   Pipe Management            ( NFC Device <-> NFC Controller )  */
   0x03 | 0x6000,   /* 0x02   Firmware (patchs)          ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x03   Identity                   ( NFC Device <-> NFC Controller )  */
   0x00 | 0x6000,   /* 0x04   Link management            ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x05   Mode Reader ISO 14443 A-4  ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x06   Mode Reader ISO 14443 B-4  ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x07   Mode Reader ISO 14443 A-3  ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x08   Mode Reader ISO 14443 B-3  ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x09   Mode Reader NFC_T1        ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x0A   Mode Reader FeliCa         ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x0B   Mode Reader ISO 15693-3    ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x0C   Mode Reader ISO 15693-2    ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x0D   P2P Initiator              ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x0E   Mode Card ISO 14443 A-4    ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x0F   Mode Card ISO 14443 B-4    ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x10   Mode Card B  PRIME         ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x11   Mode Card ISO 15693-3      ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x12   Mode Card FeliCa           ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x13   P2P Target                 ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x14   UICC Connectivity Gate     ( NFC Device <-> UICC )            */
   0xFF | 0x6000,   /* 0x15   Generic Reader             ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x16   SE                         ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x17   Instances                  ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x18   RF                         ( NFC Device <-> NFC Controller )  */
   0xFF | 0x6000,   /* 0x19   LoopBack                   ( NFC Device <-> NFC Controller )  */
};
#else
static const uint16_t g_aServicePipe[P_HCI_SERVICE_MAX_ID + 1] =
{
   0x01 | 0x8000,   /* 0x00   Administration             ( NFC Device <-> NFC Controller )  */
   0x02 | 0x6000,   /* 0x01   Pipe Management            ( NFC Device <-> NFC Controller )  */
   0x03 | 0x8000,   /* 0x02   Firmware (patchs)          ( NFC Device <-> NFC Controller )  */
   0x05 | 0x8000,   /* 0x03   Identity                   ( NFC Device <-> NFC Controller )  */
   0x00 | 0x8000,   /* 0x04   Link management            ( NFC Device <-> NFC Controller )  */
   0x12 | 0x6000,   /* 0x05   Mode Reader ISO 14443 A-4  ( NFC Device <-> NFC Controller )  */
   0x10 | 0x6000,   /* 0x06   Mode Reader ISO 14443 B-4  ( NFC Device <-> NFC Controller )  */
   0x18 | 0x6000,   /* 0x07   Mode Reader ISO 14443 A-3  ( NFC Device <-> NFC Controller )  */
   0x16 | 0x6000,   /* 0x08   Mode Reader ISO 14443 B-3  ( NFC Device <-> NFC Controller )  */
   0x11 | 0x6000,   /* 0x09   Mode Reader NFC_T1         ( NFC Device <-> NFC Controller )  */
   0x15 | 0x6000,   /* 0x0A   Mode Reader FeliCa         ( NFC Device <-> NFC Controller )  */
   0x13 | 0x2000,   /* 0x0B   Mode Reader ISO 15693-3    ( NFC Device <-> NFC Controller )  */
   0x14 | 0x2000,   /* 0x0C   Mode Reader ISO 15693-2    ( NFC Device <-> NFC Controller )  */
   0x20 | 0x6000,   /* 0x0D   P2P Initiator              ( NFC Device <-> NFC Controller )  */
   0x0A | 0x4000,   /* 0x0E   Mode Card ISO 14443 A-4    ( NFC Device <-> NFC Controller )  */
   0x08 | 0x4000,   /* 0x0F   Mode Card ISO 14443 B-4    ( NFC Device <-> NFC Controller )  */
   0x09 | 0x4000,   /* 0x10   Mode Card B  PRIME         ( NFC Device <-> NFC Controller )  */
   0x0B | 0x4000,   /* 0x11   Mode Card ISO 15693-3      ( NFC Device <-> NFC Controller )  */
   0x0D | 0x4000,   /* 0x12   Mode Card FeliCa           ( NFC Device <-> NFC Controller )  */
   0x1F | 0x4000,   /* 0x13   P2P Target                 ( NFC Device <-> NFC Controller )  */
   0x07 | 0x8000,   /* 0x14   UICC Connectivity Gate     ( NFC Device <-> UICC )            */
   0x1B | 0x8000,   /* 0x15   Generic Reader             ( NFC Device <-> NFC Controller )  */
   0x1C | 0x8000,   /* 0x16   SE                         ( NFC Device <-> NFC Controller )  */
   0x1D | 0x8000,   /* 0x17   Instances                  ( NFC Device <-> NFC Controller )  */
   0x1E | 0x8000,   /* 0x18   RF                         ( NFC Device <-> NFC Controller )  */
   0x21 | 0x8000,   /* 0x19   Generic Card               ( NFC Device <-> NFC Controller )  */
   0x17 | 0x6000,   /* 0x1A   Mode Reader BPRIME         ( NFC Device <-> NFC Controller )  */
   0x04 | 0x8000,   /* 0x1B   Loopback                   ( NFC Device <-> NFC Controller )  */
   0x06 | 0x8000,   /* 0x1C   Sim Loopback               ( NFC Device <-> NFC Controller )  */
   0x0C | 0x8000,   /* 0x1D   Mode Card ISO 15693 Part-2 ( NFC Device <-> NFC Controller )  */
   0x0E | 0x8000,   /* 0x1E   Mode Card ISO 14443 B-2    ( NFC Device <-> NFC Controller )  */
   0x0F | 0x8000,   /* 0x1F   Mode Card Custom           ( NFC Device <-> NFC Controller )  */
   0x19 | 0x8000,   /* 0x20   Mode Reader NFC A          ( NFC Device <-> NFC Controller )  */
   0x1A | 0x8000,   /* 0x21   Mode Reader NFC F          ( NFC Device <-> NFC Controller )  */
   0x22 | 0x6000,   /* 0x22   Mode Reader KOVIO          ( NFC Device <-> NFC Controller )  */
};
#endif
/*******************************************************************************
   Instance States
*******************************************************************************/
#define P_HCI_STATE_DISCONNECTED       0x00
#define P_HCI_STATE_CONNECTED_PENDING  0x01
#define P_HCI_STATE_CONNECTED          0x02

/*******************************************************************************
   Service States
*******************************************************************************/
#define P_HCI_SERVICE_STATE_CLOSED           0x00
#define P_HCI_SERVICE_STATE_OPEN_PENDING     0x01
#define P_HCI_SERVICE_STATE_OPEN             0x02
#define P_HCI_SERVICE_STATE_CLOSE_PENDING    0x03
#define P_HCI_SERVICE_STATE_OPEN_INACTIVE    0x04

/*******************************************************************************
   Static Functions
*******************************************************************************/

static void static_PHCIServiceFrameReadDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nOffset,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

static void static_PHCIServiceFrameConnectCompleted(
         tNALBindingContext* pBindingContext,
         void*     pCallbackParameter )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceContext* pServiceContext;
   uint32_t nServiceIdentifier;

   CNALDebugAssert(pInstance->nState == P_HCI_STATE_CONNECTED_PENDING);

   for(nServiceIdentifier = 0; nServiceIdentifier <= P_HCI_SERVICE_MAX_ID; nServiceIdentifier++)
   {
      pServiceContext = &pInstance->aServiceList[nServiceIdentifier];

      CNALDebugAssert ( pServiceContext->nState == P_HCI_SERVICE_STATE_CLOSED );

      pServiceContext->pCurrentReadOperation = null;

      if((g_aServicePipe[nServiceIdentifier] & 0x8000) != 0)
      {
         pServiceContext->nState = P_HCI_SERVICE_STATE_OPEN_INACTIVE;
      }
      if((g_aServicePipe[nServiceIdentifier] & 0x4000) != 0)
      {
         pServiceContext->nState = P_HCI_SERVICE_STATE_OPEN;
      }

      pServiceContext->pOperationListHead = null;

      PHCIFrameReadStream(
         pBindingContext,
         &pServiceContext->sReadContext,
         pServiceContext->nPipeIdentifier,
         static_PHCIServiceFrameReadDataReceived,
         pServiceContext );
   }

   pInstance->nState = P_HCI_STATE_CONNECTED;

   /* Post Application callback */
   PNALDFCPost1(
      pBindingContext,
      P_DFC_TYPE_HCI_SERVICE,
      pInstance->pCallbackFunction,
      pInstance->pCallbackParameter);
}

#ifdef P_NAL_DEBUG_ACTIVE
static void static_PHCIServiceCheckOperation(
   tHCIServiceOperation* pOperation)
{
   tHCIServiceContext* pService = pOperation->pService;
   tHCIServiceOperation* pOtherOperation;

   CNALDebugAssert(pOperation->pNext != pOperation);
   if(pService != null)
   {
      pOtherOperation = pService->pOperationListHead;

      while(pOtherOperation != null)
      {
         if(pOtherOperation == pOperation)
         {
            return;
         }
         pOtherOperation = pOtherOperation->pNext;
      }
      CNALDebugAssert(W_FALSE);
   }
}
#else /* #ifdef P_NAL_DEBUG_ACTIVE */
#define static_PHCIServiceCheckOperation(X)
#endif /* #ifdef P_NAL_DEBUG_ACTIVE */

static void static_PHCIServiceResetInstance(
         tNALBindingContext* pBindingContext,
         tHCIServiceInstance* pInstance )
{
   tHCIServiceContext* pService;
   uint32_t nServiceIdentifier;

   for(nServiceIdentifier = 0; nServiceIdentifier <= P_HCI_SERVICE_MAX_ID; nServiceIdentifier++)
   {
      tHCIServiceOperation* pOperation;

      pService = &pInstance->aServiceList[nServiceIdentifier];
      pOperation = pService->pOperationListHead;

      while( pOperation != null )
      {
         tHCIServiceOperation* pNextOperation = pOperation->pNext;
         tHCIServiceOperationType* pType = pOperation->pType;

         CNALMemoryFill(pOperation, 0, sizeof(tHCIServiceOperation));

         pOperation->pType = pType;
         pOperation->nState = P_HCI_OPERATION_STATE_COMPLETED;
         pOperation = pNextOperation;
      }

      if(pBindingContext != null)
      {
         if(pService->nState != P_HCI_SERVICE_STATE_CLOSED)
         {
            PHCIFrameCancelReadStream(pBindingContext, &pService->sReadContext);
         }
      }
   }

   CNALMemoryFill(pInstance, 0, sizeof(tHCIServiceInstance));
   pInstance->nState = P_HCI_STATE_DISCONNECTED;

   /* pInstance->nLock = P_HCI_UNLOCK; */

   for(nServiceIdentifier = 0; nServiceIdentifier <= P_HCI_SERVICE_MAX_ID; nServiceIdentifier++)
   {
      pService = &pInstance->aServiceList[nServiceIdentifier];

      pService->nState = P_HCI_SERVICE_STATE_CLOSED;
      pService->nPipeIdentifier = (uint8_t)(g_aServicePipe[nServiceIdentifier] & 0x00FF);
   }
}

static void static_PHCIServiceFrameReconnectIndication(
         tNALBindingContext* pBindingContext,
         void*     pCallbackParameter );

static void static_PHCIServiceInactivityTimeoutElapsed(
      tNALBindingContext * pBindingContext,
      void     * pCallbackParameter);

static void static_PHCIServiceExecuteNextOperation(
         tNALBindingContext* pBindingContext,
         tHCIServiceOperation* pOperation)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance(pBindingContext);

   if(pOperation != null)
   {
      /* Add the operation to the pending operation list and set the state to "scheduled" */

      tHCIServiceOperation* pCurrentOperation = pInstance->pPendingOperationListHead;

      static_PHCIServiceCheckOperation(pOperation);

      pOperation->pNextPending = null;

      pOperation->nState = P_HCI_OPERATION_STATE_SCHEDULED;

      if(pCurrentOperation != null)
      {
         while(pCurrentOperation->pNextPending != null)
         {
            pCurrentOperation = pCurrentOperation->pNextPending;
         }

         pCurrentOperation->pNextPending = pOperation;
      }
      else
      {
         pInstance->pPendingOperationListHead = pOperation;
      }
   }

   /* Check if there's an operation waiting to be processed */

   switch (pBindingContext->nHCIMode)
   {
      case W_NFCC_MODE_BOOT_PENDING :
      case W_NFCC_MODE_SWITCH_TO_STANDBY :
      case W_NFCC_MODE_SWITCH_TO_ACTIVE :

         /* In this modes, only the special mode operations are processed */

         for (pOperation = pInstance->pPendingOperationListHead; pOperation != null; pOperation = pOperation->pNextPending)
         {
            if (pOperation->bIsSpecialModeCommand == W_TRUE)
               break;
         }
         break;

      case W_NFCC_MODE_STANDBY :

         CNALDebugAssert(pInstance->pPendingOperationListHead == null);
         return;

      default :

         /* Other modes : all operations can be processed */
         pOperation = pInstance->pPendingOperationListHead;
   }

   if ((pOperation != null) && (pInstance->bWaitingForAnswer == W_FALSE))
   {
      static_PHCIServiceCheckOperation(pOperation);

      CNALDebugAssert(pOperation->nState == P_HCI_OPERATION_STATE_SCHEDULED);
      pInstance->pPendingOperationListHead = pOperation->pNextPending;
      pOperation->pNextPending = null;

      /* Start the operation */
      pOperation->pType->pStartFunction( pBindingContext, pOperation );
   }
}

/* Remove from the operation list and set the state to completed */
static void static_PHCISetOperationCompleted(
         tHCIServiceOperation* pOperation )
{
   tHCIServiceContext* pService = pOperation->pService;
   tHCIServiceOperation** ppCurrentOperation;

   static_PHCIServiceCheckOperation(pOperation);

   pOperation->nState = P_HCI_OPERATION_STATE_COMPLETED;

   for(ppCurrentOperation = &pService->pOperationListHead;
      *ppCurrentOperation != pOperation;
      ppCurrentOperation = &((*ppCurrentOperation)->pNext))
   {
      CNALDebugAssert(*ppCurrentOperation != null);
   }
   *ppCurrentOperation = pOperation->pNext;
   CNALDebugAssert(pOperation != pOperation->pNext);
   if(*ppCurrentOperation != null)
   {
      CNALDebugAssert(*ppCurrentOperation != (*ppCurrentOperation)->pNext);
   }

   pOperation->pNext = null;
   pOperation->pService = null;
}

static void static_PHCIServiceCancelAll(
         tNALBindingContext* pBindingContext,
         tHCIServiceContext* pServiceContext)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceOperation* pOperation;

   for(pOperation = pServiceContext->pOperationListHead;
      pOperation != null;
      pOperation = pOperation->pNext)
   {
      static_PHCIServiceCheckOperation(pOperation);

      if((pOperation->nState != P_HCI_OPERATION_STATE_COMPLETED)
      && (pOperation->nState != P_HCI_OPERATION_STATE_CANCELLED))
      {
         if(pOperation->pType->pCancelFunction != null)
         {
            if(pOperation->nState == P_HCI_OPERATION_STATE_SCHEDULED)
            {
               tHCIServiceOperation** ppCurrentOperation;

               pOperation->pType->pCancelFunction(pBindingContext, pOperation);

               /* Remove the operation from the pending list */
               for(ppCurrentOperation = &pInstance->pPendingOperationListHead;
                  *ppCurrentOperation != pOperation;
                  ppCurrentOperation = &((*ppCurrentOperation)->pNextPending))
               {
                  CNALDebugAssert(*ppCurrentOperation != null);
               }
               *ppCurrentOperation = pOperation->pNextPending;
               CNALDebugAssert(pOperation != pOperation->pNext);
               if(*ppCurrentOperation != null)
               {
                  CNALDebugAssert(*ppCurrentOperation != (*ppCurrentOperation)->pNext);
               }
            }
            pOperation->nState = P_HCI_OPERATION_STATE_CANCELLED;
         }
      }
   }
}

/*******************************************************************************
   Close Operation Functions
*******************************************************************************/

static void static_PHCIServiceCloseWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;

   static_PHCIServiceCheckOperation(pOperation);

   CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_WRITE_PENDING );
   pOperation->nState = P_HCI_OPERATION_STATE_READ_PENDING;
}

static void static_PHCIServiceCloseStart(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

   static_PHCIServiceCheckOperation(pOperation);

   PHCIFrameWritePrepareContext(
      pBindingContext, &pOperation->sWriteContext,
      pOperation->pService->nPipeIdentifier,
      1,
      static_PHCIServiceCloseWriteCompleted, pOperation );

   pOperation->info.close.aWriteBuffer[0] = ETSI_CMD_ANY_CLOSE_PIPE;

   PHCIFrameWrite( pBindingContext, &pOperation->sWriteContext, pOperation->info.close.aWriteBuffer );
}

static void static_PHCIServiceCloseReadDataReceived(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint32_t nOffset,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   uint8_t nResult;
   tHCIServiceContext* pService = pOperation->pService;

   static_PHCIServiceCheckOperation(pOperation);

   if((nOffset != 0) || (nLength != 1) || (nHCIMessageReceptionCounter == 0))
   {
      PNALDebugWarning("static_PHCIServiceCloseReadDataReceived %d bytes lost", nLength);
      pInstance->nReadBytesLost += nLength;
      pInstance->nReadMessageLostCount++;
      return;
   }

   CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING );

   nResult = pBuffer[0];
   CNALDebugAssert( (nResult&ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_ANS);
   nResult &= ETSI_MSG_INFO_MASK;

   if(nResult != ETSI_ERR_ANY_OK)
   {
      if((nResult != ETSI_ERR_ANY_E_PIPE_NOT_OPENED)
      && (nResult != ETSI_ERR_ANY_E_NOK))
      {
         PNALDebugError("static_PHCIServiceCloseReadDataReceived: Error 0x%02X returned by NFCC", nResult);
         if(pOperation->nHCIRetryCount < P_HCI_SERVICE_MAX_RETRY)
         {
            pOperation->nHCIRetryCount++;
            PNALDebugWarning("static_PHCIServiceCloseReadDataReceived: Maybe a RX/TX error, retry (%d/%d)",
               pOperation->nHCIRetryCount, P_HCI_SERVICE_MAX_RETRY);
            pOperation->pType->pStartFunction(pBindingContext, pOperation);
            return;
         }
      }
      else if(nResult == ETSI_ERR_ANY_E_PIPE_NOT_OPENED)
      {
         PNALDebugWarning("static_PHCIServiceCloseReadDataReceived: ANY_E_PIPE_NOT_OPENED ignored, the pipe was not open");
         nResult = ETSI_ERR_ANY_OK;
      }
      else
      {
         PNALDebugWarning("static_PHCIServiceCloseReadDataReceived: ETSI_ERR_ANY_E_NOK ignored, the pipe is opened by UICC");
         nResult = ETSI_ERR_ANY_OK;
      }
   }

   /* Remove from the operation list and set the state to completed */
   static_PHCISetOperationCompleted(pOperation);

   /* Send the result */
   PNALDFCPost1(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.close.pCallbackFunction,
      pOperation->info.close.pCallbackParameter);

   pService->pCurrentReadOperation = null;
   pService->nState = P_HCI_SERVICE_STATE_CLOSED;

   /* Process the next pending operation, if any */
   static_PHCIServiceExecuteNextOperation(pBindingContext, null);
}

static tHCIServiceOperationType P_HCI_SERVICE_CLOSE_OPERATION =
{
   static_PHCIServiceCloseStart,
   null, /* Open is not cancellable */
   static_PHCIServiceCloseReadDataReceived,
};

/*******************************************************************************
   Open Operation Functions
*******************************************************************************/

static void static_PHCIServiceOpenWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;

   static_PHCIServiceCheckOperation(pOperation);

   CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_WRITE_PENDING );
   pOperation->nState = P_HCI_OPERATION_STATE_READ_PENDING;
}

static void static_PHCIServiceOpenStart(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

   static_PHCIServiceCheckOperation(pOperation);

   PHCIFrameWritePrepareContext(
      pBindingContext, &pOperation->sWriteContext,
      pOperation->pService->nPipeIdentifier,
      1,
      static_PHCIServiceOpenWriteCompleted, pOperation );

   pOperation->info.open.aWriteBuffer[0] = ETSI_CMD_ANY_OPEN_PIPE;

   PHCIFrameWrite( pBindingContext, &pOperation->sWriteContext, pOperation->info.open.aWriteBuffer );
}

static void static_PHCIServiceOpenReadDataReceived(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint32_t nOffset,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   uint8_t nResult;
   W_ERROR nError;
   tHCIServiceContext* pServiceContext = pOperation->pService;

   static_PHCIServiceCheckOperation(pOperation);

   if((nOffset != 0) || (nLength != 1) || (nHCIMessageReceptionCounter == 0))
   {
      PNALDebugWarning("static_PHCIServiceOpenReadDataReceived %d bytes lost", nLength);
      pInstance->nReadBytesLost += nLength;
      pInstance->nReadMessageLostCount++;
      return;
   }

   CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING );

   nResult = pBuffer[0];
   CNALDebugAssert( (nResult&ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_ANS);
   nResult &= ETSI_MSG_INFO_MASK;

   switch(nResult)
   {
      case ETSI_ERR_ANY_OK:
         nError = W_SUCCESS;
         break;
      case ETSI_ERR_ANY_E_PIPE_NOT_OPENED:
         nError = W_ERROR_NFCC_COMMUNICATION;
         break;
      case ETSI_ERR_ANY_E_REG_ACCESS_DENIED:
         nError = W_ERROR_EXCLUSIVE_REJECTED;
         break;
      default:
         nError = W_ERROR_NFCC_COMMUNICATION;
         break;
   }

   if(nError != W_SUCCESS)
   {
      if(pOperation->nHCIRetryCount < P_HCI_SERVICE_MAX_RETRY)
      {
         pOperation->nHCIRetryCount++;
         PNALDebugWarning("static_PHCIServiceOpenReadDataReceived: Maybe a RX/TX error, retry (%d/%d)",
            pOperation->nHCIRetryCount, P_HCI_SERVICE_MAX_RETRY);
         pOperation->pType->pStartFunction(pBindingContext, pOperation);
         return;
      }
   }

   /* Remove from the operation list and set the state to completed */
   static_PHCISetOperationCompleted(pOperation);

   CNALDebugAssert((pServiceContext->nState == P_HCI_SERVICE_STATE_OPEN_PENDING)
      || (pServiceContext->nState == P_HCI_SERVICE_STATE_CLOSE_PENDING));

   if(pServiceContext->nState == P_HCI_SERVICE_STATE_OPEN_PENDING)
   {
      /* Send the result */
      PNALDFCPost2(
         pBindingContext, P_DFC_TYPE_HCI_SERVICE,
         pOperation->info.open.pCallbackFunction, pOperation->info.open.pCallbackParameter,
         PNALUtilConvertUint32ToPointer(nError));

      if(nError == W_SUCCESS)
      {
          /* Set the service status to "open" */
          pServiceContext->nState = P_HCI_SERVICE_STATE_OPEN;
      }
      else
      {
         /* Unregister the read stream */
         CNALDebugAssert(pServiceContext->pCurrentReadOperation == null);
         CNALDebugAssert(pServiceContext->pOperationListHead == null);
         pServiceContext->nState = P_HCI_SERVICE_STATE_CLOSED;
      }

      /* Process the next pending operation, if any */
      static_PHCIServiceExecuteNextOperation(pBindingContext, null);
   }
   else
   {
      /* Retrieve the close callback info stored in the open structure */
      void* pCallbackParameter = pServiceContext->sInnerOperation.info.open.pCallbackParameter;
      tPNALGenericCompletion* pCallbackFunction =
         (tPNALGenericCompletion*)pServiceContext->sInnerOperation.info.open.pCallbackFunction;

      /* The service was closed while the open operation was pending: execute the close operation now */

      /* Check that no operation is pending */
      CNALDebugAssert(pServiceContext->pOperationListHead == null);

      /**
       * Initiate the close service operation
       */
      /* Add the opration in the list */
      pOperation = &pServiceContext->sInnerOperation;
      pOperation->pNext = pServiceContext->pOperationListHead;
      pServiceContext->pOperationListHead = pOperation;
      pOperation->pService = pServiceContext;
      pOperation->nHCIRetryCount = 0;
      pOperation->pType = &P_HCI_SERVICE_CLOSE_OPERATION;
      pOperation->info.close.pCallbackFunction = pCallbackFunction;
      pOperation->info.close.pCallbackParameter = pCallbackParameter;

      static_PHCIServiceCheckOperation(pOperation);

      /* Process the next pending operation if any, otherwise unlock the queue */
      static_PHCIServiceExecuteNextOperation(pBindingContext, null);

      /* Then post the close operation */
      static_PHCIServiceExecuteNextOperation(pBindingContext, pOperation);
   }
}

static tHCIServiceOperationType P_HCI_SERVICE_OPEN_OPERATION =
{
   static_PHCIServiceOpenStart,
   null, /* Open is not cancellable */
   static_PHCIServiceOpenReadDataReceived,
};

/*******************************************************************************
   Receive Event Operation Functions
*******************************************************************************/
static void static_PHCIServiceRecvEventCancel(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   /* Nothing to do */
}


static void static_PHCIServiceRecvEventReadDataReceivedDFC(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint8_t* pBuffer,
                  uint32_t nOffset,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)

{
   CNALDebugAssert(nLength > 0);

   static_PHCIServiceCheckOperation(pOperation);

   if(nOffset == 0)
   {
      CNALDebugAssert((pBuffer[0] & ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_EVT);
      pOperation->info.recv_event.nEventIdentifier = pBuffer[0] & ETSI_MSG_INFO_MASK;

      pBuffer++;
      nLength--;
   }
   else
   {
      nOffset--;
   }

   pOperation->info.recv_event.pCallbackFunction(
         pBindingContext,
         pOperation->info.recv_event.pCallbackParameter,
         pOperation->info.recv_event.nEventIdentifier,
         nOffset,
         pBuffer,
         nLength,
         nHCIMessageReceptionCounter
      );
}


static void static_PHCIServiceRecvEventReadDataReceived(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint32_t nOffset,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)
{

   /* We need to post a DFC here to ensure all data paths will go through the same number of DFC.
      Otherwise, message crossing may appear */

   PNALDFCPost5(
         pBindingContext, P_DFC_TYPE_HCI_SERVICE,
         static_PHCIServiceRecvEventReadDataReceivedDFC,
         pOperation,
         pBuffer,
         nOffset,
         nLength,
         nHCIMessageReceptionCounter
         );
}


static tHCIServiceOperationType P_HCI_SERVICE_RECV_EVENT_OPERATION =
{
   null,
   static_PHCIServiceRecvEventCancel,
   static_PHCIServiceRecvEventReadDataReceived,
};
/*******************************************************************************
   Receive Command Operation Functions
*******************************************************************************/
static void static_PHCIServiceRecvCmdCancel(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   /* Nothing to do */
}


static void static_PHCIServiceRecvCmdReadDataReceivedDFC(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint8_t* pBuffer,
                  uint32_t nOffset,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)
{
   CNALDebugAssert(nLength > 0);

   static_PHCIServiceCheckOperation(pOperation);

   if(nOffset == 0)
   {
      CNALDebugAssert((pBuffer[0] & ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_CMD);
      pOperation->info.recv_cmd.nCommandIdentifier = pBuffer[0] & ETSI_MSG_INFO_MASK;

      pBuffer++;
      nLength--;
   }
   else
   {
      nOffset--;
   }

   pOperation->info.recv_cmd.pCallbackFunction(
         pBindingContext,
         pOperation->info.recv_cmd.pCallbackParameter,
         pOperation->info.recv_cmd.nCommandIdentifier,
         nOffset,
         pBuffer,
         nLength,
         nHCIMessageReceptionCounter
         );
}

static void static_PHCIServiceRecvCmdReadDataReceived(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint32_t nOffset,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)
{

   /* We need to post a DFC here to ensure all data paths will go through the same number of DFC.
      Otherwise, message crossing may appear */

   PNALDFCPost5(
         pBindingContext, P_DFC_TYPE_HCI_SERVICE,
         static_PHCIServiceRecvCmdReadDataReceivedDFC,
         pOperation,
         pBuffer,
         nOffset,
         nLength,
         nHCIMessageReceptionCounter
         );
}

static tHCIServiceOperationType P_HCI_SERVICE_RECV_CMD_OPERATION =
{
   null,
   static_PHCIServiceRecvCmdCancel,
   static_PHCIServiceRecvCmdReadDataReceived,
};

/*******************************************************************************
   Send Event Operation Functions
*******************************************************************************/
static void static_PHCIServiceSendEventCancel(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   static_PHCIServiceCheckOperation(pOperation);

    PNALDFCPost3(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.send_event.pCallbackFunction,
      pOperation->info.send_event.pCallbackParameter,
      PNALUtilConvertUint32ToPointer(W_ERROR_CANCEL), 0);
}

static void static_PHCIServiceSendEventWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;

   static_PHCIServiceCheckOperation(pOperation);

   if ( pOperation->nState != P_HCI_OPERATION_STATE_CANCELLED )
   {
      CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_WRITE_PENDING );

      /* Remove from the operation list and set the state to completed */
      static_PHCISetOperationCompleted(pOperation);

      /* Send the result */
      PNALDFCPost3(
         pBindingContext, P_DFC_TYPE_HCI_SERVICE,
         pOperation->info.send_event.pCallbackFunction,
         pOperation->info.send_event.pCallbackParameter,
         W_SUCCESS, nReceptionCounter);
   }

   /* Process the next pending operation, if any */
   static_PHCIServiceExecuteNextOperation(pBindingContext, null);
}

static void static_PHCIServiceSendEventFrameGetData(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pDestinationBuffer,
         uint32_t nMessageOffset,
         uint32_t nLength )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;
   CNALDebugAssert(nLength != 0);

   static_PHCIServiceCheckOperation(pOperation);

   /* Check that the input buffer is in range */
   CNALDebugAssert(nMessageOffset + nLength <= pOperation->info.send_event.nInputBufferLength + 1);

   if(nMessageOffset == 0)
   {
      *pDestinationBuffer++ = ETSI_MSG_TYPE_EVT | pOperation->info.send_event.nEventCode;

      CNALMemoryCopy(pDestinationBuffer, pOperation->info.send_event.pInputBuffer, nLength - 1);
   }
   else
   {
      CNALMemoryCopy(pDestinationBuffer,
         &pOperation->info.send_event.pInputBuffer[nMessageOffset - 1], nLength);
   }
}

static void static_PHCIServiceSendEventStart(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   static_PHCIServiceCheckOperation(pOperation);

   pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

   PHCIFrameWritePrepareContext(
      pBindingContext, &pOperation->sWriteContext,
      pOperation->pService->nPipeIdentifier,
      pOperation->info.send_event.nInputBufferLength + 1,
      static_PHCIServiceSendEventWriteCompleted, pOperation );

   PHCIFrameWriteStream( pBindingContext, &pOperation->sWriteContext,
         static_PHCIServiceSendEventFrameGetData, pOperation );
}

static tHCIServiceOperationType P_HCI_SERVICE_SEND_EVENT_OPERATION =
{
   static_PHCIServiceSendEventStart,
   static_PHCIServiceSendEventCancel,
   null,
};

/*******************************************************************************
   Send Answer Operation Functions
*******************************************************************************/
static void static_PHCIServiceSendAnswerCancel(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   static_PHCIServiceCheckOperation(pOperation);

    PNALDFCPost3(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.send_answer.pCallbackFunction,
      pOperation->info.send_answer.pCallbackParameter,
      PNALUtilConvertUint32ToPointer(W_ERROR_CANCEL), 0);
}

static void static_PHCIServiceSendAnswerWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;

   static_PHCIServiceCheckOperation(pOperation);

   if ( pOperation->nState != P_HCI_OPERATION_STATE_CANCELLED )
   {
      CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_WRITE_PENDING );

      /* Remove from the operation list and set the state to completed */
      static_PHCISetOperationCompleted(pOperation);

      /* Send the result */
      PNALDFCPost3(
         pBindingContext, P_DFC_TYPE_HCI_SERVICE,
         pOperation->info.send_answer.pCallbackFunction,
         pOperation->info.send_answer.pCallbackParameter,
         W_SUCCESS, nReceptionCounter);
   }

   /* Process the next pending operation, if any */
   static_PHCIServiceExecuteNextOperation(pBindingContext, null);
}

static void static_PHCIServiceSendAnswerFrameGetData(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pDestinationBuffer,
         uint32_t nMessageOffset,
         uint32_t nLength )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;
   CNALDebugAssert(nLength != 0);

   static_PHCIServiceCheckOperation(pOperation);

   /* Check that the input buffer is in range */
   CNALDebugAssert(nMessageOffset + nLength <= pOperation->info.send_answer.nInputBufferLength + 1);

   if(nMessageOffset == 0)
   {
      *pDestinationBuffer++ = ETSI_MSG_TYPE_ANS | pOperation->info.send_answer.nAnswerCode;

      CNALMemoryCopy(pDestinationBuffer, pOperation->info.send_answer.pInputBuffer, nLength - 1);
   }
   else
   {
      CNALMemoryCopy(pDestinationBuffer,
         &pOperation->info.send_answer.pInputBuffer[nMessageOffset - 1], nLength);
   }
}

static void static_PHCIServiceSendAnswerStart(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   static_PHCIServiceCheckOperation(pOperation);

   pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

   PHCIFrameWritePrepareContext(
      pBindingContext, &pOperation->sWriteContext,
      pOperation->pService->nPipeIdentifier,
      pOperation->info.send_answer.nInputBufferLength + 1,
      static_PHCIServiceSendAnswerWriteCompleted, pOperation );

   PHCIFrameWriteStream( pBindingContext, &pOperation->sWriteContext,
         static_PHCIServiceSendAnswerFrameGetData, pOperation );
}

static tHCIServiceOperationType P_HCI_SERVICE_SEND_ANSWER_OPERATION =
{
   static_PHCIServiceSendAnswerStart,
   static_PHCIServiceSendAnswerCancel,
   null,
};

/*******************************************************************************
   Execute Operation
*******************************************************************************/

static void static_PHCIServiceExecuteCancel(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   static_PHCIServiceCheckOperation(pOperation);

    PNALDFCPost6(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.execute.pCallbackFunction,
      pOperation->info.execute.pCallbackParameter,
      pOperation->info.execute.pOutputBuffer,
      0,
      W_ERROR_CANCEL,
      ETSI_ERR_ANY_OK,
      0);

   /* operation has been cancelled, no longer wait for its answer */
   pInstance->bWaitingForAnswer = W_FALSE;
}

static void static_PHCIServiceExecuteWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;

   static_PHCIServiceCheckOperation(pOperation);

   if ( pOperation->nState != P_HCI_OPERATION_STATE_CANCELLED )
   {
      CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_WRITE_PENDING );

      pOperation->nState = P_HCI_OPERATION_STATE_READ_PENDING;
   }
   else
   {
      PNALDebugWarning("static_PHCIServiceExecuteWriteCompleted: operation cancelled, ignore");

      /* operation has been cancelled, no longer wait for its answer */
      pInstance->bWaitingForAnswer = W_FALSE;

      /* Process the next pending operation, if any */
      static_PHCIServiceExecuteNextOperation(pBindingContext, null);
   }
}

static void static_PHCIServiceExecuteFrameGetData(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pDestinationBuffer,
         uint32_t nMessageOffset,
         uint32_t nLength )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;
   CNALDebugAssert(nLength != 0);
   static_PHCIServiceCheckOperation(pOperation);

   /* Check that the input buffer is in range */
   CNALDebugAssert(nMessageOffset + nLength <= pOperation->info.execute.nInputBufferLength + 1);

   if(nMessageOffset == 0)
   {
      *pDestinationBuffer++ = ETSI_MSG_TYPE_CMD | pOperation->info.execute.nCommandCode;

      CNALMemoryCopy(pDestinationBuffer, pOperation->info.execute.pInputBuffer, nLength - 1);
   }
   else
   {
      CNALMemoryCopy(pDestinationBuffer,
         &pOperation->info.execute.pInputBuffer[nMessageOffset - 1], nLength);
   }
}

static void static_PHCIServiceExecuteStart(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   static_PHCIServiceCheckOperation(pOperation);

   pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

   PHCIFrameWritePrepareContext(
      pBindingContext, &pOperation->sWriteContext,
      pOperation->pService->nPipeIdentifier,
      pOperation->info.execute.nInputBufferLength + 1,
      static_PHCIServiceExecuteWriteCompleted, pOperation );

   PHCIFrameWriteStream( pBindingContext, &pOperation->sWriteContext,
         static_PHCIServiceExecuteFrameGetData, pOperation );

   /* a command has been sent, we must wait for the answer prior sending a new command */
   pInstance->bWaitingForAnswer = W_TRUE;
}

static void static_PHCIServiceExecuteReadDataReceived(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint32_t nOffset,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)
{
   W_ERROR nError = W_SUCCESS;
   uint8_t nResult;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   static_PHCIServiceCheckOperation(pOperation);

   CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING );
   CNALDebugAssert( nLength > 0 );

   if(nOffset + nLength > pOperation->info.execute.nOutputBufferLengthMax + 1)
   {
      nLength += nOffset;
      PNALDebugWarning("static_PHCIServiceExecuteReadDataReceived: reception buffer too short %d bytes lost", nLength);
      pInstance->nReadBytesLost += nLength;
      pInstance->nReadMessageLostCount++;
      nError = W_ERROR_BUFFER_TOO_SHORT;
      pOperation->info.execute.nOutputBufferLength = 0;
      goto return_result;
   }

   if(nOffset == 0)
   {
      nResult = *pBuffer++;
      CNALDebugAssert((nResult & ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_ANS);
      nResult &= ETSI_MSG_INFO_MASK;

      if((nResult != ETSI_ERR_ANY_OK) && (nResult != ETSI_ERR_ANY_E_TIMEOUT))
      {
         PNALDebugWarning("static_PHCIServiceExecuteReadDataReceived: Error 0x%02X returned by NFCC", nResult);
         if(pOperation->nHCIRetryCount < P_HCI_SERVICE_MAX_RETRY)
         {
            pOperation->nHCIRetryCount++;
            PNALDebugWarning("static_PHCIServiceExecuteReadDataReceived: Maybe a RX/TX error, retry (%d/%d)",
               pOperation->nHCIRetryCount, P_HCI_SERVICE_MAX_RETRY);
            pOperation->pType->pStartFunction(pBindingContext, pOperation);
            return;
         }
      }

      pOperation->info.execute.nResultCode = nResult;

      if(nLength > 1)
      {
         CNALDebugAssert(pOperation->info.execute.pOutputBuffer != null);

         CNALMemoryCopy(
            pOperation->info.execute.pOutputBuffer,
            pBuffer, nLength - 1);
         pOperation->info.execute.nOutputBufferLength = nLength - 1;
      }
   }
   else
   {
      CNALDebugAssert(pOperation->info.execute.pOutputBuffer != null);

      CNALMemoryCopy(
         &pOperation->info.execute.pOutputBuffer[nOffset - 1],
         pBuffer, nLength);
      pOperation->info.execute.nOutputBufferLength += nLength;
   }

   if(nHCIMessageReceptionCounter == 0)
   {
      return;
   }

return_result:

   /* Remove from the operation list and set the state to completed */
   static_PHCISetOperationCompleted(pOperation);

   if(nError == W_SUCCESS)
   {
      nResult = pOperation->info.execute.nResultCode;
   }
   else
   {
      nResult = ETSI_ERR_ANY_OK;
   }

   /* Send the result */
   PNALDFCPost6(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.execute.pCallbackFunction,
      pOperation->info.execute.pCallbackParameter,
      pOperation->info.execute.pOutputBuffer,
      pOperation->info.execute.nOutputBufferLength,
      nError, nResult, nHCIMessageReceptionCounter);

   /* answer has been received, we can send new commands */
   pInstance->bWaitingForAnswer = W_FALSE;

   /* Process the next pending operation, if any */
   static_PHCIServiceExecuteNextOperation(pBindingContext, null);
}

static tHCIServiceOperationType P_HCI_SERVICE_EXECUTE_OPERATION =
{
   static_PHCIServiceExecuteStart,
   static_PHCIServiceExecuteCancel,
   static_PHCIServiceExecuteReadDataReceived,
};

/*******************************************************************************
   Get Properties Operation
*******************************************************************************/

static void static_PHCIServiceGetPropertiesCancel(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   static_PHCIServiceCheckOperation(pOperation);

   PNALDFCPost6(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.get_properties.pCallbackFunction,
      pOperation->info.get_properties.pCallbackParameter,
      pOperation->info.get_properties.pOutputBuffer,
      0,
      W_ERROR_CANCEL,
      ETSI_ERR_ANY_OK, 0);

   /* operation has been cancelled, no longer wait for its answer */
   pInstance->bWaitingForAnswer = W_FALSE;
}

static void static_PHCIServiceGetPropertiesWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;

   static_PHCIServiceCheckOperation(pOperation);

   if ( pOperation->nState != P_HCI_OPERATION_STATE_CANCELLED )
   {
      CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_WRITE_PENDING );

      pOperation->nState = P_HCI_OPERATION_STATE_READ_PENDING;
   }
   else
   {
      PNALDebugWarning("static_PHCIServiceGetPropertiesWriteCompleted: operation cancelled, ignore");

      /* operation has been cancelled, no longer wait for its answer */
      pInstance->bWaitingForAnswer = W_FALSE;

      /* Process the next pending operation, if any */
      static_PHCIServiceExecuteNextOperation(pBindingContext, null);
   }
}

static void static_PHCIServiceGetPropertiesFrameGetData(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pDestinationBuffer,
         uint32_t nMessageOffset,
         uint32_t nLength )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;
   static_PHCIServiceCheckOperation(pOperation);

   /* Check that the input buffer is in range */
   CNALDebugAssert( nLength <= 2 );

   switch ( nMessageOffset )
   {
   case 0:
      *pDestinationBuffer++ = ETSI_MSG_TYPE_CMD | ETSI_CMD_ANY_GET_PARAMETER;
      ++nMessageOffset;
      if ( nLength == 1 )
      {
         break;
      }

   case 1:
      *pDestinationBuffer++ = pOperation->info.get_properties.pInputBuffer[
         pOperation->info.get_properties.nPropertiesIndex ];
      ++nMessageOffset;
      break;

   default:
      CNALDebugAssert( W_FALSE );
      break;
   }
}

static void static_PHCIServiceGetPropertiesStart(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   static_PHCIServiceCheckOperation(pOperation);

   CNALDebugAssert( pOperation->info.get_properties.nInputBufferLength != 0 );
   CNALDebugAssert( pOperation->info.get_properties.pInputBuffer != null );
   CNALDebugAssert( pOperation->info.get_properties.nOutputBufferLengthMax != 0 );
   CNALDebugAssert( pOperation->info.get_properties.pOutputBuffer != null );

   /* Reset Properties index */
   pOperation->info.get_properties.nPropertiesIndex = 0;

   pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

   PHCIFrameWritePrepareContext(
      pBindingContext, &pOperation->sWriteContext,
      pOperation->pService->nPipeIdentifier,
      2,
      static_PHCIServiceGetPropertiesWriteCompleted, pOperation );

   PHCIFrameWriteStream( pBindingContext, &pOperation->sWriteContext,
         static_PHCIServiceGetPropertiesFrameGetData, pOperation );

   /* a command has been sent, we must wait for the answer prior sending a new command */
   pInstance->bWaitingForAnswer = W_TRUE;
}

static void static_PHCIServiceGetPropertiesReadDataReceived(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint32_t nOffset,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)
{
   W_ERROR nError = W_SUCCESS;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   static_PHCIServiceCheckOperation(pOperation);

   CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING );
   CNALDebugAssert( nLength > 0 );

   if( nOffset + nLength - 1 + 2 > ( pOperation->info.get_properties.nOutputBufferLengthMax - pOperation->info.get_properties.nOutputBufferLength ) )
   {
      nLength += nOffset;
      PNALDebugWarning("static_PHCIServiceGetPropetiesReadDataReceived: reception buffer too short %d bytes lost", nLength);
      pInstance->nReadBytesLost += nLength;
      pInstance->nReadMessageLostCount++;
      nError = W_ERROR_BUFFER_TOO_SHORT;
      goto return_result;
   }

   if(nOffset == 0)
   {
      uint8_t nResult = *pBuffer;
      CNALDebugAssert((nResult & ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_ANS);
      nResult &= ETSI_MSG_INFO_MASK;

      if(nResult != ETSI_ERR_ANY_OK)
      {
         nLength += nOffset;
         pInstance->nReadBytesLost += nLength;
         pInstance->nReadMessageLostCount++;

         if ( nResult == ETSI_ERR_ANY_E_REG_PAR_UNKNOWN )
         {
            PNALDebugWarning("static_PHCIServiceGetPropertiesReadDataReceived: Error item not found, %d bytes lost", nLength );
         }
         else if(nResult == ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED)
         {
            PNALDebugWarning("static_PHCIServiceGetPropertiesReadDataReceived: Error function not supported, %d bytes lost", nLength );
         }
         else if(nResult == ETSI_ERR_ANY_E_TIMEOUT)
         {
            PNALDebugWarning("static_PHCIServiceGetPropertiesReadDataReceived: Error timeout, %d bytes lost", nLength );
         }
         else
         {
            PNALDebugWarning("static_PHCIServiceGetPropertiesReadDataReceived: Error protocol 0x%02X returned by NFCC, %d bytes lost", nResult, nLength );
         }

         if(pOperation->nHCIRetryCount < P_HCI_SERVICE_MAX_RETRY)
         {
            pOperation->nHCIRetryCount++;
            PNALDebugWarning("static_PHCIServiceGetPropertiesReadDataReceived: Maybe a RX/TX error, retry (%d/%d)",
               pOperation->nHCIRetryCount, P_HCI_SERVICE_MAX_RETRY);

            pOperation->info.get_properties.nPropertiesIndex--;
            nError = W_SUCCESS;
            goto return_result;
         }

         if ( nResult == ETSI_ERR_ANY_E_REG_PAR_UNKNOWN )
         {
            nError = W_ERROR_ITEM_NOT_FOUND;
         }
         else if(nResult == ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED)
         {
            nError = W_ERROR_FUNCTION_NOT_SUPPORTED;
         }
         else if(nResult == ETSI_ERR_ANY_E_TIMEOUT)
         {
            nError = W_ERROR_TIMEOUT;
         }
          else
         {
            nError = W_ERROR_NFCC_COMMUNICATION;
         }

         goto return_result;
      }

      if(nLength >= 1)
      {
         /* Store Property Id */
         pOperation->info.get_properties.pOutputBuffer[ pOperation->info.get_properties.nOutputBufferLength ] =
            pOperation->info.get_properties.pInputBuffer[ pOperation->info.get_properties.nPropertiesIndex ];

         /* Copy data (without answer byte) after property Id and length */
         CNALMemoryCopy(
            &pOperation->info.get_properties.pOutputBuffer[ pOperation->info.get_properties.nOutputBufferLength + 2 + nOffset ],
            &pBuffer[1], nLength - 1);
      }
   }
   else
   {
      /* Copy data */
      CNALMemoryCopy(
         &pOperation->info.get_properties.pOutputBuffer[ pOperation->info.get_properties.nOutputBufferLength + 2 + nOffset - 1 ],
         pBuffer, nLength);
   }

   if( nHCIMessageReceptionCounter == 0 )
   {
      return;
   }

   /* Store property final length */
   pOperation->info.get_properties.pOutputBuffer[ pOperation->info.get_properties.nOutputBufferLength + 1 ] =
      (uint8_t)( nOffset + nLength - 1 );

   pOperation->info.get_properties.nOutputBufferLength += 2 + nOffset + nLength - 1;

return_result:

   if(nError == W_SUCCESS)
   {
      /* Chain get properties ? */
      if ( ++pOperation->info.get_properties.nPropertiesIndex < pOperation->info.get_properties.nInputBufferLength )
      {
         pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

         PHCIFrameWritePrepareContext(
            pBindingContext, &pOperation->sWriteContext,
            pOperation->pService->nPipeIdentifier,
            2,
            static_PHCIServiceGetPropertiesWriteCompleted, pOperation );

         PHCIFrameWriteStream( pBindingContext, &pOperation->sWriteContext,
               static_PHCIServiceGetPropertiesFrameGetData, pOperation );

         return;
      }
   }

   /* Remove from the operation list and set the state to completed */
   static_PHCISetOperationCompleted(pOperation);

   /* Send the result */
   PNALDFCPost6(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.get_properties.pCallbackFunction,
      pOperation->info.get_properties.pCallbackParameter,
      pOperation->info.get_properties.pOutputBuffer,
      pOperation->info.get_properties.nOutputBufferLength,
      nError, ETSI_ERR_ANY_OK, nHCIMessageReceptionCounter);

   /* answer has been received, we can send new commands */
   pInstance->bWaitingForAnswer = W_FALSE;

   /* Process the next pending operation, if any */
   static_PHCIServiceExecuteNextOperation(pBindingContext, null);
}

static tHCIServiceOperationType P_HCI_SERVICE_GET_PROPERTIES_OPERATION =
{
   static_PHCIServiceGetPropertiesStart,
   static_PHCIServiceGetPropertiesCancel,
   static_PHCIServiceGetPropertiesReadDataReceived,
};

/*******************************************************************************
   Set Properties Operation
*******************************************************************************/

static void static_PHCIServiceSetPropertiesCancel(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   static_PHCIServiceCheckOperation(pOperation);

   PNALDFCPost6(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.set_properties.pCallbackFunction,
      pOperation->info.set_properties.pCallbackParameter,
      null,
      0,
      W_ERROR_CANCEL,
      ETSI_ERR_ANY_OK,
      0);

   /* operation has been cancelled, no longer wait for its answer */
   pInstance->bWaitingForAnswer = W_FALSE;
}

static void static_PHCIServiceSetPropertiesWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   static_PHCIServiceCheckOperation(pOperation);

   if ( pOperation->nState != P_HCI_OPERATION_STATE_CANCELLED )
   {
      CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_WRITE_PENDING );

      pOperation->nState = P_HCI_OPERATION_STATE_READ_PENDING;
   }
   else
   {
      PNALDebugWarning("static_PHCIServiceSetPropertiesWriteCompleted: operation cancelled, ignore");

      /* operation has been cancelled, no longer wait for its answer */
      pInstance->bWaitingForAnswer = W_FALSE;

      /* Process the next pending operation, if any */
      static_PHCIServiceExecuteNextOperation(pBindingContext, null);
   }
}

static void static_PHCIServiceSetPropertiesFrameGetData(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pDestinationBuffer,
         uint32_t nMessageOffset,
         uint32_t nLength )
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*)pCallbackParameter;
   static_PHCIServiceCheckOperation(pOperation);

   /* Check that the input buffer is in range */
   CNALDebugAssert( nMessageOffset + nLength <=
      pOperation->info.set_properties.nInputBufferLength - pOperation->info.set_properties.nInputBufferOffset );

   switch ( nMessageOffset )
   {
   case 0:
      *pDestinationBuffer++ = ETSI_MSG_TYPE_CMD | ETSI_CMD_ANY_SET_PARAMETER;
      ++nMessageOffset;
      if ( --nLength == 0 )
      {
         break;
      }

   case 1:
      *pDestinationBuffer++ = pOperation->info.set_properties.pInputBuffer[ pOperation->info.set_properties.nInputBufferOffset ];
      ++nMessageOffset;
      if ( --nLength == 0 )
      {
         break;
      }

   default:
      CNALMemoryCopy(
         pDestinationBuffer,
         &pOperation->info.set_properties.pInputBuffer [ pOperation->info.set_properties.nInputBufferOffset + 2 + nMessageOffset - 2 ],
         nLength );
   }
}

static void static_PHCIServiceSetPropertiesStart(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   /* Check TLV format of input buffer */
   uint32_t i = 0;

   static_PHCIServiceCheckOperation(pOperation);

   CNALDebugAssert( pOperation->info.set_properties.pInputBuffer != null );

   for (;;)
   {
      CNALDebugAssert (  i     < pOperation->info.set_properties.nInputBufferLength );
      CNALDebugAssert (( i+1 ) < pOperation->info.set_properties.nInputBufferLength );

      i += pOperation->info.set_properties.pInputBuffer[ i + 1 ] + 2;
      if ( i == pOperation->info.set_properties.nInputBufferLength )
      {
         break;
      }

   }

   /* Reset Properties index & input offset */
   pOperation->info.set_properties.nPropertiesIndex   = 0;
   pOperation->info.set_properties.nInputBufferOffset = 0;
   pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

   PHCIFrameWritePrepareContext(
      pBindingContext, &pOperation->sWriteContext,
      pOperation->pService->nPipeIdentifier,
      2 + pOperation->info.set_properties.pInputBuffer[1],
      static_PHCIServiceSetPropertiesWriteCompleted, pOperation );

   PHCIFrameWriteStream( pBindingContext, &pOperation->sWriteContext,
         static_PHCIServiceSetPropertiesFrameGetData, pOperation );

   /* a command has been sent, we must wait for the answer prior sending a new command */
   pInstance->bWaitingForAnswer = W_TRUE;
}

static void static_PHCIServiceSetPropertiesReadDataReceived(
                  tNALBindingContext* pBindingContext,
                  tHCIServiceOperation* pOperation,
                  uint32_t nOffset,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter)
{
   W_ERROR nError = W_SUCCESS;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   static_PHCIServiceCheckOperation(pOperation);

   CNALDebugAssert( pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING );

   if(nOffset == 0)
   {
      uint8_t nResult = *pBuffer;
      CNALDebugAssert((nResult & ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_ANS);
      nResult &= ETSI_MSG_INFO_MASK;

      if(nResult != ETSI_ERR_ANY_OK)
      {
         nLength += nOffset;
         pInstance->nReadBytesLost += nLength;
         pInstance->nReadMessageLostCount++;

         if ( nResult == ETSI_ERR_ANY_E_REG_PAR_UNKNOWN )
         {
            PNALDebugWarning("static_PHCIServiceSetPropertiesReadDataReceived: Error item not found, %d bytes lost", nLength );
         }
         else if(nResult == ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED)
         {
            PNALDebugWarning("static_PHCIServiceSetPropertiesReadDataReceived: Error function not supported, %d bytes lost", nLength );
         }
         else if(nResult == ETSI_ERR_ANY_E_TIMEOUT)
         {
            PNALDebugWarning("static_PHCIServiceSetPropertiesReadDataReceived: Error timeout, %d bytes lost", nLength );
         }
         else
         {
            PNALDebugWarning("static_PHCIServiceSetPropertiesReadDataReceived: Error protocol 0x%02X returned by NFCC, %d bytes lost", nResult, nLength );
         }

         if(pOperation->nHCIRetryCount < P_HCI_SERVICE_MAX_RETRY)
         {
            pOperation->nHCIRetryCount++;
            PNALDebugWarning("static_PHCIServiceSetPropertiesReadDataReceived: Maybe a RX/TX error, retry (%d/%d)",
               pOperation->nHCIRetryCount, P_HCI_SERVICE_MAX_RETRY);

            nError = W_SUCCESS;
            goto return_result;
         }

         if ( nResult == ETSI_ERR_ANY_E_REG_PAR_UNKNOWN )
         {
            nError = W_ERROR_ITEM_NOT_FOUND;
         }
         else if(nResult == ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED)
         {
            nError = W_ERROR_FUNCTION_NOT_SUPPORTED;
         }
         else if(nResult == ETSI_ERR_ANY_E_TIMEOUT)
         {
            nError = W_ERROR_TIMEOUT;
         }
         else
         {
            nError = W_ERROR_NFCC_COMMUNICATION;
         }

         goto return_result;
      }
   }

   if( nHCIMessageReceptionCounter == 0 )
   {
      return;
   }

   pOperation->info.set_properties.nInputBufferOffset +=
      2 + pOperation->info.set_properties.pInputBuffer[ 1 + pOperation->info.set_properties.nInputBufferOffset ];

return_result:

   if(nError == W_SUCCESS)
   {
      /* Chain get properties ? */
      if ( pOperation->info.set_properties.nInputBufferOffset < pOperation->info.set_properties.nInputBufferLength )
      {
         pOperation->nState = P_HCI_OPERATION_STATE_WRITE_PENDING;

         PHCIFrameWritePrepareContext(
            pBindingContext, &pOperation->sWriteContext,
            pOperation->pService->nPipeIdentifier,
            2 + pOperation->info.set_properties.pInputBuffer[ pOperation->info.set_properties.nInputBufferOffset + 1 ],
            static_PHCIServiceSetPropertiesWriteCompleted, pOperation );

         PHCIFrameWriteStream( pBindingContext, &pOperation->sWriteContext,
               static_PHCIServiceSetPropertiesFrameGetData, pOperation );

         return;
      }
   }

   /* Remove from the operation list and set the state to completed */
   static_PHCISetOperationCompleted(pOperation);

   /* Send the result */
   PNALDFCPost6(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pOperation->info.set_properties.pCallbackFunction,
      pOperation->info.set_properties.pCallbackParameter,
      null,
      0,
      nError, ETSI_ERR_ANY_OK, nHCIMessageReceptionCounter);

   /* answer has been received, we can send new commands */
   pInstance->bWaitingForAnswer = W_FALSE;

   /* Process the next pending operation, if any */
   static_PHCIServiceExecuteNextOperation(pBindingContext, null);
}

static tHCIServiceOperationType P_HCI_SERVICE_SET_PROPERTIES_OPERATION =
{
   static_PHCIServiceSetPropertiesStart,
   static_PHCIServiceSetPropertiesCancel,
   static_PHCIServiceSetPropertiesReadDataReceived,
};

/*******************************************************************************
   Other Static Functions
*******************************************************************************/

/**
 *Restart the Stand by Timer for a BindingContext
 *
 *@param[in] pBindingContext Current context
**/
static void static_PHCIServiceResetStandByTimer(tNALBindingContext* pBindingContext){
   if(pBindingContext->nAutoStandbyTimeout != 0)
   {
      /* AUTO STANDBY feature is enabled */

      if (pBindingContext->bIsLowPowerRequested != W_FALSE)
      {
         PNALMultiTimerSet(pBindingContext, TIMER_T8_HCI_INACTIVITY, pBindingContext->nStandbyTimeout, static_PHCIServiceInactivityTimeoutElapsed, null);
      }
      else
      {
         PNALMultiTimerSet(pBindingContext, TIMER_T8_HCI_INACTIVITY, pBindingContext->nAutoStandbyTimeout, static_PHCIServiceInactivityTimeoutElapsed, null);
      }
   }
   else
   {
      if (pBindingContext->bIsLowPowerRequested != W_FALSE)
      {
         PNALMultiTimerSet(pBindingContext, TIMER_T8_HCI_INACTIVITY, pBindingContext->nStandbyTimeout, static_PHCIServiceInactivityTimeoutElapsed, null);
      }
      else
      {
         PNALMultiTimerCancel(pBindingContext, TIMER_T8_HCI_INACTIVITY);
      }
   }
}

static const uint8_t g_aAnswerAnyOk[] = { ETSI_MSG_TYPE_ANS | ETSI_ERR_ANY_OK };

static void static_PHCIServiceSendAnyOkCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   tHCIServiceContext* pService = (tHCIServiceContext*)pCallbackParameter;

   PNALDebugTrace("static_PHCIServiceSendAnyOkCompleted: automatic answer ANY_OK sent");

   pService->nSendAnyOkState = 0;
}

static void static_PHCIServiceFrameReadDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nOffset,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter)
{
   tHCIServiceContext* pService = (tHCIServiceContext*)pCallbackParameter;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceOperation* pOperation = pService->pCurrentReadOperation;

   PNALDebugTrace("static_PHCIServiceFrameReadDataReceived");

   if (pBindingContext->bRawMode == W_FALSE)
   {
      static_PHCIServiceResetStandByTimer(pBindingContext);

      /* Restart the HCI inactivity timeout */
      if (nOffset == 0)
      {
         /* receiving a new frame */
         pInstance->nCurrentFrameType = pBuffer[0] & ETSI_MSG_TYPE_MASK;
      }

      switch (pBindingContext->nHCIMode)
      {
         case W_NFCC_MODE_STANDBY :
         case W_NFCC_MODE_BOOT_PENDING :
         case W_NFCC_MODE_SWITCH_TO_STANDBY :
         case W_NFCC_MODE_SWITCH_TO_ACTIVE :

            /* Command and events received during this state are queued for later processing,
               answers are processed as usual */

            PNALDebugTrace("nHCIMessageReceptionCounter %d", nHCIMessageReceptionCounter);

            if ( (pInstance->nCurrentFrameType == ETSI_MSG_TYPE_EVT) ||
                 (pInstance->nCurrentFrameType == ETSI_MSG_TYPE_CMD))
            {
               tHCIServiceReceivedFrameDescriptor * psItem, * psLastItem;

               PNALDebugTrace("received event / command during state transition - queued");

               /* In these modes, the events received from the NFCC are put in a receive queue for later processing */

               psItem = CNALMemoryAlloc(sizeof(tHCIServiceReceivedFrameDescriptor));

               if (psItem != null)
               {
                  psItem->pBuffer = CNALMemoryAlloc(nLength);

                  if (psItem->pBuffer == null)
                  {
                     CNALMemoryFree(psItem);
                     psItem = null;
                  }
               }

               if (psItem == null)
               {
                  /* no memory, drop this packet */
                  PNALDebugWarning("static_PHCIServiceFrameReadDataReceived %d bytes lost", nLength);
                  pInstance->nReadBytesLost += nLength;
                  pInstance->nReadMessageLostCount++;
                  return;
               }

               /* initializes the received frame descriptor */

               psItem->nHCIMessageReceptionCounter = nHCIMessageReceptionCounter;
               psItem->nLength = nLength;
               psItem->nOffset = nOffset;
               CNALMemoryCopy(psItem->pBuffer, pBuffer, nLength);
               psItem->pCallbackParameter = pCallbackParameter;
               psItem->pBindingContext = pBindingContext;
               psItem->pNext = null;

               /* and queue it in the instance context */

               if (pInstance->pReceivedFrameListHead == null)
               {
                  pInstance->pReceivedFrameListHead = psItem;
               }
               else
               {
                  psLastItem = pInstance->pReceivedFrameListHead;

                  while (psLastItem->pNext != null)
                  {
                     psLastItem = psLastItem->pNext;
                  }

                  psLastItem->pNext = psItem;
               }

               /* no more processing */

               return;
            }

            break;
      }

      if(nOffset == 0)
      {
         CNALDebugAssert(pOperation == null);

         /* Set pOperation */
         CNALDebugAssert(nLength >= 1);
         if((pBuffer[0] & ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_ANS)
         {
            for(  pOperation = pService->pOperationListHead;
                  pOperation != null;
                  pOperation = pOperation->pNext)
            {
               static_PHCIServiceCheckOperation(pOperation);

               if((pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING)&&
                 (pOperation->pType != &P_HCI_SERVICE_RECV_EVENT_OPERATION)&&
                 (pOperation->pType != &P_HCI_SERVICE_RECV_CMD_OPERATION))
               {
                  break;
               }
            }
         }
         else if((pBuffer[0] & ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_EVT)
         {
            for(  pOperation = pService->pOperationListHead;
                  pOperation != null;
                  pOperation = pOperation->pNext)
            {
               static_PHCIServiceCheckOperation(pOperation);

               if((pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING)
               && (pOperation->pType == &P_HCI_SERVICE_RECV_EVENT_OPERATION))
               {
                  break;
               }
            }
         }
         else if((pBuffer[0] & ETSI_MSG_TYPE_MASK) == ETSI_MSG_TYPE_CMD)
         {
            bool_t bAutomaticAnswer = W_TRUE;

            for(  pOperation = pService->pOperationListHead;
                  pOperation != null;
                  pOperation = pOperation->pNext)
            {
               static_PHCIServiceCheckOperation(pOperation);

               if((pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING)
               && (pOperation->pType == &P_HCI_SERVICE_RECV_CMD_OPERATION))
               {
                  bAutomaticAnswer = pOperation->info.recv_cmd.bAutomaticAnswer;
                  break;
               }
            }
            if((nHCIMessageReceptionCounter != 0)&&(bAutomaticAnswer != W_FALSE))
            {
               if(pService->nSendAnyOkState == 0)
               {
                  pService->nSendAnyOkState = 1;

                  PHCIFrameWritePrepareContext(
                     pBindingContext, &pService->sWriteAnyOkContext,
                     pService->nPipeIdentifier,
                     1,
                     static_PHCIServiceSendAnyOkCompleted, pService );

                  PHCIFrameWrite( pBindingContext, &pService->sWriteAnyOkContext, (uint8_t*)g_aAnswerAnyOk );

                  PNALDebugTrace("static_PHCIServiceFrameReadDataReceived automatic answer ANY_OK");
               }
            }
         }

         if(nHCIMessageReceptionCounter == 0)
         {
            pService->pCurrentReadOperation = pOperation;
         }
      }
      else if(nHCIMessageReceptionCounter != 0)
      {
         pService->pCurrentReadOperation = null;
      }

      if((pOperation == null)
      || (pOperation->nState != P_HCI_OPERATION_STATE_READ_PENDING))
      {
         PNALDebugWarning("static_PHCIServiceFrameReadDataReceived %d bytes lost", nLength);
         pInstance->nReadBytesLost += nLength;
         pInstance->nReadMessageLostCount++;
      }
      else
      {
         static_PHCIServiceCheckOperation(pOperation);

         pOperation->pType->pReadFunction(pBindingContext, pOperation,
                     nOffset, pBuffer, nLength, nHCIMessageReceptionCounter);
      }
   }
   else
   {
      if (nOffset == 0)
      {
         pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;
         pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_RAW_MESSAGE;
         pBindingContext->pNALReceptionBuffer[2] = pBuffer[-1];
         pBindingContext->nNALReceptionBufferLength = 3;
      }

      CNALMemoryCopy(pBindingContext->pNALReceptionBuffer + pBindingContext->nNALReceptionBufferLength, pBuffer, nLength);

      pBindingContext->nNALReceptionBufferLength += nLength;

      if (nHCIMessageReceptionCounter != 0)
      {
         /* we received a complete HCI frame */
         PNALBindingCallReadCallback(pBindingContext, pBindingContext->nNALReceptionBufferLength, nHCIMessageReceptionCounter);
      }
   }
}

/*******************************************************************************
   Functions
*******************************************************************************/

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceCreate(
         tHCIServiceInstance* pInstance )
{
   uint32_t nServiceIdentifier;

   CNALMemoryFill(pInstance, 0, sizeof(tHCIServiceInstance));
   pInstance->nState = P_HCI_STATE_DISCONNECTED;
   /* pInstance->nLock = P_HCI_UNLOCK; */

   for(nServiceIdentifier = 0; nServiceIdentifier <= P_HCI_SERVICE_MAX_ID; nServiceIdentifier++)
   {
      pInstance->aServiceList[nServiceIdentifier].nState = P_HCI_SERVICE_STATE_CLOSED;
      pInstance->aServiceList[nServiceIdentifier].nPipeIdentifier = (uint8_t)(g_aServicePipe[nServiceIdentifier] & 0x007F);
   }
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceDestroy(
         tHCIServiceInstance* pInstance )
{
   static_PHCIServiceResetInstance(null, pInstance);
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServicePreReset(
         tNALBindingContext* pBindingContext )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   /* Pre-reset HCI instance */
   PHCIFramePreReset( pBindingContext );

   /* Flush DFC Queue */
   PNALDFCFlush( pBindingContext, P_DFC_TYPE_HCI_SERVICE );

   /* Reset the instance context */
   static_PHCIServiceResetInstance(pBindingContext, pInstance);
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceConnect(
         tNALBindingContext* pBindingContext,
         tPNALGenericCompletion* pCallbackFunction,
         void*                         pCallbackParameter )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   static_PHCIServiceResetInstance(pBindingContext, pInstance);

   pInstance->nState = P_HCI_STATE_CONNECTED_PENDING;

   pInstance->pCallbackFunction  = pCallbackFunction;
   pInstance->pCallbackParameter = pCallbackParameter;

   PHCIFrameConnect(
      pBindingContext,
      &static_PHCIServiceFrameConnectCompleted, null,
      &static_PHCIServiceFrameReconnectIndication, null);
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceOpen(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tPHCIServiceOpenCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t bIsSpecialModeCommand)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceOperation* pOperation;
   tHCIServiceContext* pServiceContext;
   W_ERROR nError;

   /* Check Service Id is not already open */
   CNALDebugAssert(nServiceIdentifier <= P_HCI_SERVICE_MAX_ID);
   pServiceContext = &pInstance->aServiceList[nServiceIdentifier];
   CNALDebugAssert(pServiceContext->nPipeIdentifier <=0x7F);
   /* Check hci state is not connected */
   if(pInstance->nState != P_HCI_STATE_CONNECTED)
   {
      PNALDebugError("PHCIServiceOpen: The instance is not open");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   if(( pServiceContext->nState != P_HCI_SERVICE_STATE_CLOSED ) && ( pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN ))
   {
      PNALDebugError("PHCIServiceOpen: Invalid service #%d already open", nServiceIdentifier);
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   pServiceContext->nState = P_HCI_SERVICE_STATE_OPEN_PENDING;

   /**
    * Initiate the open pipe operation
    */
   /* Add the opration in the list */
   pOperation = &pServiceContext->sInnerOperation;
   pOperation->pNext = pServiceContext->pOperationListHead;
   pServiceContext->pOperationListHead = pOperation;
   pOperation->pService = pServiceContext;
   pOperation->nHCIRetryCount = 0;
   pOperation->pType = &P_HCI_SERVICE_OPEN_OPERATION;
   pOperation->info.open.pCallbackFunction = pCallbackFunction;
   pOperation->info.open.pCallbackParameter = pCallbackParameter;
   pOperation->bIsSpecialModeCommand = bIsSpecialModeCommand;

   static_PHCIServiceCheckOperation(pOperation);

   static_PHCIServiceExecuteNextOperation( pBindingContext, pOperation );

   return;

return_error:

   PNALDFCPost3(
      pBindingContext, P_DFC_TYPE_HCI_SERVICE,
      pCallbackFunction, pCallbackParameter,
      pServiceContext, nError);
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceClose(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tPNALGenericCompletion* pCallbackFunction,
         void* pCallbackParameter,
         bool_t bIsSpecialModeCommand)
{
   tHCIServiceContext* pServiceContext;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceOperation* pOperation;

   /* Check hci state is not connected */
   if(pInstance->nState != P_HCI_STATE_CONNECTED)
   {
      PNALDebugError("PHCIServiceClose: The instance is not open");
      return;
   }

   CNALDebugAssert(nServiceIdentifier <= P_HCI_SERVICE_MAX_ID);
   pServiceContext = &pInstance->aServiceList[nServiceIdentifier];

   switch(pServiceContext->nState)
   {
   case P_HCI_SERVICE_STATE_CLOSED:
      PNALDebugWarning("PHCIServiceClose: The service is already closed");
      CNALDebugAssert(pServiceContext->pOperationListHead == null);
      return;
   case P_HCI_SERVICE_STATE_OPEN_INACTIVE:
      PNALDebugError("PHCIServiceClose: This service cannot be closed!");
      return;
   case P_HCI_SERVICE_STATE_CLOSE_PENDING:
      PNALDebugError("PHCIServiceClose: This service is already closing!");
      return;
   case P_HCI_SERVICE_STATE_OPEN:
      /* Cancel every operation for this service */
      static_PHCIServiceCancelAll(pBindingContext, pServiceContext);
      break;

   case P_HCI_SERVICE_STATE_OPEN_PENDING:
      PNALDebugWarning("PHCIServiceClose: This service is opening, register a delayed close operation");
      /* Only one operation should be pending: The open operation */
      CNALDebugAssert(pServiceContext->pOperationListHead == &pServiceContext->sInnerOperation);
      CNALDebugAssert(pServiceContext->pOperationListHead->pNext == null);

      /* Store the close callback info in the open structure */
      pServiceContext->sInnerOperation.info.open.pCallbackParameter = pCallbackParameter;
      pServiceContext->sInnerOperation.info.open.pCallbackFunction = (tPHCIServiceOpenCompleted*)pCallbackFunction;

      /* The close operation will be automatically executed when the open callback is received */
      pServiceContext->nState = P_HCI_SERVICE_STATE_CLOSE_PENDING;
      return;
   }

   pServiceContext->nState = P_HCI_SERVICE_STATE_CLOSE_PENDING;

   /**
    * Initiate the close pipe operation
    */
   /* Add the opration in the list */
   pOperation = &pServiceContext->sInnerOperation;
   pOperation->pNext = pServiceContext->pOperationListHead;
   pServiceContext->pOperationListHead = pOperation;
   pOperation->pService = pServiceContext;
   pOperation->nHCIRetryCount = 0;
   pOperation->pType = &P_HCI_SERVICE_CLOSE_OPERATION;
   pOperation->info.close.pCallbackFunction = pCallbackFunction;
   pOperation->info.close.pCallbackParameter = pCallbackParameter;
   pOperation->bIsSpecialModeCommand = bIsSpecialModeCommand;

   static_PHCIServiceCheckOperation(pOperation);

   static_PHCIServiceExecuteNextOperation( pBindingContext, pOperation );
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceExecuteCommand(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tHCIServiceOperation* pOperation,
         uint8_t nCommandCode,
         const uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         uint8_t* pOutputBuffer,
         uint32_t nOutputBufferLength,
         tPHCIServiceExecuteCommandCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t  bIsSpecialModeCommand)
{
   tHCIServiceContext* pServiceContext;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   /* Check hci state is not connected */
   if(pInstance->nState != P_HCI_STATE_CONNECTED)
   {
      PNALDebugError("PHCIServiceExecuteCommand: The instance is not open");
      return;
   }

   CNALDebugAssert(nServiceIdentifier <= P_HCI_SERVICE_MAX_ID);
   pServiceContext = &pInstance->aServiceList[nServiceIdentifier];
   CNALDebugAssert(pServiceContext->nPipeIdentifier <=0x7F);
   /* Check the service state */
   if((pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN)
   && (pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN_INACTIVE))
   {
      if((pServiceContext->nState != P_HCI_SERVICE_STATE_CLOSED)
      || ((g_aServicePipe[nServiceIdentifier] & 0x2000) == 0))
      {
         PNALDebugError("PHCIServiceExecuteCommand: The service is not open");
         return;
      }
   }

   /**
    * Initiate the execute operation
    */

   /* check the operation is not already in the service */
   {
      tHCIServiceOperation * pCurrentOperation;

      pCurrentOperation = pServiceContext->pOperationListHead;

      while (pCurrentOperation != null)
      {
         CNALDebugAssert(pCurrentOperation != pOperation);
         pCurrentOperation = pCurrentOperation->pNext;
      }
   }

   /* Add the operation in the list */
   pOperation->pNext = pServiceContext->pOperationListHead;
   pServiceContext->pOperationListHead = pOperation;
   pOperation->pService = pServiceContext;
   pOperation->nHCIRetryCount = 0;
   pOperation->bIsSpecialModeCommand = bIsSpecialModeCommand;

   if(nCommandCode == P_HCI_SERVICE_CMD_GET_PROPERTIES)
   {
      pOperation->pType = &P_HCI_SERVICE_GET_PROPERTIES_OPERATION;
      pOperation->info.get_properties.pCallbackFunction = pCallbackFunction;
      pOperation->info.get_properties.pCallbackParameter = pCallbackParameter;
      pOperation->info.get_properties.pInputBuffer = pInputBuffer;
      pOperation->info.get_properties.nInputBufferLength = nInputBufferLength;
      pOperation->info.get_properties.pOutputBuffer = pOutputBuffer;
      pOperation->info.get_properties.nOutputBufferLengthMax = nOutputBufferLength;
      pOperation->info.get_properties.nOutputBufferLength = 0;
   }
   else if(nCommandCode == P_HCI_SERVICE_CMD_SET_PROPERTIES)
   {
      pOperation->pType = &P_HCI_SERVICE_SET_PROPERTIES_OPERATION;
      pOperation->info.set_properties.pCallbackFunction = pCallbackFunction;
      pOperation->info.set_properties.pCallbackParameter = pCallbackParameter;
      pOperation->info.set_properties.pInputBuffer = pInputBuffer;
      pOperation->info.set_properties.nInputBufferLength = nInputBufferLength;
   }
   else
   {
      pOperation->pType = &P_HCI_SERVICE_EXECUTE_OPERATION;
      pOperation->info.execute.pCallbackFunction = pCallbackFunction;
      pOperation->info.execute.pCallbackParameter = pCallbackParameter;
      pOperation->info.execute.nCommandCode = nCommandCode;
      pOperation->info.execute.pInputBuffer = pInputBuffer;
      pOperation->info.execute.nInputBufferLength = nInputBufferLength;
      pOperation->info.execute.pOutputBuffer = pOutputBuffer;
      pOperation->info.execute.nOutputBufferLengthMax = nOutputBufferLength;
      pOperation->info.execute.nOutputBufferLength = 0;
   }

   static_PHCIServiceCheckOperation(pOperation);

   static_PHCIServiceExecuteNextOperation( pBindingContext, pOperation );
}

static void static_PHCIServiceExecuteNextMultiServiceCommand(
         tNALBindingContext* pBindingContext,
         tHCIServiceOperation* pOperation);

static void static_PHCIServiceExecuteMultiServiceCommandCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   tHCIServiceOperation* pOperation = (tHCIServiceOperation*) pCallbackParameter;

   if ((nError == W_SUCCESS) && (nStatusCode == ETSI_ERR_ANY_OK))
   {
      /* update the length field in the output buffer */
      pOperation->sMultiService.pOutputBuffer[pOperation->sMultiService.nOutputBufferIndex++] = (uint8_t) pOperation->sMultiService.nCurrentService;
      pOperation->sMultiService.pOutputBuffer[pOperation->sMultiService.nOutputBufferIndex++] = (uint8_t) nLength;

      /* ensure the content of the received data is at the correct place in the output buffer */
      if (pBuffer != pOperation->sMultiService.pOutputBuffer + pOperation->sMultiService.nOutputBufferIndex)
      {
         CNALMemoryMove(pOperation->sMultiService.pOutputBuffer + pOperation->sMultiService.nOutputBufferIndex, pBuffer, nLength);
      }

      /* update the output buffer length */
      pOperation->sMultiService.nOutputBufferIndex += nLength;

      /* execute the next operation */
      if (pOperation->sMultiService.nInputBufferIndex < pOperation->sMultiService.nInputBufferLength)
      {
         static_PHCIServiceExecuteNextMultiServiceCommand(pBindingContext, pOperation);
         return;
      }
   }

   /* send the result */
   PNALDFCPost6(
         pBindingContext, P_DFC_TYPE_HCI_SERVICE,
         pOperation->sMultiService.pCallbackFunction,
         pOperation->sMultiService.pCallbackParameter,
         pOperation->sMultiService.pOutputBuffer,
         pOperation->sMultiService.nOutputBufferIndex,
         nError,
         nStatusCode,
         nHCIMessageReceptionCounter);
}

static void static_PHCIServiceExecuteNextMultiServiceCommand(
         tNALBindingContext* pBindingContext,
         tHCIServiceOperation* pOperation)
{
   uint8_t nService;
   uint32_t nLength;
   uint32_t nIndex;

   nService = pOperation->sMultiService.pInputBuffer[pOperation->sMultiService.nInputBufferIndex++];
   nLength  = pOperation->sMultiService.pInputBuffer[pOperation->sMultiService.nInputBufferIndex++];
   nIndex = pOperation->sMultiService.nInputBufferIndex;
   pOperation->sMultiService.nInputBufferIndex += nLength;
   pOperation->sMultiService.nCurrentService = nService;

   PHCIServiceExecuteCommand(pBindingContext, nService, pOperation,
      pOperation->sMultiService.nCommandCode,
      pOperation->sMultiService.pInputBuffer + nIndex,
      nLength,
      pOperation->sMultiService.pOutputBuffer + pOperation->sMultiService.nOutputBufferIndex + 2,           /* keep room for Service/length before payload */
      pOperation->sMultiService.nOutputBufferLength - pOperation->sMultiService.nOutputBufferIndex - 2 ,
      static_PHCIServiceExecuteMultiServiceCommandCompleted,
      pOperation,
      pOperation->sMultiService.bIsSpecialModeCommand);
}

NFC_HAL_INTERNAL void PHCIServiceExecuteMultiServiceCommand(
         tNALBindingContext* pBindingContext,
         tHCIServiceOperation* pOperation,
         uint8_t nCommandCode,
         const uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         uint8_t* pOutputBuffer,
         uint32_t nOutputBufferLength,
         tPHCIServiceExecuteCommandCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t  bIsSpecialModeCommand)
{
   /* store the current operation */
   pOperation->sMultiService.nCommandCode = nCommandCode;
   pOperation->sMultiService.pInputBuffer = pInputBuffer;
   pOperation->sMultiService.nInputBufferLength = nInputBufferLength;
   pOperation->sMultiService.pOutputBuffer = pOutputBuffer;
   pOperation->sMultiService.nOutputBufferLength = nOutputBufferLength;
   pOperation->sMultiService.pCallbackFunction = pCallbackFunction;
   pOperation->sMultiService.pCallbackParameter = pCallbackParameter;
   pOperation->sMultiService.bIsSpecialModeCommand = bIsSpecialModeCommand;

   pOperation->sMultiService.nInputBufferIndex = 0;
   pOperation->sMultiService.nOutputBufferIndex = 0;

   static_PHCIServiceExecuteNextMultiServiceCommand(pBindingContext, pOperation);
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceRegister(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         uint8_t nRegisterType,
         bool_t bAutomaticAnswer,
         tHCIServiceOperation* pOperation,
         tPHCIServiceDataReceived* pCallbackFunction,
         void* pCallbackParameter,
         bool_t bIsSpecialModeCommand)
{
   tHCIServiceContext* pServiceContext;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   /* Check hci state is not connected */
   if(pInstance->nState != P_HCI_STATE_CONNECTED)
   {
      PNALDebugError("PHCIServiceRegister: The instance is not open");
      return;
   }

   CNALDebugAssert(nServiceIdentifier <= P_HCI_SERVICE_MAX_ID);
   pServiceContext = &pInstance->aServiceList[nServiceIdentifier];

   /* Check the service state */
   if((pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN)
   && (pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN_INACTIVE))
   {
      if((pServiceContext->nState != P_HCI_SERVICE_STATE_CLOSED)
      || ((g_aServicePipe[nServiceIdentifier] & 0x2000) == 0))
      {
         PNALDebugError("PHCIServiceRegister: The service is not open");
         return;
      }
   }

   /* check the operation is not already in the service */
   {
      tHCIServiceOperation * pCurrentOperation;

      pCurrentOperation = pServiceContext->pOperationListHead;

      while (pCurrentOperation != null)
      {
         CNALDebugAssert(pCurrentOperation != pOperation);
         pCurrentOperation = pCurrentOperation->pNext;
      }
   }

   /**
    * Initiate the receive operation
    */
   if(nRegisterType == P_EVT_REGISTER_TYPE)
   {
      pOperation->pType = &P_HCI_SERVICE_RECV_EVENT_OPERATION;
      pOperation->info.recv_event.pCallbackFunction = pCallbackFunction;
      pOperation->info.recv_event.pCallbackParameter = pCallbackParameter;
   }
   else if(nRegisterType == P_CMD_REGISTER_TYPE)
   {
      pOperation->pType = &P_HCI_SERVICE_RECV_CMD_OPERATION;
      pOperation->info.recv_cmd.pCallbackFunction = pCallbackFunction;
      pOperation->info.recv_cmd.pCallbackParameter = pCallbackParameter;
      pOperation->info.recv_cmd.bAutomaticAnswer   = bAutomaticAnswer;
   }
   else
   {
      PNALDebugError("PHCIServiceRegister: The type of register is unknow : 0x%x",nRegisterType);
      return;
   }

   /* Add the opration in the list */
   pOperation->pNext = pServiceContext->pOperationListHead;
   pServiceContext->pOperationListHead = pOperation;
   pOperation->pService = pServiceContext;
   pOperation->nHCIRetryCount = 0;

   pOperation->nState = P_HCI_OPERATION_STATE_READ_PENDING;
   pOperation->pNextPending = null;
   pOperation->bIsSpecialModeCommand = bIsSpecialModeCommand;

   static_PHCIServiceCheckOperation(pOperation);
}
/* See header file */
NFC_HAL_INTERNAL void PHCIServiceCancelOperation(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tHCIServiceOperation* pOperation)
{
   tHCIServiceContext* pServiceContext;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   CNALDebugAssert(nServiceIdentifier <= P_HCI_SERVICE_MAX_ID);

   static_PHCIServiceCheckOperation(pOperation);

   /* Check hci state is not connected */
   if(pInstance->nState != P_HCI_STATE_CONNECTED)
   {
      PNALDebugError("PHCIServiceCancelOperation: The instance is not open");
      return;
   }

   if(pOperation->pService == null)
   {
      return;
   }

   pServiceContext = &pInstance->aServiceList[nServiceIdentifier];
   CNALDebugAssert(pOperation->pService == pServiceContext);

   /* Check the service state */
   if((pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN)
   && (pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN_INACTIVE))
   {
      if((pServiceContext->nState != P_HCI_SERVICE_STATE_CLOSED)
      || ((g_aServicePipe[nServiceIdentifier] & 0x2000) == 0))
      {
         PNALDebugError("PHCIServiceCancelOperation: The service is not open");
         return;
      }
   }

   /* Cancel the operation */
   if((pOperation->nState != P_HCI_OPERATION_STATE_COMPLETED)
   && (pOperation->nState != P_HCI_OPERATION_STATE_CANCELLED))
   {
      if(pOperation->pType->pCancelFunction != null)
      {
         tHCIServiceOperation** ppCurrentOperation;

         pOperation->pType->pCancelFunction(pBindingContext, pOperation);

         if(pOperation->nState == P_HCI_OPERATION_STATE_SCHEDULED)
         {
            /* Remove the operation from the pending list */
            for(ppCurrentOperation = &pInstance->pPendingOperationListHead;
               *ppCurrentOperation != pOperation;
               ppCurrentOperation = &((*ppCurrentOperation)->pNextPending))
            {
               CNALDebugAssert(*ppCurrentOperation != null);
            }
            *ppCurrentOperation = pOperation->pNextPending;
         }

         /* Remove the operation from the operation list */
         for(ppCurrentOperation = &pOperation->pService->pOperationListHead;
            *ppCurrentOperation != pOperation;
            ppCurrentOperation = &((*ppCurrentOperation)->pNext))
         {
            CNALDebugAssert(*ppCurrentOperation != null);
         }

         *ppCurrentOperation = pOperation->pNext;
         CNALDebugAssert(pOperation != pOperation->pNext);
         if(*ppCurrentOperation != null)
         {
            CNALDebugAssert(*ppCurrentOperation != (*ppCurrentOperation)->pNext);
         }

         pOperation->nState = P_HCI_OPERATION_STATE_CANCELLED;
         pOperation->pNext = null;
         pOperation->pNextPending = null;
         pOperation->pService = null;
      }
   }
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceSendEvent(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tHCIServiceOperation* pOperation,
         uint8_t nEventCode,
         uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         tPHCIServiceSendEventCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t bIsSpecialModeCommand)
{
   tHCIServiceContext* pServiceContext;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   /* Check hci state is not connected */
   if(pInstance->nState != P_HCI_STATE_CONNECTED)
   {
      PNALDebugError("PHCIServiceSendEvent: The instance is not open");
      return;
   }

   CNALDebugAssert(nServiceIdentifier <= P_HCI_SERVICE_MAX_ID);
   pServiceContext = &pInstance->aServiceList[nServiceIdentifier];
   CNALDebugAssert(pServiceContext->nPipeIdentifier <=0x7F);
   /* Check the service state */
   if((pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN)
   && (pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN_INACTIVE))
   {
      if((pServiceContext->nState != P_HCI_SERVICE_STATE_CLOSED)
      || ((g_aServicePipe[nServiceIdentifier] & 0x2000) == 0))
      {
         PNALDebugError("PHCIServiceSendEvent: The service is not open");
         return;
      }
   }

   /**
    * Initiate the receive event operation
    */

   /* check the operation is not already in the service */
   {
      tHCIServiceOperation * pCurrentOperation;

      pCurrentOperation = pServiceContext->pOperationListHead;

      while (pCurrentOperation != null)
      {
         CNALDebugAssert(pCurrentOperation != pOperation);
         pCurrentOperation = pCurrentOperation->pNext;
      }
   }

   /* Add the operation in the list */
   pOperation->pNext = pServiceContext->pOperationListHead;
   pServiceContext->pOperationListHead = pOperation;

   pOperation->pService = pServiceContext;
   pOperation->nHCIRetryCount = 0;
   pOperation->bIsSpecialModeCommand = bIsSpecialModeCommand;
   pOperation->pType = &P_HCI_SERVICE_SEND_EVENT_OPERATION;
   pOperation->info.send_event.pCallbackFunction = pCallbackFunction;
   pOperation->info.send_event.pCallbackParameter = pCallbackParameter;
   pOperation->info.send_event.nEventCode = nEventCode;
   pOperation->info.send_event.pInputBuffer = pInputBuffer;
   pOperation->info.send_event.nInputBufferLength = nInputBufferLength;

   static_PHCIServiceCheckOperation(pOperation);

   static_PHCIServiceExecuteNextOperation( pBindingContext, pOperation );
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceSendAnswer(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tHCIServiceOperation* pOperation,
         uint8_t nAnswerCode,
         uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         tPHCIServiceSendAnswerCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t bIsSpecialModeCommand)
{
   tHCIServiceContext* pServiceContext;
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   /* Check hci state is not connected */
   if(pInstance->nState != P_HCI_STATE_CONNECTED)
   {
      PNALDebugError("PHCIServiceSendAnswer: The instance is not open");
      return;
   }

   CNALDebugAssert(nServiceIdentifier <= P_HCI_SERVICE_MAX_ID);
   pServiceContext = &pInstance->aServiceList[nServiceIdentifier];
   CNALDebugAssert(pServiceContext->nPipeIdentifier <=0x7F);

   /* Check the service state */
   if((pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN)
   && (pServiceContext->nState != P_HCI_SERVICE_STATE_OPEN_INACTIVE))
   {
      if((pServiceContext->nState != P_HCI_SERVICE_STATE_CLOSED)
      || ((g_aServicePipe[nServiceIdentifier] & 0x2000) == 0))
      {
         PNALDebugError("PHCIServiceSendAnswer: The service is not open");
         return;
      }
   }

   /**
    * Initiate the receive answer operation
    */

   /* check the operation is not already in the service */
   {
      tHCIServiceOperation * pCurrentOperation;

      pCurrentOperation = pServiceContext->pOperationListHead;

      while (pCurrentOperation != null)
      {
         CNALDebugAssert(pCurrentOperation != pOperation);
         pCurrentOperation = pCurrentOperation->pNext;
      }
   }

   /* Add the operation in the list */
   pOperation->pNext = pServiceContext->pOperationListHead;
   pServiceContext->pOperationListHead = pOperation;
   pOperation->pService = pServiceContext;
   pOperation->nHCIRetryCount = 0;
   pOperation->bIsSpecialModeCommand = bIsSpecialModeCommand;
   pOperation->pType = &P_HCI_SERVICE_SEND_ANSWER_OPERATION;
   pOperation->info.send_answer.pCallbackFunction = pCallbackFunction;
   pOperation->info.send_answer.pCallbackParameter = pCallbackParameter;
   pOperation->info.send_answer.nAnswerCode = nAnswerCode;
   pOperation->info.send_answer.pInputBuffer = pInputBuffer;
   pOperation->info.send_answer.nInputBufferLength = nInputBufferLength;

   static_PHCIServiceCheckOperation(pOperation);

   static_PHCIServiceExecuteNextOperation( pBindingContext, pOperation );
}

NFC_HAL_INTERNAL void PHCIServiceKick(
         tNALBindingContext * pBindingContext)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceReceivedFrameDescriptor * psItem;

   PNALDebugTrace("processing received event / command during state transition");

   while ((psItem = pInstance->pReceivedFrameListHead) != null)
   {
      pInstance->pReceivedFrameListHead = pInstance->pReceivedFrameListHead->pNext;

      static_PHCIServiceFrameReadDataReceived(
            psItem->pBindingContext, psItem->pCallbackParameter, psItem->pBuffer, psItem->nOffset, psItem->nLength, psItem->nHCIMessageReceptionCounter);

      psItem->pNext = pInstance->pProcessedFrameListHead;
      pInstance->pProcessedFrameListHead = psItem;
   }

   PNALDebugTrace("process next HCI service operation");

   static_PHCIServiceExecuteNextOperation( pBindingContext, null );
   static_PHCIServiceResetStandByTimer(pBindingContext);
}

NFC_HAL_INTERNAL void PHCIServiceFlushProcessedFrames(
      tNALBindingContext * pBindingContext)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );
   tHCIServiceReceivedFrameDescriptor * psItem;

   if (pInstance->pProcessedFrameListHead != null)
   {
      PNALDebugError("Flushing processed frames");
   }

   while ((psItem = pInstance->pProcessedFrameListHead) != null)
   {
      pInstance->pProcessedFrameListHead = pInstance->pProcessedFrameListHead->pNext;

      if (psItem->pBuffer != null)
      {
         CNALMemoryFree(psItem->pBuffer);
      }

      CNALMemoryFree(psItem);
   }
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceGetStatistics(
         tNALBindingContext* pBindingContext,
         uint32_t* pnOSI6ReadMessageLost,
         uint32_t* pnOSI6ReadByteErrorCount )
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   *pnOSI6ReadByteErrorCount  = pInstance->nReadBytesLost;
   *pnOSI6ReadMessageLost = pInstance->nReadMessageLostCount;
}

/* See header file */
NFC_HAL_INTERNAL void PHCIServiceResetStatistics(
         tNALBindingContext* pBindingContext)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance( pBindingContext );

   pInstance->nReadBytesLost    = 0;
   pInstance->nReadMessageLostCount = 0;
}

static bool_t static_PHCIServiceSendManagementEvent(
   tNALBindingContext       * pBindingContext,
   const uint8_t  * pBuffer,
   uint32_t         nLength)
{
   tHCIServiceInstance* pInstance = PNALContextGetHCIServiceInstance(pBindingContext);
   tHCIServiceContext* pService = &pInstance->aServiceList[P_HCI_SERVICE_PIPE_MANAGEMENT];
   tHCIServiceOperation* pOperation;

   PNALDebugTrace("static_PHCIServiceSendManagementEvent");

   /* Look for an operation waiting for events on P_HCI_SERVICE_PIPE_MANAGEMENT */

   for(  pOperation = pService->pOperationListHead;
               pOperation != null;
               pOperation = pOperation->pNext)
   {
      static_PHCIServiceCheckOperation(pOperation);

      if(   (pOperation->nState == P_HCI_OPERATION_STATE_READ_PENDING)
         && (pOperation->pType == &P_HCI_SERVICE_RECV_EVENT_OPERATION))
      {
         break;
      }
   }

   if (pOperation != null)
   {
      static_PHCIServiceCheckOperation(pOperation);

      pOperation->pType->pReadFunction(pBindingContext, pOperation, 0, (uint8_t *) pBuffer, nLength, 0);

      return (W_TRUE);
   }

   return (W_FALSE);
}

/**
  * HCI inactivity timer expiry callback.
  */

const uint8_t  g_aHCIInactivityEvent[] = { ETSI_MSG_TYPE_EVT | HCI2_EVT_MGT_HCI_INACTIVITY };

static void static_PHCIServiceInactivityTimeoutElapsed(
      tNALBindingContext * pBindingContext,
      void     * pCallbackParameter)
{
   if (static_PHCIServiceSendManagementEvent(pBindingContext, g_aHCIInactivityEvent, sizeof(g_aHCIInactivityEvent)) == W_FALSE)
   {
      PNALDebugWarning("No listener for inactivity timeout event");
   }
}

/**
  * SHDLC Reconnect indication
  */

const uint8_t  g_aHCIWakeUpEvent[]  = { ETSI_MSG_TYPE_EVT | HCI2_EVT_MGT_HCI_WAKE_UP };

static void static_PHCIServiceFrameReconnectIndication(
         tNALBindingContext* pBindingContext,
         void*     pCallbackParameter )
{
   if (static_PHCIServiceSendManagementEvent(pBindingContext, g_aHCIWakeUpEvent, sizeof(g_aHCIWakeUpEvent)) == W_FALSE)
   {
      PNALDebugWarning("No listener for reconnect indication event");
   }
}

