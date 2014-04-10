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
   Contains the reader registry implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( REG )

#include "wme_context.h"
#include "wme_reader_driver_registry.h"
tGerenalByte pGerenalByte;

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static void static_PReaderSetPulsePeriodDriverCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   PDFCPostContext2(pCallbackParameter, nError);
   CMemoryFree(pCallbackParameter);
}

void PReaderSetPulsePeriod(
      tContext * pContext,
      tPBasicGenericCallbackFunction* pCallback,
      void* pCallbackParameter,
      uint32_t nPulsePeriod )
{
   tDFCCallbackContext * pCallbackContext = CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PReaderSetPulsePeriodDriver(pContext, static_PReaderSetPulsePeriodDriverCompleted, pCallbackContext, nPulsePeriod);

      nError = PContextGetLastIoctlError(pContext);
      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PReaderSetPulsePeriodDriverCompleted, pCallbackContext, &sCallbackContext);
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

#endif /* (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#if P_READER_DETECTION_TIMEOUT < 600
#  undef P_READER_DETECTION_TIMEOUT
#  define P_READER_DETECTION_TIMEOUT  600
#endif

/* The detection states */
#define P_READER_DETECTION_STATE_DETECTION  0x00
#define P_READER_DETECTION_STATE_PENDING    0x01
#define P_READER_DETECTION_STATE_ACTIVE     0x02

/* The detection configuration state */
#define P_READER_CONFIG_STATE_GENERIC           0
#define P_READER_CONFIG_STATE_GENERIC_PENDING   1
#define P_READER_CONFIG_STATE_SPECIFIC          2
#define P_READER_CONFIG_STATE_SPECIFIC_PENDING  3

/* Reader protocols implemented in other files */
extern tPRegistryDriverReaderProtocolConstant g_sP14P3ReaderProtocolConstantTypeA;
extern tPRegistryDriverReaderProtocolConstant g_sP14P3ReaderProtocolConstantTypeB;
extern tPRegistryDriverReaderProtocolConstant g_sP15P3ReaderProtocoConstant;
extern tPRegistryDriverReaderProtocolConstant g_sType1ChipReaderProtocolConstant;
extern tPRegistryDriverReaderProtocolConstant g_sFeliCaReaderProtocolConstant;
extern tPRegistryDriverReaderProtocolConstant g_sP2PInitiatorReaderProtocolConstant;

#ifdef P_READER_14P4_STANDALONE_SUPPORT

   extern tPRegistryDriverReaderProtocolConstant g_sP14P4ReaderProtocolConstantTypeA;
   extern tPRegistryDriverReaderProtocolConstant g_sP14P4ReaderProtocolConstantTypeB;

#endif /* P_READER_14P4_STANDALONE_SUPPORT */

/* Reader B Prime */
extern tPRegistryDriverReaderProtocolConstant g_sPBPrimeReaderProtocolConstant;
extern tPRegistryDriverReaderProtocolConstant g_sPKovioReaderProtocolConstant;

/* Reader protocol contant array */
static tPRegistryDriverReaderProtocolConstant* const g_aReaderProtocolConstantlArray[] =
{
   &g_sFeliCaReaderProtocolConstant,
   &g_sType1ChipReaderProtocolConstant,
   &g_sP15P3ReaderProtocoConstant,
   &g_sP14P3ReaderProtocolConstantTypeA,
   &g_sP14P3ReaderProtocolConstantTypeB,

#ifdef P_READER_14P4_STANDALONE_SUPPORT
   &g_sP14P4ReaderProtocolConstantTypeA,
   &g_sP14P4ReaderProtocolConstantTypeB,
#endif /* P_READER_14P4_STANDALONE_SUPPORT */

   &g_sP2PInitiatorReaderProtocolConstant,
   &g_sPBPrimeReaderProtocolConstant,
   &g_sPKovioReaderProtocolConstant
};

/* The number of reader protocols */
#define P_READER_PROTOCOL_NUMBER  \
      (sizeof(g_aReaderProtocolConstantlArray)/sizeof(tPRegistryDriverReaderProtocolConstant*))

/* Card protocols implemented in other files */
extern tPRegistryDriverCardProtocolConstant g_sEmulCardProtocolConstantTypeA;
extern tPRegistryDriverCardProtocolConstant g_sEmulCardProtocolConstantTypeB;
extern tPRegistryDriverCardProtocolConstant g_sEmulCardProtocolConstantP2PTarget;

/* Card protocol contant array */
static tPRegistryDriverCardProtocolConstant* const g_aCardProtocolConstantlArray[] =
{
   &g_sEmulCardProtocolConstantTypeA,
   &g_sEmulCardProtocolConstantTypeB,
   &g_sEmulCardProtocolConstantP2PTarget
};

/* The number of card protocols */
#define P_CARD_PROTOCOL_NUMBER  \
      (sizeof(g_aCardProtocolConstantlArray)/sizeof(tPRegistryDriverCardProtocolConstant*))

/* Declare a reader listener */
typedef struct __tPRegistryDriverReaderListener
{
   /* Connection registry handle */
   tHandleObjectHeader  sObjectHeader;

   /* The priority of the listener */
   uint8_t  nPriority;

   /* The requested protocols */
   uint32_t nRequestedProtocolsBF;

   /* The protocols for which this handler has been notified */
   uint32_t nNotifiedProtocolsBF;

   /* User response buffer */
   uint8_t*  pBuffer;
   /* User max length response buffer */
   uint32_t  nBufferMaxLength;

   /* reader listener registered from  user or kernel */
   bool_t bFromUser;

   /* Callback information */
   tDFCDriverCCReference pListenerDriverCC;
   tDFCCallbackContext   sListenerCC;

   /* Next listener in the list */
   struct __tPRegistryDriverReaderListener*   pNextDriverListener;
} tPRegistryDriverReaderListener;

/* Declare a reader protocol information */
typedef struct __tPRegistryDriverReaderProtocol
{
   /* Detection Configuration Operation */
   tNALServiceOperation sDetectionConfigurationOperation;
   /* Service Operation */
   tNALServiceOperation sDetectionOperationEvent;
   /* NFC HAL command buffer */
   uint8_t aSetDetectionConfigurationCommandBuffer[NAL_MESSAGE_MAX_LENGTH];
   /* NFC HAL response buffer */
   uint8_t aResponseBuffer[NAL_MESSAGE_MAX_LENGTH];

   /* Detection configuration */
   uint8_t aDetectionConfiguration[NAL_MESSAGE_MAX_LENGTH];
   uint32_t nDetectionConfigurationLength;
   uint32_t nSetDetectionConfigurationState;

   /* The constant information */
   tPRegistryDriverReaderProtocolConstant* pConstant;

   /* the last detected card */
   tPReaderDriverCardInfo sLastCard;

   /* The next element in the list */
   struct __tPRegistryDriverReaderProtocol* pNext;

} tPRegistryDriverReaderProtocol;

/* Declare a card protocol information */
typedef struct __tPRegistryDriverCardProtocol
{
   /* The event operation */
   tNALServiceOperation sOperationEvent;
   tNALServiceOperation sRegisterOperation;

   /* The constant information */
   tPRegistryDriverCardProtocolConstant* pConstant;

   /* The callback parameter */
   void* pCallbackParameter;

   /* The current state */
   bool_t bCardEmulationInUse;

   /* The current configuration state */
   bool_t bCardEmulationConfigured;

   /* The registration callback */
   tDFCCallbackContext sRegisterCallbackContext;

   /* The last parameters */
   uint8_t aLastParameters[NAL_MESSAGE_MAX_LENGTH];
   uint32_t nLastParametersLength;

   /* The next element in the list */
   struct __tPRegistryDriverCardProtocol* pNext;

} tPRegistryDriverCardProtocol;


/* Destroy registry callback */
static uint32_t static_PReaderDriverListenerDestroy(
            tContext* pContext,
            void* pObject );

static bool_t static_PReaderDriverCheckDejaVu(
            tContext* pContext,
            uint32_t nTimeout,
            uint32_t* pnReference);

/* Registry type */
tHandleType g_sReaderListenerRegistry = { static_PReaderDriverListenerDestroy, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_READER_LISTENER_REGISTRY (&g_sReaderListenerRegistry)

/* Destroy error registry callback */
static uint32_t static_PReaderDriverErrorRegistryDestroy(
            tContext* pContext,
            void* pObject );

/* Error registry type */
tHandleType g_sError = { static_PReaderDriverErrorRegistryDestroy, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_ERROR (&g_sError)

/* Destroy connection callback */
static uint32_t static_PReaderDriverConnectionDestroy(
            tContext* pContext,
            void* pObject );

/* Reader connection type */
tHandleType g_sReaderDriverConnection = { static_PReaderDriverConnectionDestroy, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_READER_DRIVER_CONNECTION (&g_sReaderDriverConnection)

static void static_PReaderDriverReactivateDetection(
            tContext* pContext,
            tPReaderDriverRegistry* pReaderDriverRegistry,
            bool_t bStopActiveReader);

static void static_PReaderDriverTargetDetection(
            tContext* pContext,
            tPReaderDriverRegistry* pReaderDriverRegistry,
            tPReaderDriverTargetDetectionMessage* pTargetDetectionMessage );

static tPRegistryDriverReaderListener* static_PReaderDriverGetNextListener(
            tContext* pContext,
            tPReaderDriverRegistry* pReaderDriverRegistry,
            tPReaderDriverCardInfo* pCardInfo);

static void static_PReaderDriverSetAntiReplayReferenceIfNotAlreadyElapsed(tContext * pContext);

/**
 * @brief   Destroyes a listener object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PReaderDriverListenerDestroy(
            tContext* pContext,
            void* pObject )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tPRegistryDriverReaderListener* pDriverListener = (tPRegistryDriverReaderListener*)pObject;
   tPRegistryDriverReaderListener** ppDriverListener;
   bool_t bDoNotReactivate = W_FALSE;

   PDebugTrace("static_PReaderDriverListenerDestroy()");

   CDebugAssert(pDriverListener != null);

   /* Remove the listener from the list */
   for(  ppDriverListener = &pReaderDriverRegistry->pDriverListenerListHead;
         *ppDriverListener != null;
         ppDriverListener = &((*ppDriverListener)->pNextDriverListener))
   {
      if(*ppDriverListener == pDriverListener)
      {
         *ppDriverListener = pDriverListener->pNextDriverListener;
         break;
      }
   }

   if (pDriverListener->bFromUser != W_FALSE)
   {
      PDFCDriverFlushCall(pDriverListener->pListenerDriverCC);
   }
   else
   {
      PDFCFlushCall(&pDriverListener->sListenerCC);
   }

   CDebugAssert(pReaderDriverRegistry->pCurrentDriverListener != pDriverListener);

   /* specific case for P2P initiator, the reactivation will be done later , during P2P TARGET configuration */
   if (pDriverListener->nRequestedProtocolsBF == W_NFCC_PROTOCOL_READER_P2P_INITIATOR)
   {
      bDoNotReactivate = W_TRUE;
   }

   /* Free the buffer */
   CMemoryFree( pDriverListener );

   if (bDoNotReactivate == W_FALSE)
   {
      /* Reactivate the detection, if needed */
      static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);
   }

   PDebugTrace("static_PReaderDriverListenerDestroy: end");

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Destroyes an error object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PReaderDriverErrorRegistryDestroy(
            tContext* pContext,
            void* pObject )
{
   tErrorRegistry* pErrorRegistry = (tErrorRegistry*)pObject;

   PDebugTrace("static_PReaderDriverErrorRegistryDestroy()");

   if ( pErrorRegistry->bIsRegistered != W_FALSE )
   {
      PDFCDriverFlushCall(pErrorRegistry->pErrorListenerDriverCC);
      pErrorRegistry->bIsRegistered = W_FALSE;

      /* Closing the unknown card detection may stop the detection */
      static_PReaderDriverReactivateDetection(pContext,
         PContextGetReaderDriverRegistry( pContext ), W_FALSE);
   }

   PDebugTrace("static_PReaderDriverErrorRegistryDestroy: end");

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Checkes if the card matches one application.
 *
 * @param[in]  pReaderDriverRegistry  The registry instance.
 **/
static void static_PReaderDriverNotifyUnknownTargetHandler(
            tContext * pContext,
            tPReaderDriverRegistry* pReaderDriverRegistry )
{
   PDebugTrace("         @NotifyUnknownCardHandler()");

   /* Call the unknown callback if one exists */
   if((pReaderDriverRegistry->sUnknownRegistry.bIsRegistered != W_FALSE)
   && (pReaderDriverRegistry->bCardApplicationMatch == W_FALSE))
   {
      PDebugTrace("         @NotifyUnknownCardHandler: calling the handler");

      /* Set the card to the unknown registry */
      pReaderDriverRegistry->bCardApplicationMatch = W_TRUE;

      /* Send the event */
      PDFCDriverPostCC2(
         pReaderDriverRegistry->sUnknownRegistry.pErrorListenerDriverCC,
         W_READER_ERROR_UNKNOWN );
   }
}

/* See  tPNALServiceExecuteCommandCompleted() */
static void static_PReaderDriverReactivateDetectionCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError,
            uint32_t nReceptionCounter)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = (tPReaderDriverRegistry*)pCallbackParameter;

   PDebugTrace("static_PReaderDriverReactivateDetectionCompleted(nReceptionCounter=%d)",
      nReceptionCounter);

   CDebugAssert(pReaderDriverRegistry->nDetectionState == P_READER_DETECTION_STATE_PENDING);
   CDebugAssert(pReaderDriverRegistry->nReaderDetectionCommandMessageCounter == 0);
   CDebugAssert(pReaderDriverRegistry->nCardEmulationDetectionCommandMessageCounter == 0);
   CDebugAssert(nReceptionCounter != 0);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PReaderDriverReactivateDetectionCompleted: Error %s",
         PUtilTraceError(nError));
      goto return_error;
   }

   if(nLength != 0)
   {
      PDebugError("static_PReaderDriverReactivateDetectionCompleted: Wrong length");
      goto return_error;
   }

   pReaderDriverRegistry->nDetectionState = P_READER_DETECTION_STATE_DETECTION;

   /* If at least one card protocol was requested */
   if((pReaderDriverRegistry->nCurrentDetectionProtocolBF & W_NFCC_PROTOCOL_CARD_ALL) != 0)
   {
      uint32_t i;

      pReaderDriverRegistry->nCardEmulationDetectionCommandMessageCounter = nReceptionCounter;

      /* process all stored messages */

      for (i=0; i<pReaderDriverRegistry->nStoredEmulMessageNb; i++)
      {
         if(pReaderDriverRegistry->sStoredCardEmulationMessage[i].nMessageCounter >= nReceptionCounter)
         {
            tPRegistryDriverCardProtocol* pCardProtocol =
               pReaderDriverRegistry->sStoredCardEmulationMessage[i].pCardProtocol;

            PDebugTrace("Processing stored message count %d / nReceptionCounter %d",
                     pReaderDriverRegistry->sStoredCardEmulationMessage[i].nMessageCounter,
                     nReceptionCounter);

            pCardProtocol->pConstant->pEventReceived(
               pContext, pCardProtocol->pCallbackParameter,
               pCardProtocol->pConstant->nProtocolBF,
               pReaderDriverRegistry->sStoredCardEmulationMessage[i].nEventIdentifier,
               pReaderDriverRegistry->sStoredCardEmulationMessage[i].aBuffer,
               pReaderDriverRegistry->sStoredCardEmulationMessage[i].nBufferLength);

            /* update replay reference only if not already elapsed */
            static_PReaderDriverSetAntiReplayReferenceIfNotAlreadyElapsed(pContext);

            /* Reset the message storage */
            pReaderDriverRegistry->sStoredCardEmulationMessage[i].nMessageCounter = 0;
         }
         else
         {
            PDebugTrace("Discarding stored message count %d / nReceptionCounter %d",
                     pReaderDriverRegistry->sStoredCardEmulationMessage[i].nMessageCounter,
                     nReceptionCounter);
         }
      }

      pReaderDriverRegistry->nStoredEmulMessageNb = 0;
   }

   while (pReaderDriverRegistry->nStoredEmulMessageNb > 0) {

      PDebugTrace("static_PReaderDriverReactivateDetectionCompleted: discard card emulation message");

      pReaderDriverRegistry->sStoredCardEmulationMessage[pReaderDriverRegistry->nStoredEmulMessageNb-1].nMessageCounter = 0;
      pReaderDriverRegistry->nStoredEmulMessageNb--;
   }

   /* If at least one reader protocol was requested */
   if((pReaderDriverRegistry->nCurrentDetectionProtocolBF & W_NFCC_PROTOCOL_READER_ALL) != 0)
   {
      pReaderDriverRegistry->nReaderDetectionCommandMessageCounter = nReceptionCounter;

      if(pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter >= nReceptionCounter)
      {
         static_PReaderDriverTargetDetection(
                  pContext, pReaderDriverRegistry,
                  &pReaderDriverRegistry->sStoredTargetDetectionMessage);

         CDebugAssert(pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter == 0);
         /* If a reacivation was needed, static_PReaderDriverTargetDetection() already called it */
         PDebugTrace("static_PReaderDriverReactivateDetectionCompleted: end");
         return;
      }
   }

   if(pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter != 0)
   {
      PDebugWarning("static_PReaderDriverReactivateDetectionCompleted: The last target discovered message is ignored");
      pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter = 0;
   }

   /* Call again the activation to be sure that nothing changed */
   static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);

   PDebugTrace("static_PReaderDriverReactivateDetectionCompleted: end");
   return;

return_error:

   /* Force the re-call to the detection command */
   pReaderDriverRegistry->nDetectionState = P_READER_DETECTION_STATE_DETECTION;

   static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);
   PDebugTrace("static_PReaderDriverReactivateDetectionCompleted: end");
}

/* Called when the reduced detection timer elapses */
static void static_PReaderDriverReducedDetectionTimerCompleted(
         tContext* pContext,
         void* pCallbackParameter )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = (tPReaderDriverRegistry*)pCallbackParameter;

   PDebugTrace("static_PReaderDriverReducedDetectionTimerCompleted()");

   if (pReaderDriverRegistry->bReducedDetection == W_FALSE)
   {

      PDebugError("static_PReaderDriverReducedDetectionTimerCompleted : spurious timer expiry");
      return;
   }

   pReaderDriverRegistry->bReducedDetectionExpired = W_TRUE;

   static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);
}

/**
 * Reactivates the detection of the protocols.
 *
 * @param[in]   pContext  The current context.
 *
 * @param[in]   pReaderDriverRegistry  The reader registry
 *
 * @param[in]   bStopActiveReader  Set the behavior when a reader protocol is active:
 *                - If set to W_TRUE, the reader mode will be stopped
 *                - If set to W_FALSE, the reader mode is not stopped.
 **/
static void static_PReaderDriverReactivateDetection(
         tContext* pContext,
         tPReaderDriverRegistry* pReaderDriverRegistry,
         bool_t bStopActiveReader)
{
   uint32_t nNewProtocolBF = 0;
/*    tPRegistryDriverReaderProtocol* pReaderProtocol; */
   tPRegistryDriverCardProtocol* pCardProtocol;
   bool_t bAtLeastOneListenerNotified = W_FALSE;
   tPRegistryDriverReaderListener* pDriverListener;
   uint32_t nNotNotifiedListenerProtocolBF = 0;
   uint32_t nAllListenerProtocolBF = 0;
   bool_t bSEListener = W_FALSE;
   uint32_t nNewTimeout;

   PDebugTrace("         @ReactivateDetection( bStopActiveReader=%s )",
      PUtilTraceBool(bStopActiveReader));

   if(pReaderDriverRegistry->nDetectionState == P_READER_DETECTION_STATE_PENDING)
   {
      PDebugTrace("         @ReactivateDetection() command pending --> does nothing");
      CDebugAssert(pReaderDriverRegistry->pCurrentDriverListener == null);
      goto return_function;
   }
   else if(pReaderDriverRegistry->nDetectionState == P_READER_DETECTION_STATE_ACTIVE)
   {
      if(bStopActiveReader == W_FALSE)
      {
         PDebugTrace("         @ReactivateDetection() reader active --> does nothing");

         if (pReaderDriverRegistry->pStopCallback != null)
         {
            tDFCCallbackContext sCallbackContext;
            PDFCFillCallbackContext(pContext, (tDFCCallback *) pReaderDriverRegistry->pStopCallback, pReaderDriverRegistry->pStopCallbackParameter, &sCallbackContext);
            PDFCPostContext1(&sCallbackContext);
            pReaderDriverRegistry->pStopCallback = null;
         }

         goto return_function;
      }
      else
      {
         PDebugTrace("         @ReactivateDetection() reader active --> will be stopped");
         pDriverListener = pReaderDriverRegistry->pCurrentDriverListener;
         if(pDriverListener != null)
         {
            /* Must be done before decrement, because of the possible destruction of the object */
            pReaderDriverRegistry->pCurrentDriverListener = null;

            /* Decrement the ref count of the listenner */
            /* Warning: this may cause:
                 - the destruction of the listener
                 - the modification of the listener list
            */
            PHandleDecrementReferenceCount(pContext, pDriverListener);
         }
      }
   }
   else
   {
      CDebugAssert(pReaderDriverRegistry->pCurrentDriverListener == null);
      CDebugAssert((pReaderDriverRegistry->nDetectionState == P_READER_DETECTION_STATE_DETECTION) );
   }

   /*
    * Force complete redetection if the detection timeout is elapsed
    */
   nNewTimeout = pReaderDriverRegistry->nTimeReference;

   if (static_PReaderDriverCheckDejaVu(pContext, P_READER_DETECTION_TIMEOUT, &nNewTimeout) == W_FALSE)
   {
      PDebugTrace("         @ReactivateDetection() - force complete redetection due to reader timeout expiry");

      /* Reset the notifications */
      for(  pDriverListener = pReaderDriverRegistry->pDriverListenerListHead;
            pDriverListener != null;
            pDriverListener = pDriverListener->pNextDriverListener)
      {
         /* we have a specific case for SE here, since SE detection is always one-shot.
            Several card handlers can not be registered for SE detection */

         if ((pDriverListener->nPriority != W_PRIORITY_SE) && (pDriverListener->nPriority != W_PRIORITY_SE_FORCED))
         {
            pDriverListener->nNotifiedProtocolsBF = 0;
         }

         pReaderDriverRegistry->bInCollisionResolution = W_FALSE;
      }
   }

   if ( (pReaderDriverRegistry->sUnknownRegistry.bIsRegistered != W_FALSE) &&
        (pReaderDriverRegistry->sUnknownRegistry.bCardDetectionRequested != W_FALSE))
   {
      tPRegistryDriverReaderProtocol* pReaderProtocol;

      PDebugTrace("         @ReactivateDetection() Detection requested because unknown card handler registered");
      PDebugTrace("         @ReactivateDetection() Then request every protocol supported");


      /* Get every reader protocol supported */
      for(pReaderProtocol = pReaderDriverRegistry->pReaderProtocolListHead;
            pReaderProtocol != null;
            pReaderProtocol = pReaderProtocol->pNext)
      {
         nAllListenerProtocolBF |= pReaderProtocol->pConstant->nProtocolBF;
      }
   }

   /* Check:
       - if a SE listener is registered
       - if at least one listener is notified
       - Build the protocol BF of the not notified listeners
       - Build the protocol BF of all the listeners
    */
   for(  pDriverListener = pReaderDriverRegistry->pDriverListenerListHead;
         pDriverListener != null;
         pDriverListener = pDriverListener->pNextDriverListener)
   {

      PDebugTrace("         @ReactivateDetection() pDriverListener %p : nPriority %d - nProtocol %08x :- notified %08x",
            pDriverListener, pDriverListener->nPriority, pDriverListener->nRequestedProtocolsBF, pDriverListener->nNotifiedProtocolsBF);

      if((pDriverListener->nPriority == W_PRIORITY_SE) ||
         (pDriverListener->nPriority == W_PRIORITY_SE_FORCED))
      {

         /* If a SE listener is present, all the other listener are ignored */
         bSEListener = W_TRUE;
         nAllListenerProtocolBF = pDriverListener->nRequestedProtocolsBF;

         if (pDriverListener->nNotifiedProtocolsBF != 0)
         {
            nNotNotifiedListenerProtocolBF = 0;
            bAtLeastOneListenerNotified = W_TRUE;
         }
         else
         {
            nNotNotifiedListenerProtocolBF = pDriverListener->nRequestedProtocolsBF;
         }

         break;
      }

      if(pDriverListener->nNotifiedProtocolsBF != 0)
      {
         bAtLeastOneListenerNotified = W_TRUE;
      }

      nNotNotifiedListenerProtocolBF |= pDriverListener->nRequestedProtocolsBF ^ pDriverListener->nNotifiedProtocolsBF;
      nAllListenerProtocolBF         |= pDriverListener->nRequestedProtocolsBF;
   }

   if (bSEListener != W_FALSE)
   {
      /* specific case : if a SE listener is registered, we use it */

      PDebugTrace("         @ReactivateDetection() A SE listener has been registered");

      pReaderDriverRegistry->bInCollisionResolution = W_FALSE;
      nNewProtocolBF = nNotNotifiedListenerProtocolBF;
   }
   else if (pReaderDriverRegistry->bStopAllActiveDetection != W_FALSE)
   {
      /* specific case : if stop of all non SE listener has been requested, we poll for nothing */

      PDebugTrace("         @ReactivateDetection() Stop of all detection has been requested");

      pReaderDriverRegistry->bInCollisionResolution = W_FALSE;
      nNewProtocolBF = 0;
   }
   else
   {
      /* this is the standard case */

      if (bAtLeastOneListenerNotified != W_FALSE)
      {
         tPRegistryDriverReaderProtocol* pReaderProtocol;

         PDebugTrace("         @ReactivateDetection() At least one listener is already notified");

         CDebugAssert(pReaderDriverRegistry->pLastCard->nProtocolBF != 0);

         /* Use the protocol of the last detected card */
         for(pReaderProtocol = pReaderDriverRegistry->pReaderProtocolListHead;
             pReaderProtocol != null;
             pReaderProtocol = pReaderProtocol->pNext)
         {
            if((pReaderProtocol->pConstant->nProtocolBF & pReaderDriverRegistry->pLastCard->nProtocolBF) != 0)
            {
               nNewProtocolBF = pReaderProtocol->pConstant->nProtocolBF;
               break;
            }
         }

         if (pReaderDriverRegistry->bReducedDetectionExpired == W_FALSE)
         {
            if ((nNewProtocolBF & nNotNotifiedListenerProtocolBF) == 0)
            {
               PDebugTrace("         @ReactivateDetection()  no more listener for the current card");

               if (pReaderDriverRegistry->bInCollisionResolution != W_FALSE)
               {
                  if (nNewProtocolBF & pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCurrentCollisionProtocolIdx])
                  {
                     pReaderDriverRegistry->nCurrentCollisionProtocolIdx++;

                     if (pReaderDriverRegistry->nCurrentCollisionProtocolIdx < pReaderDriverRegistry->nCollisionProtocolsSize)
                     {
                        nNewProtocolBF = nAllListenerProtocolBF;
                     }
                     else
                     {
                        pReaderDriverRegistry->bInCollisionResolution = W_FALSE;
                        nNewProtocolBF = 0;
                     }
                  }
                  else
                  {
                     nNewProtocolBF = nAllListenerProtocolBF;
                  }
               }
               else
               {
                  /* no more handler to be called */
                  nNewProtocolBF = 0;
               }
            }
         }
         else
         {
            PDebugTrace("         @ReactivateDetection()  bReducedDetection expired : start everything");
            nNewProtocolBF = nAllListenerProtocolBF;
         }
      }
      else
      {
         PDebugTrace("         @ReactivateDetection() New detection sequence : start everything");

         nNewProtocolBF = nAllListenerProtocolBF;
      }


      /* Apply the current collision filter if any */

      if (pReaderDriverRegistry->bInCollisionResolution != W_FALSE)
      {
         PDebugTrace("         @ReactivateDetection() Current collision filter %d, %08x",
                                             pReaderDriverRegistry->nCurrentCollisionProtocolIdx,
                                             pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCurrentCollisionProtocolIdx]);

         if ( (nNewProtocolBF & pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCurrentCollisionProtocolIdx] & nNotNotifiedListenerProtocolBF) == 0)
         {
            PDebugTrace("         @ReactivateDetection()  in collision, try to swith to next technology");

            /* There's no more listener matching the current listener index */
            pReaderDriverRegistry->nCurrentCollisionProtocolIdx++;

            while (pReaderDriverRegistry->nCurrentCollisionProtocolIdx < pReaderDriverRegistry->nCollisionProtocolsSize)
            {
               if ((nNotNotifiedListenerProtocolBF & pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCurrentCollisionProtocolIdx]) != 0)
               {
                  break;
               }

               pReaderDriverRegistry->nCurrentCollisionProtocolIdx++;
            }

            if (pReaderDriverRegistry->nCurrentCollisionProtocolIdx < pReaderDriverRegistry->nCollisionProtocolsSize)
            {
               PDebugTrace("         @ReactivateDetection() Applying  filter %d, %08x",
                                             pReaderDriverRegistry->nCurrentCollisionProtocolIdx,
                                             pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCurrentCollisionProtocolIdx]);

               nNewProtocolBF = nNotNotifiedListenerProtocolBF & pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCurrentCollisionProtocolIdx];
            }
            else
            {
               PDebugTrace("         @ReactivateDetection() End of collision resolution");

               pReaderDriverRegistry->bInCollisionResolution = W_FALSE;
            }
         }
         else
         {
            nNewProtocolBF &= pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCurrentCollisionProtocolIdx];
         }

      }

      pReaderDriverRegistry->bReducedDetection = (nNewProtocolBF != nAllListenerProtocolBF) ? W_TRUE : W_FALSE;
      PDebugTrace("         @ReactivateDetection() Reduced detection %d", pReaderDriverRegistry->bReducedDetection);


      /* Get every card protocol supported and active */

      for(pCardProtocol = pReaderDriverRegistry->pCardProtocolListHead;
          pCardProtocol != null;
          pCardProtocol = pCardProtocol->pNext)
      {
         if((pCardProtocol->bCardEmulationInUse) && (pCardProtocol->bCardEmulationConfigured))
         {
            nNewProtocolBF |= pCardProtocol->pConstant->nProtocolBF;
         }
      }
   }


   PDebugTrace("         @ReactivateDetection() request the protocols: Reader:%s  Card:%s",
      PUtilTraceReaderProtocol(pContext, nNewProtocolBF),
      PUtilTraceCardProtocol(pContext, nNewProtocolBF));
   if (nNewProtocolBF == pReaderDriverRegistry->nCurrentDetectionProtocolBF)
   {
      if(pReaderDriverRegistry->nDetectionState == P_READER_DETECTION_STATE_DETECTION)
      {
         PDebugTrace("         @ReactivateDetection() same protocols --> does nothing");

         pReaderDriverRegistry->bReducedDetectionExpired = W_FALSE;

         if (pReaderDriverRegistry->bReducedDetection != W_FALSE)
         {
            PDebugWarning("         @ReactivateDetection() Starting redetection timer");

            PMultiTimerSet( pContext,
               TIMER_T10_READER_DETECTION, 300,
               &static_PReaderDriverReducedDetectionTimerCompleted,
               pReaderDriverRegistry );
         }

         /* If a stop callback has been registered, post it */
         if (pReaderDriverRegistry->pStopCallback != null)
         {
            tDFCCallbackContext sCallbackContext;
            PDFCFillCallbackContext(pContext, (tDFCCallback *) pReaderDriverRegistry->pStopCallback, pReaderDriverRegistry->pStopCallbackParameter, &sCallbackContext);
            PDFCPostContext1(&sCallbackContext);
            pReaderDriverRegistry->pStopCallback = null;
         }

         goto return_function;
      }
   }
   /* Reset the message counter to avoid message crossing */
   pReaderDriverRegistry->nReaderDetectionCommandMessageCounter = 0;
   pReaderDriverRegistry->nCardEmulationDetectionCommandMessageCounter = 0;

   while (pReaderDriverRegistry->nStoredEmulMessageNb > 0)
   {
      PDebugWarning("         @ReactivateDetection() card emulation message is discarded");
      pReaderDriverRegistry->sStoredCardEmulationMessage[pReaderDriverRegistry->nStoredEmulMessageNb-1].nMessageCounter = 0;
      pReaderDriverRegistry->nStoredEmulMessageNb--;
   }

   if(pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter != 0)
   {
      PDebugWarning("         @ReactivateDetection() The last target discovered message is ignored");
      pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter = 0;
   }

   /* Cancel the timer and flush the timer event if needed */
   PDebugTrace("         @ReactivateDetection() Cancelling redetection timer");
   PMultiTimerCancel( pContext, TIMER_T10_READER_DETECTION );

   pReaderDriverRegistry->nDetectionState = P_READER_DETECTION_STATE_PENDING;

   PDebugTrace("         @ReactivateDetection() send NFC HAL detection");
   PDebugTrace("         @ReactivateDetection() nNewProtocolBF %08x",nNewProtocolBF );

   pReaderDriverRegistry->nCurrentDetectionProtocolBF = nNewProtocolBF;

   PNALWriteCardProtocols(nNewProtocolBF, pReaderDriverRegistry->aDetectionCommandData);
   PNALWriteReaderProtocols(nNewProtocolBF, &pReaderDriverRegistry->aDetectionCommandData[2]);

   PNALServiceExecuteCommand(
      pContext,
      NAL_SERVICE_ADMIN,
      &pReaderDriverRegistry->sDetectionCommandOperation,
      NAL_CMD_DETECTION,
      pReaderDriverRegistry->aDetectionCommandData, 4,
      null, 0,
      static_PReaderDriverReactivateDetectionCompleted,
      pReaderDriverRegistry );

return_function:

   PDebugTrace("         @ReactivateDetection() end");
   return; /* "return" statement avoid compilation error when traces are inactive */
}

/**
 * Compares a time reference with a timeout.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nTimeout  The timeout in milliseconds.
 *
 * @param[inout] pnReference  The current value of the time reference.
 *             This value is updated with the next reference value.
 *
 * @return  <i>W_TRUE</i> if the timeout is not elapsed, <i>W_FALSE</i> otherwise.
 **/
static bool_t static_PReaderDriverCheckDejaVu(
            tContext* pContext,
            uint32_t nTimeout,
            uint32_t* pnReference)
{
   uint32_t nReference = *pnReference;
   uint32_t nCurrentTime = PNALServiceDriverGetCurrentTime(pContext);

   *pnReference = nCurrentTime + nTimeout;

   return (nCurrentTime >= nReference)?W_FALSE:W_TRUE;
}

/**
 * Get the last reference Time
 *
 * @param[in]  pContext  The context.
 *
 * @return  <i>the last exchange time</i>
 **/
uint32_t PReaderDriverGetLastReferenceTime(tContext* pContext)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );

   return pReaderDriverRegistry->nTimeReference - P_READER_DETECTION_TIMEOUT;
}

/**
 * Gets the timeout reference.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nTimeout  The timeout in milliseconds.
 *
 * @return  The next timeout reference value.
 **/
uint32_t static_PReaderDriverGetDejaVu(
            tContext* pContext,
            uint32_t nTimeout)
{
   return PNALServiceDriverGetCurrentTime(pContext) + nTimeout;
}


static void static_PReaderDriverTargetDetection(
            tContext* pContext,
            tPReaderDriverRegistry* pReaderDriverRegistry,
            tPReaderDriverTargetDetectionMessage* pTargetDetectionMessage )
{
   bool_t bIsNewCard;
   tPRegistryDriverReaderListener* pDriverListener;
   W_HANDLE hDriverConnection;
   tPReaderDriverCardInfo sCurrentCard;
   W_ERROR nError;
   tPRegistryDriverReaderProtocol* pReaderProtocol;
   uint32_t i;
   uint32_t nCollision14443A, nCollision14443B, nCollisionFelica ,nCollision153, nCollisionBPrime;

   PDebugTrace("      @CardDetection( bCollision=%s )",
      PUtilTraceBool(pTargetDetectionMessage->bCollision));

   CDebugAssert(pReaderDriverRegistry->nDetectionState == P_READER_DETECTION_STATE_DETECTION);
   CDebugAssert(pReaderDriverRegistry->nReaderDetectionCommandMessageCounter != 0);

   pReaderDriverRegistry->nReaderDetectionCommandMessageCounter = 0;
   pReaderDriverRegistry->nDetectionState = P_READER_DETECTION_STATE_ACTIVE;

   PDebugTrace("      @CardDetection: cancelling redetection timer");
   PMultiTimerCancel( pContext, TIMER_T10_READER_DETECTION );

   /* Reset/Consume the message */
   pTargetDetectionMessage->nMessageCounter = 0;

   PDebugTrace("      @CardDetection: pTargetDetectionMessage->nProtocolBF %08X", pTargetDetectionMessage->nProtocolBF);

   /* Check the protocol of the card */
   nError = W_ERROR_BAD_PARAMETER;
   for(pReaderProtocol = pReaderDriverRegistry->pReaderProtocolListHead;
       pReaderProtocol != null;
       pReaderProtocol = pReaderProtocol->pNext)
   {
      PDebugTrace("      @CardDetection: pReaderProtocol->pConstant->nProtocolBF %08x", pReaderProtocol->pConstant->nProtocolBF);

      if((pReaderProtocol->pConstant->nProtocolBF & pTargetDetectionMessage->nProtocolBF) != 0)
      {
         nError = W_SUCCESS;
         break;
      }
   }

   if(nError != W_SUCCESS)
   {
      PDebugError("      @CardDetection: Unknown protocol %s",
         PUtilTraceReaderProtocol(pContext, pTargetDetectionMessage->nProtocolBF));
      goto return_error;
   }

   if(pTargetDetectionMessage->bCollision != W_FALSE)
   {
      PDebugWarning("      @CardDetection: Collision");

      goto collision_detected;
   }

   CMemoryFill(&sCurrentCard, 0, sizeof(sCurrentCard));
   sCurrentCard.nProtocolBF = pReaderProtocol->pConstant->nProtocolBF;

   /* Parse the detection message and get the card UID */
   PDebugTrace("      @CardDetection: Parsing card data:");
   nError = pReaderProtocol->pConstant->pParseDetectionMessage(
      pContext,
      pTargetDetectionMessage->aBuffer, pTargetDetectionMessage->nBufferLength,
      &sCurrentCard );

   if(nError != W_SUCCESS)
   {
      PDebugError("      @CardDetection: Error %s during the UID parsing",
            PUtilTraceError(nError));
      goto return_error;
   }

   if (pReaderDriverRegistry->pLastCard != & pReaderProtocol->sLastCard)
   {
      PDebugTrace("        @CardDetection: card protocol change detected, force cache reinitialisation");
      /* Reset the cache Connection */
      PCacheConnectionReset(pContext);
   }

   /* Select the last card according to the current protocol */
   pReaderDriverRegistry->pLastCard = & pReaderProtocol->sLastCard;

   /* Check if the card is already detected */
   if(static_PReaderDriverCheckDejaVu(pContext,P_READER_DETECTION_TIMEOUT, &pReaderDriverRegistry->nTimeReference ) != W_FALSE)
   {
      PDebugTrace("      @CardDetection: card detected during the reader detection timeout");

      /* Check the procotol */
      if (  ( sCurrentCard.nUIDLength == pReaderDriverRegistry->pLastCard->nUIDLength )
         && ( sCurrentCard.nAFI == pReaderDriverRegistry->pLastCard->nAFI )
         && ( sCurrentCard.nProtocolBF == pReaderDriverRegistry->pLastCard->nProtocolBF ) )
      {
         /* Special case : random UID for type A-3 and A-4 */
         if ( ((sCurrentCard.nProtocolBF & (W_NFCC_PROTOCOL_READER_ISO_14443_3_A | W_NFCC_PROTOCOL_READER_ISO_14443_4_A)) != 0)
               && (sCurrentCard.nUIDLength > 0)
               && (sCurrentCard.aUID[0] == 0x08)
               && (pReaderDriverRegistry->pLastCard->aUID[0] == 0x08))
         {
            bIsNewCard = W_FALSE;
         }
         /* Special case : random UID for type B - 4 */
         else if ((sCurrentCard.nProtocolBF & W_NFCC_PROTOCOL_READER_ISO_14443_4_B) != 0)
         {
            bIsNewCard = W_FALSE;
         }
         else if ( CMemoryCompare( sCurrentCard.aUID, pReaderDriverRegistry->pLastCard->aUID, sCurrentCard.nUIDLength ) == 0 )
         {
            bIsNewCard = W_FALSE;
         }
         else
         {
            bIsNewCard = W_TRUE;
         }
      }
      else
      {
         bIsNewCard = W_TRUE;
      }

      if (bIsNewCard == W_TRUE)
      {
         /* Reset the registry for a new detection sequence */

         /* when a card change is detected during a reduced detection,
            the card should be redetected with all reader modes activated, but
            never break a P2P connection and
            when in collision resolution, do not detect card twice during protocol change */

         if ( (pReaderDriverRegistry->bReducedDetection != W_FALSE)  &&
              (sCurrentCard.nProtocolBF != W_NFCC_PROTOCOL_READER_P2P_INITIATOR) &&
              ((pReaderDriverRegistry->bInCollisionResolution == W_FALSE) || ((sCurrentCard.nProtocolBF == pReaderDriverRegistry->pLastCard->nProtocolBF))))
         {
            PDebugTrace("      @CardDetection: New card detected during reduced detection : force redetection");

            /* Fake expiry of the redetection timer */
            pReaderDriverRegistry->bReducedDetectionExpired = W_TRUE;
            goto return_end_reader;
         }
         else
         {
            PDebugTrace("      @CardDetection: not the same card has before, only reset matching notified protocols");

            for(  pDriverListener = pReaderDriverRegistry->pDriverListenerListHead;
                  pDriverListener != null;
                  pDriverListener = pDriverListener->pNextDriverListener)
            {
               pDriverListener->nNotifiedProtocolsBF &= ~sCurrentCard.nProtocolBF;
            }
         }
      }
   }
   else
   {
      PDebugTrace("      @CardDetection: card detected after the redetection timer,  reset all notified protocols");

      /* Reset the registry for a new detection sequence */

      for(  pDriverListener = pReaderDriverRegistry->pDriverListenerListHead;
            pDriverListener != null;
            pDriverListener = pDriverListener->pNextDriverListener)
      {
         pDriverListener->nNotifiedProtocolsBF = 0;
      }

      bIsNewCard = W_TRUE;
   }

   if ( bIsNewCard != W_FALSE )
   {
      PDebugTrace("      @CardDetection: New card detected");

      /* Copy this new card parameter for the next anti-replay */
      pReaderDriverRegistry->pLastCard->nUIDLength = sCurrentCard.nUIDLength;
      pReaderDriverRegistry->pLastCard->nAFI = sCurrentCard.nAFI;
      pReaderDriverRegistry->pLastCard->nProtocolBF = sCurrentCard.nProtocolBF;
      CMemoryCopy( pReaderDriverRegistry->pLastCard->aUID, sCurrentCard.aUID, sCurrentCard.nUIDLength );

      pReaderDriverRegistry->bCardApplicationMatch = W_FALSE;

      /* Reset the cache Connection */
      PCacheConnectionReset(pContext);
   }
   else
   {
      PDebugTrace("      @CardDetection: Same card as before");
   }

   pDriverListener = static_PReaderDriverGetNextListener(pContext, pReaderDriverRegistry, &sCurrentCard);

   if( pDriverListener == null )
   {
      PDebugTrace("      @CardDetection: No listener remaining for this card");

      static_PReaderDriverNotifyUnknownTargetHandler(pContext, pReaderDriverRegistry);
      goto return_end_reader;
   }

   /* Store the answer */
   if(pTargetDetectionMessage->nBufferLength > pDriverListener->nBufferMaxLength)
   {
      PDebugError("      @CardDetection: The user buffer is too short");
      goto return_end_reader;
   }
   CMemoryCopy( pDriverListener->pBuffer, pTargetDetectionMessage->aBuffer, pTargetDetectionMessage->nBufferLength );

   if (pDriverListener->bFromUser != W_FALSE)
   {
      PDFCDriverSetCurrentUserInstance(pContext, pDriverListener->pListenerDriverCC );
   }

   /* Get the driver connection handle */
   PDebugTrace("      @CardDetection: Create the driver connection for the card");

   nError = pReaderProtocol->pConstant->pCreateConnection(
            pContext,
            pReaderProtocol->pConstant->nServiceIdentifier,
            &hDriverConnection );
   if (nError != W_SUCCESS )
   {

      if (pDriverListener->bFromUser != W_FALSE)
      {
         PDFCDriverSetCurrentUserInstance( pContext, null );
      }

      PDebugError("      @CardDetection: error %s on create driver connection",
         PUtilTraceError(nError));
      goto return_end_reader;
   }

   /* Add the global connection structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hDriverConnection,
                     &pReaderDriverRegistry->sReaderConnectionObjectHeader,
                     P_HANDLE_TYPE_READER_DRIVER_CONNECTION ) ) != W_SUCCESS )
   {
      if (pDriverListener->bFromUser != W_FALSE)
      {
         PDFCDriverSetCurrentUserInstance( pContext, null );
      }

      PHandleClose(pContext, hDriverConnection);

      PDebugError("      @CardDetection: error %s returned by PHandleAddHeir()",
         PUtilTraceError(nError));
      goto return_end_reader;
   }

   if (pDriverListener->bFromUser != W_FALSE)
   {
      PDFCDriverSetCurrentUserInstance( pContext, null );
   }

   /* increment the reference count of the listener to keep it with the connection */

   CDebugAssert(pReaderDriverRegistry->pCurrentDriverListener == null);
   CDebugAssert(pReaderDriverRegistry->hCurrentDriverConnection == W_NULL_HANDLE);
   pReaderDriverRegistry->pCurrentDriverListener = pDriverListener;
   pReaderDriverRegistry->hCurrentDriverConnection = hDriverConnection;
   PHandleIncrementReferenceCount(pDriverListener);

   PDebugTrace("      @CardDetection() Send the detection event to user");

   if (pDriverListener->bFromUser != W_FALSE)
   {
      PDFCDriverPostCC5(
         pDriverListener->pListenerDriverCC,
         PUtilConvertUintToPointer(sCurrentCard.nProtocolBF),
         hDriverConnection,
         pTargetDetectionMessage->nBufferLength,
         pReaderDriverRegistry->bCardApplicationMatch );
   }
   else
   {
      PDFCPostContext5(
         &pDriverListener->sListenerCC,
         PUtilConvertUintToPointer(sCurrentCard.nProtocolBF),
         hDriverConnection,
         pTargetDetectionMessage->nBufferLength,
         pReaderDriverRegistry->bCardApplicationMatch );
   }


   PDebugTrace("      @CardDetection() end");
   return;

return_error:

   PDebugError("      @CardDetection: error %s", PUtilTraceError(nError));

   PNFCControllerNotifyException(pContext, W_NFCC_EXCEPTION_NAL_PROTOCOL_ERROR);

   goto return_end_reader;

collision_detected:

   if(static_PReaderDriverCheckDejaVu(pContext,P_READER_DETECTION_TIMEOUT, &pReaderDriverRegistry->nTimeReference ) == W_FALSE)
   {
      PDebugTrace("      @CardDetection: collision detected after the redetection timer,  reset all notified protocols");
      /* Reset the registry for a new detection sequence */

      for(  pDriverListener = pReaderDriverRegistry->pDriverListenerListHead;
            pDriverListener != null;
            pDriverListener = pDriverListener->pNextDriverListener)
      {
         pDriverListener->nNotifiedProtocolsBF = 0;
      }

   }

   nCollision14443A = nCollision14443B = nCollisionFelica = nCollision153 = nCollisionBPrime = 0;

   for (i=0; i<pTargetDetectionMessage->nBufferLength; i+= 2)
   {
      switch (PNALReadUint16FromBuffer(&pTargetDetectionMessage->aBuffer[i]))
      {
         case NAL_PROTOCOL_READER_KOVIO:
         case NAL_PROTOCOL_READER_TYPE_1_CHIP:
         case NAL_PROTOCOL_READER_ISO_14443_3_A:
         case NAL_PROTOCOL_READER_ISO_14443_4_A:
            nCollision14443A++;
            break;
         case NAL_PROTOCOL_READER_ISO_14443_3_B:
         case NAL_PROTOCOL_READER_ISO_14443_4_B:
            nCollision14443B++;
            break;
         case NAL_PROTOCOL_READER_P2P_INITIATOR:
         case NAL_PROTOCOL_READER_FELICA:
            nCollisionFelica++;
            break;
         case NAL_PROTOCOL_READER_ISO_15693_2:
         case NAL_PROTOCOL_READER_ISO_15693_3:
            nCollision153++;
            break;
         case NAL_PROTOCOL_READER_BPRIME:
            nCollisionBPrime++;
            break;
      }
   }

   if ( (nCollision14443A > 1) || (nCollision14443B > 1) || (nCollisionFelica > 1) ||  (nCollision153 > 1) || (nCollisionBPrime > 1))
   {
      /* several cards in the same technology */

      /* Call the collision event handler if one exists */
      if ( pReaderDriverRegistry->sCollisionRegistry.bIsRegistered != W_FALSE )
      {
         PDFCDriverPostCC2(
            pReaderDriverRegistry->sCollisionRegistry.pErrorListenerDriverCC,
            W_READER_ERROR_COLLISION );
      }

      /* do not try to resolve collion */
      goto return_end_reader;
   }


   /* Call the multiple card event handler if one exists */
   if ( pReaderDriverRegistry->sMultipleDetectionRegistry.bIsRegistered != W_FALSE )
   {
      PDFCDriverPostCC2(
            pReaderDriverRegistry->sMultipleDetectionRegistry.pErrorListenerDriverCC,
            W_READER_MULTIPLE_DETECTION );

      /* do not try to resolve collion */
      goto return_end_reader;
   }

   /* try to resolve collision */
   pReaderDriverRegistry->bInCollisionResolution = W_TRUE;

   pReaderDriverRegistry->nCollisionProtocolsSize = 0;
   pReaderDriverRegistry->nCurrentCollisionProtocolIdx = 0;

   if (nCollision14443A)
   {
      pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCollisionProtocolsSize++]
            = W_NFCC_PROTOCOL_READER_ISO_14443_3_A | W_NFCC_PROTOCOL_READER_ISO_14443_4_A | W_NFCC_PROTOCOL_READER_TYPE_1_CHIP | W_NFCC_PROTOCOL_READER_KOVIO;
   }

   if (nCollision14443B)
   {
        pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCollisionProtocolsSize++]
            = W_NFCC_PROTOCOL_READER_ISO_14443_3_B | W_NFCC_PROTOCOL_READER_ISO_14443_4_B;
   }

   if (nCollisionFelica)
   {
        pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCollisionProtocolsSize++]
            = W_NFCC_PROTOCOL_READER_FELICA | W_NFCC_PROTOCOL_READER_P2P_INITIATOR;
   }

   if (nCollision153)
   {
      pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCollisionProtocolsSize++]
            = W_NFCC_PROTOCOL_READER_ISO_15693_3;
   }

   if (nCollisionBPrime)
   {
      pReaderDriverRegistry->aCollisionProtocols[pReaderDriverRegistry->nCollisionProtocolsSize++]
            = W_NFCC_PROTOCOL_READER_BPRIME;
   }

return_end_reader:

   /* Restart the detection, if needed */
   static_PReaderDriverReactivateDetection( pContext, pReaderDriverRegistry, W_TRUE);
   PDebugTrace("      @CardDetection() end");
}

/* See tPNALServiceEventReceived() */
static void static_PReaderDriverTargetDetectionEventHandler(
         tContext* pContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         const uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nNALMessageReceptionCounter)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   uint32_t nProtocolBF = (uint32_t)PUtilConvertPointerToUint(pCallbackParameter);
   bool_t bCollision;
   bool_t bFeliCaCollision;
   uint16_t nIndexTmp;

   pReaderDriverRegistry->nNbCardDetected = 0;

   PDebugTrace(
      "static_PReaderDriverTargetDetectionEventHandler: nEventIdentifier 0x%02X (nNALMessageReceptionCounter %d)",
      nEventIdentifier, nNALMessageReceptionCounter );

   if(pBuffer == null)
   {
      goto return_protocol_error;
   }

   switch(nEventIdentifier)
   {
      case NAL_EVT_READER_TARGET_DISCOVERED:
         if((nLength == 0) || (nLength > NAL_MESSAGE_MAX_LENGTH))
         {
            goto return_protocol_error;
         }

         pReaderDriverRegistry->nNbCardDetected = 1;
         bCollision = W_FALSE;
         bFeliCaCollision = W_FALSE;
         break;
      case NAL_EVT_READER_TARGET_COLLISION:
         if((nLength == 0) || (nLength > NAL_MESSAGE_MAX_LENGTH))
         {
            goto return_protocol_error;
         }

         pReaderDriverRegistry->nNbCardDetected = (uint8_t) ( nLength >> 1 );

         bFeliCaCollision = W_TRUE;
         bCollision = W_FALSE;

         PDebugTrace("static_PReaderDriverTargetDetectionEventHandler: Test FeliCa Cards");
         /* Test if there are only FeliCa Cards */
         for(nIndexTmp = 0; nIndexTmp < nLength; nIndexTmp += 2)
         {
            if(PNALReadUint16FromBuffer(&pBuffer[nIndexTmp]) != NAL_PROTOCOL_READER_FELICA)
            {
               /* if one card is not a FeliCa, we take the conclusion that it's a real collision */
               bFeliCaCollision = W_FALSE;
               bCollision = W_TRUE;
               break;
            }
         }

         break;

      case NAL_EVT_P2P_SEND_DATA :

         /* Store the information */
         CMemoryCopy( pReaderDriverRegistry->aDataBuffer, pBuffer, nLength );
         PDFCPostContext3(&pReaderDriverRegistry->sReceptionCC, pReaderDriverRegistry->aDataBuffer, nLength);
         return;

      case NAL_EVT_P2P_TARGET_DISCOVERED :
         bCollision = W_FALSE;
         bFeliCaCollision = W_FALSE;
         break;
      default:
         goto return_protocol_error;
   }

   if(pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter != 0)
   {
      PDebugWarning("static_PReaderDriverTargetDetectionEventHandler: The last target discovered event is ignored");
      pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter = 0;
   }

   if(pReaderDriverRegistry->nReaderDetectionCommandMessageCounter != 0)
   {
      if(pReaderDriverRegistry->nReaderDetectionCommandMessageCounter <= nNALMessageReceptionCounter)
      {
         tPReaderDriverTargetDetectionMessage sTargetDetectionMessage;

         sTargetDetectionMessage.nMessageCounter = nNALMessageReceptionCounter;
         sTargetDetectionMessage.nProtocolBF = nProtocolBF;
         sTargetDetectionMessage.bCollision = bCollision;


         if(bFeliCaCollision != W_FALSE)
         {
            sTargetDetectionMessage.nBufferLength = NAL_READER_FELICA_DETECTION_MESSAGE_SIZE;
            CMemoryFill(sTargetDetectionMessage.aBuffer, 0xFF, NAL_READER_FELICA_DETECTION_MESSAGE_SIZE);
         }
         else if(nLength != 0)
         {
            sTargetDetectionMessage.nBufferLength = nLength;
            CMemoryCopy(sTargetDetectionMessage.aBuffer, pBuffer, nLength);
         }
         else
         {
            sTargetDetectionMessage.nBufferLength = 0;
         }

         static_PReaderDriverTargetDetection(
                  pContext, pReaderDriverRegistry, &sTargetDetectionMessage);
      }
      else
      {
         PDebugWarning("static_PReaderDriverTargetDetectionEventHandler: The card detection event is ignored");
      }
   }
   else
   {
      /* Keep the card detection info until the start completion is received */
      PDebugTrace("static_PReaderDriverTargetDetectionEventHandler: store card detection message");
      pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter = nNALMessageReceptionCounter;
      pReaderDriverRegistry->sStoredTargetDetectionMessage.nProtocolBF = nProtocolBF;
      pReaderDriverRegistry->sStoredTargetDetectionMessage.bCollision = bCollision;

      if(bFeliCaCollision != W_FALSE)
      {
         pReaderDriverRegistry->sStoredTargetDetectionMessage.nBufferLength = NAL_READER_FELICA_DETECTION_MESSAGE_SIZE;
         CMemoryFill(pReaderDriverRegistry->sStoredTargetDetectionMessage.aBuffer, 0xFF, NAL_READER_FELICA_DETECTION_MESSAGE_SIZE);
      }
      else if(nLength != 0)
      {
         pReaderDriverRegistry->sStoredTargetDetectionMessage.nBufferLength = nLength;
         CMemoryCopy(pReaderDriverRegistry->sStoredTargetDetectionMessage.aBuffer, pBuffer, nLength);
      }
      else
      {
         pReaderDriverRegistry->sStoredTargetDetectionMessage.nBufferLength = 0;
      }
   }

   PDebugTrace("static_PReaderDriverTargetDetectionEventHandler: end");
   return;

return_protocol_error:

   PDebugError("static_PReaderDriverTargetDetectionEventHandler: NFC HAL Protocol error");
   PNFCControllerNotifyException(pContext, W_NFCC_EXCEPTION_NAL_PROTOCOL_ERROR);
   PDebugTrace("static_PReaderDriverTargetDetectionEventHandler: end");
}

/**
 * Finds the next listener instance.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pReaderDriverRegistry  The registry instance.
 *
 * @param[in]  pCardInfo  The card information.
 *
 * @return  The next listener instance or null if no isntance is found.
 **/
static tPRegistryDriverReaderListener* static_PReaderDriverGetNextListener(
               tContext* pContext,
               tPReaderDriverRegistry* pReaderDriverRegistry,
               tPReaderDriverCardInfo* pCardInfo)
{
   tPRegistryDriverReaderListener* pDriverListener;
   tPRegistryDriverReaderListener* pFoundDriverListener = null;
   uint8_t nPriority = W_PRIORITY_NO_ACCESS;
   tPRegistryDriverReaderProtocol* pReaderProtocol;

   PDebugTrace("         @GetNextListener() for card %s",
      PUtilTraceReaderProtocol(pContext, pCardInfo->nProtocolBF));

   CDebugAssert(pCardInfo->nProtocolBF != 0);

   /* Check if the card match the detection configuration */
   for(pReaderProtocol = pReaderDriverRegistry->pReaderProtocolListHead;
       pReaderProtocol != null;
       pReaderProtocol = pReaderProtocol->pNext)
   {
      if((pReaderProtocol->pConstant->nProtocolBF & pCardInfo->nProtocolBF) != 0)
      {
         if((pReaderProtocol->nDetectionConfigurationLength != 0)
         && (pReaderProtocol->pConstant->pCheckCardMatchConfiguration != null))
         {
            if(pReaderProtocol->pConstant->pCheckCardMatchConfiguration(
               pContext,
               pCardInfo->nProtocolBF,
               pReaderProtocol->aDetectionConfiguration,
               pReaderProtocol->nDetectionConfigurationLength,
               pCardInfo) == W_FALSE)
            {
               PDebugTrace("         @GetNextListener() The card does not match the configuration");
               return null;
            }
         }
         break;
      }
   }

   /* Look for the next listener instance */
   for(  pDriverListener = pReaderDriverRegistry->pDriverListenerListHead;
         pDriverListener != null;
         pDriverListener = pDriverListener->pNextDriverListener)
   {
      PDebugTrace("         @GetNextListener()   - listener requested=%08x notified=%08x priority=%s",
         pDriverListener->nRequestedProtocolsBF,
         pDriverListener->nNotifiedProtocolsBF,
         PUtilTracePriority(pDriverListener->nPriority));


      if ((pDriverListener->nRequestedProtocolsBF & pCardInfo->nProtocolBF) == 0)
      {
         /* Skip not matching listeners */
         continue;
      }

      if ((pDriverListener->nNotifiedProtocolsBF & pCardInfo->nProtocolBF) != 0)
      {
         /* This handler has already been notified */
         continue;
      }

      /* If there's a SE listener registered, this is the one who will be notified */

      if  ((pDriverListener->nPriority == W_PRIORITY_SE) ||
           (pDriverListener->nPriority == W_PRIORITY_SE_FORCED))
      {
         nPriority = pDriverListener->nPriority;
         pFoundDriverListener = pDriverListener;
         break;
      }

      /* The new listener are added at the beginning of the list
         Use >= to get the oldest listener with the same priority */

      if(pDriverListener->nPriority >= nPriority)
      {
         nPriority = pDriverListener->nPriority;
         pFoundDriverListener = pDriverListener;
      }

   } /* foreach listener */

   if(pFoundDriverListener != null)
   {
      PDebugTrace("         @GetNextListener() SELECTED : protocol=%s priority=%s",
         PUtilTraceReaderProtocol(pContext, pFoundDriverListener->nRequestedProtocolsBF),
         PUtilTracePriority(pFoundDriverListener->nPriority));
   }
   else
   {
      PDebugTrace("         @GetNextListener() no listener matching this card");
   }

   return pFoundDriverListener;
}

/**
 * Checks the requested protocols.
 *
 * @param[in]   pContext  The current context.
 *
 * @param[in]   pReaderDriverRegistry  The registry instance.
 *
 * @param[inout] pnRequestedProtocolsBF  The requested protocol(s) bit field.
 *
 * @param[out]  apRequestedProtocolList  A pointer on an array valued
 *              with the filtered protocols.
 *
 * @param[out]  pnRequestedProtocolNumber  A pointer on a variable valued with
 *              the number of protocols
 *
 * @return      W_SUCCESS if the protocols are correct, an error code otherwise.
 **/
static W_ERROR static_PReaderDriverCheckProtocols(
            tContext* pContext,
            tPReaderDriverRegistry* pReaderDriverRegistry,
            uint32_t* pnRequestedProtocolsBF,
            tPRegistryDriverReaderProtocol** apRequestedProtocolList,
            uint32_t* pnRequestedProtocolNumber)
{
   uint32_t nRequestedProtocolNumber = 0;
   uint32_t nRequestedProtocolsBF = 0;
   uint32_t nCheckProtocolsBF = *pnRequestedProtocolsBF;
   tPRegistryDriverReaderProtocol* pReaderProtocol;
   W_ERROR nError = W_SUCCESS;

   if(nCheckProtocolsBF == 0)
   {
      PDebugError("static_PReaderDriverCheckProtocols: nRequestedProtocolsBF is null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_function;
   }

   for(pReaderProtocol = pReaderDriverRegistry->pReaderProtocolListHead;
       pReaderProtocol != null;
       pReaderProtocol = pReaderProtocol->pNext)
   {
      if((pReaderProtocol->pConstant->nProtocolBF & nCheckProtocolsBF) != 0)
      {
         nRequestedProtocolsBF |= pReaderProtocol->pConstant->nProtocolBF;
      }
   }
   /* Special case: ISO 14443 Part 4 protocols */
   if((nCheckProtocolsBF & W_NFCC_PROTOCOL_READER_ISO_14443_4_A) != 0)
   {
#ifndef P_READER_14P4_STANDALONE_SUPPORT
     nCheckProtocolsBF &= ~W_NFCC_PROTOCOL_READER_ISO_14443_4_A;
      nCheckProtocolsBF |= W_NFCC_PROTOCOL_READER_ISO_14443_3_A;
#endif /* P_READER_14P4_STANDALONE_SUPPORT */
      nRequestedProtocolsBF |= W_NFCC_PROTOCOL_READER_ISO_14443_4_A;
   }
   if((nCheckProtocolsBF & W_NFCC_PROTOCOL_READER_ISO_14443_4_B) != 0)
   {
#ifndef P_READER_14P4_STANDALONE_SUPPORT
      nCheckProtocolsBF &= ~W_NFCC_PROTOCOL_READER_ISO_14443_4_B;
      nCheckProtocolsBF |= W_NFCC_PROTOCOL_READER_ISO_14443_3_B;
#endif /* P_READER_14P4_STANDALONE_SUPPORT */
      nRequestedProtocolsBF |= W_NFCC_PROTOCOL_READER_ISO_14443_4_B;
   }

   for(pReaderProtocol = pReaderDriverRegistry->pReaderProtocolListHead;
       pReaderProtocol != null;
       pReaderProtocol = pReaderProtocol->pNext)
   {
      if((pReaderProtocol->pConstant->nProtocolBF & nCheckProtocolsBF) != 0)
      {
         apRequestedProtocolList[nRequestedProtocolNumber++] = pReaderProtocol;
      }
   }

   if(nRequestedProtocolNumber == 0)
   {
      PDebugError("static_PReaderDriverCheckProtocols: None of the protocols is supported");
      nError = W_ERROR_BAD_PARAMETER;
   }

return_function:

   if(nError != W_SUCCESS)
   {
      nRequestedProtocolNumber = 0;
      nRequestedProtocolsBF = 0;
   }

   *pnRequestedProtocolsBF = nRequestedProtocolsBF;
   *pnRequestedProtocolNumber = nRequestedProtocolNumber;

   return nError;
}

/* See  PNALServiceSetParameter */
static void static_PReaderDriverSetParameterCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError )
{
   tPRegistryDriverReaderProtocol* pReaderProtocol = (tPRegistryDriverReaderProtocol*)pCallbackParameter;
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry(pContext);

   PDebugTrace("static_PReaderDriverSetParameterCompleted()");

   if(pReaderProtocol->nSetDetectionConfigurationState == P_READER_CONFIG_STATE_GENERIC_PENDING)
   {
      pReaderProtocol->nSetDetectionConfigurationState = P_READER_CONFIG_STATE_GENERIC;
   }
   else if(pReaderProtocol->nSetDetectionConfigurationState == P_READER_CONFIG_STATE_SPECIFIC_PENDING)
   {
      pReaderProtocol->nSetDetectionConfigurationState = P_READER_CONFIG_STATE_SPECIFIC;
   }
   else
   {
      PDebugError("static_PReaderDriverSetParameterCompleted: Illegal state");
      nError = W_ERROR_BAD_PARAMETER;
   }

   /* Check the result */
   if ( nError == W_SUCCESS )
   {

      if (pReaderProtocol->pConstant->nProtocolBF != W_NFCC_PROTOCOL_READER_P2P_INITIATOR)
      {
         /* Check if we need to register to the NFC Controller */
         static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);
      }

      PDebugTrace("static_PReaderDriverSetParameterCompleted: end");
      return;
   }

   PDebugError( "static_PReaderDriverSetParameterCompleted: nError %s",
         PUtilTraceError(nError));

   PNFCControllerNotifyException(pContext, W_NFCC_EXCEPTION_NAL_PROTOCOL_ERROR);

   PDebugTrace("static_PReaderDriverSetParameterCompleted: end");
}

/**
 * Checks the requested priority.
 *
 * @param[in]   pContext  The current context.
 *
 * @param[in]   pReaderDriverRegistry  The registry instance.
 *
 * @param[in]   nPriority  The requested priority.
 *
 * @param[in]   nRequestedProtocolsBF  The requested protocol bit field.
 *
 * @return      W_SUCCESS if the priority is correct, an error code otherwise.
 **/
static W_ERROR static_PReaderDriverCheckPriority(
            tContext* pContext,
            tPReaderDriverRegistry* pReaderDriverRegistry,
            uint8_t nPriority,
            uint32_t nRequestedProtocolsBF)
{
   tPRegistryDriverReaderListener* pDriverListener = pReaderDriverRegistry->pDriverListenerListHead;

   if ((nPriority != W_PRIORITY_SE) && (nPriority != W_PRIORITY_SE_FORCED))
   {
      while ( pDriverListener != null )
      {
         if((pDriverListener->nRequestedProtocolsBF & nRequestedProtocolsBF) != 0)
         {
            /* Check the requested priority */
            if ( nPriority == W_PRIORITY_EXCLUSIVE )
            {
               PDebugError("PReaderDriverRegister: Exclusive mode rejected");
               return W_ERROR_EXCLUSIVE_REJECTED;
            }
            /* Check the existing priority */
            if ( pDriverListener->nPriority == W_PRIORITY_EXCLUSIVE )
            {
               PDebugError("PReaderDriverRegister: Shared mode rejected for protocol");
               return W_ERROR_SHARE_REJECTED;
            }
         }
         pDriverListener = pDriverListener->pNextDriverListener;
      }
   }

   return W_SUCCESS;
}

/* See header file */
W_ERROR PReaderDriverRegister(
            tContext* pContext,
            tPReaderDriverRegisterCompleted* pCallback,
            void* pCallbackParameter,
            uint8_t nPriority,
            uint32_t nRequestedProtocolsBF,
            uint32_t nDetectionConfigurationLength,
            uint8_t* pBuffer,
            uint32_t nBufferMaxLength,
            W_HANDLE* phListenerHandle )
{
   return PReaderDriverRegisterInternal(
               pContext,
               pCallback,
               pCallbackParameter,
               nPriority,
               nRequestedProtocolsBF,
               nDetectionConfigurationLength,
               pBuffer,
               nBufferMaxLength,
               phListenerHandle,
               W_TRUE);

}

W_ERROR PReaderDriverRegisterInternal(
            tContext* pContext,
            tPReaderDriverRegisterCompleted* pCallback,
            void* pCallbackParameter,
            uint8_t nPriority,
            uint32_t nRequestedProtocolsBF,
            uint32_t nDetectionConfigurationLength,
            uint8_t* pBuffer,
            uint32_t nBufferMaxLength,
            W_HANDLE* phListenerHandle,
            bool_t      bFromUser)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tPRegistryDriverReaderListener* pDriverListener = null;
   W_ERROR nError;
   W_HANDLE hListener;
   tPRegistryDriverReaderProtocol* pReaderProtocol;
   tPRegistryDriverReaderProtocol* apRequestedProtocolList[P_READER_PROTOCOL_NUMBER];
   uint32_t nRequestedProtocolNumber;
   bool_t bSetConfigurationPending = W_FALSE;

   PDebugTrace("PReaderDriverRegister( nRequestedProtocolsBF=%s nPriority=%s )",
      PUtilTraceReaderProtocol(pContext, nRequestedProtocolsBF),
      PUtilTracePriority(nPriority));

   /* Check the parameters */
   if (  ( phListenerHandle == null )
      || ( pBuffer == null )
      || ( nBufferMaxLength == 0 ) )
   {
      PDebugError("PReaderDriverRegister: null pointers");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the priority */
   if (  ( nPriority != W_PRIORITY_EXCLUSIVE )
      && ( nPriority != W_PRIORITY_SE)
      && ( nPriority != W_PRIORITY_SE_FORCED)
      && ( ( nPriority < W_PRIORITY_MINIMUM ) || ( nPriority > W_PRIORITY_MAXIMUM )) )
   {
      PDebugError("PReaderDriverRegister: bad priority");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }
   /* Check the detection configuration value */
   if(   (nDetectionConfigurationLength != 0)
      && (nPriority != W_PRIORITY_EXCLUSIVE))
   {
      PDebugError("PReaderDriverRegister: Detection configuration set for shared listener");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }
   if((nDetectionConfigurationLength > nBufferMaxLength) ||
      (nDetectionConfigurationLength > NAL_MESSAGE_MAX_LENGTH))
   {
      PDebugError("PReaderDriverRegister: Wrong lenngth for the detection configuration");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PReaderDriverRegister: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_error;
   }

   if((nError = static_PReaderDriverCheckProtocols(
      pContext, pReaderDriverRegistry, &nRequestedProtocolsBF,
      apRequestedProtocolList, &nRequestedProtocolNumber)) != W_SUCCESS)
   {
      PDebugError("PReaderDriverRegister: wrong requested protocols");
      goto return_error;
   }

   if((nError = static_PReaderDriverCheckPriority(
      pContext, pReaderDriverRegistry, nPriority, nRequestedProtocolsBF)) != W_SUCCESS)
   {
      PDebugError("PReaderDriverRegister: wrong priority");
      goto return_error;
   }

   /* Create the registry structure */
   pDriverListener = (tPRegistryDriverReaderListener*)CMemoryAlloc( sizeof(tPRegistryDriverReaderListener) );
   if ( pDriverListener == null )
   {
      PDebugError("PReaderDriverRegister: pDriverListener == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pDriverListener, 0, sizeof(tPRegistryDriverReaderListener));

   /* Store the registry information */
   pDriverListener->bFromUser = bFromUser;
   pDriverListener->nNotifiedProtocolsBF = 0;
   pDriverListener->nPriority = nPriority;
   pDriverListener->pBuffer = pBuffer;
   pDriverListener->nBufferMaxLength = nBufferMaxLength;
   pDriverListener->nRequestedProtocolsBF = nRequestedProtocolsBF;

   if(nDetectionConfigurationLength > 0)
   {
      uint32_t nCommandLength = 0;

      if(nRequestedProtocolNumber != 1)
      {
         PDebugError("PReaderDriverRegister: Cannot set a detection configuration for several protocols");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }

      pReaderProtocol = apRequestedProtocolList[0];

      if( pReaderProtocol->pConstant->pSetDetectionConfiguration == null)
      {
         PDebugTrace("PReaderDriverRegister: No configuration function available");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
      }

      if((pReaderProtocol->nSetDetectionConfigurationState == P_READER_CONFIG_STATE_GENERIC_PENDING)
      || (pReaderProtocol->nSetDetectionConfigurationState == P_READER_CONFIG_STATE_SPECIFIC_PENDING))
      {
         PDebugTrace("PReaderDriverRegister: Set configuration is already pending");
         nError = W_ERROR_EXCLUSIVE_REJECTED;
         goto return_error;
      }

      /* check parameters and build command message */
      if((nError = pReaderProtocol->pConstant->pSetDetectionConfiguration(
         pContext,
         pReaderProtocol->aSetDetectionConfigurationCommandBuffer,
         &nCommandLength,
         pBuffer,
         nDetectionConfigurationLength)) != W_SUCCESS)
      {
         PDebugError("PReaderDriverRegister: Error returned by pSetDetectionConfiguration");
         goto return_error;
      }

      /* Set the detection configuration value */
      pReaderProtocol->nDetectionConfigurationLength = nDetectionConfigurationLength;
      CMemoryCopy(pReaderProtocol->aDetectionConfiguration,
         pBuffer, nDetectionConfigurationLength);

      if (nRequestedProtocolsBF != W_NFCC_PROTOCOL_READER_P2P_INITIATOR)
      {
         uint8_t nServiceId;

         PDebugTrace("PReaderDriverRegister: sending NFC HAL set reader config command");

         nServiceId = pReaderProtocol->pConstant->nServiceIdentifier;

         if (nServiceId == NAL_SERVICE_READER_14_B_4)
         {
            nServiceId = NAL_SERVICE_READER_14_B_3;
         }

         PNALServiceSetParameter(
            pContext,
            nServiceId,
            &pReaderProtocol->sDetectionConfigurationOperation,
            NAL_PAR_READER_CONFIG,
            pReaderProtocol->aSetDetectionConfigurationCommandBuffer,
            nCommandLength,
            static_PReaderDriverSetParameterCompleted,
            pReaderProtocol );
      }
      else
      {
           PDebugTrace("PReaderDriverRegister: sending NFC HAL set P2P initiator config command");

           PNALServiceSetParameter(
            pContext,
            pReaderProtocol->pConstant->nServiceIdentifier,
            &pReaderProtocol->sDetectionConfigurationOperation,
            NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS,
            pReaderProtocol->aSetDetectionConfigurationCommandBuffer,
            nCommandLength,
            static_PReaderDriverSetParameterCompleted,
            pReaderProtocol );
      }

      pReaderProtocol->nSetDetectionConfigurationState = P_READER_CONFIG_STATE_SPECIFIC_PENDING;
      bSetConfigurationPending = W_TRUE;
   }
   else
   {
      uint32_t nPos;


      for(nPos = 0; nPos < nRequestedProtocolNumber; nPos++)
      {
         pReaderProtocol = apRequestedProtocolList[nPos];

         if( pReaderProtocol->pConstant->pSetDetectionConfiguration != null)
         {
            if((pReaderProtocol->nSetDetectionConfigurationState == P_READER_CONFIG_STATE_GENERIC_PENDING)
            || (pReaderProtocol->nSetDetectionConfigurationState == P_READER_CONFIG_STATE_SPECIFIC_PENDING))
            {
               PDebugTrace("PReaderDriverRegister: Set configuration is already pending");
               nError = W_ERROR_SHARE_REJECTED;
               goto return_error;
            }

            if(pReaderProtocol->nSetDetectionConfigurationState == P_READER_CONFIG_STATE_SPECIFIC)
            {
               uint32_t nCommandLength;

               /* check parameters and build command message */
               if((nError = pReaderProtocol->pConstant->pSetDetectionConfiguration(
                  pContext,
                  pReaderProtocol->aSetDetectionConfigurationCommandBuffer,
                  &nCommandLength,
                  null, 0)) != W_SUCCESS)
               {
                  PDebugError("PReaderDriverRegister: Error returned by pSetDetectionConfiguration");
                  goto return_error;
               }

               if (pReaderProtocol->pConstant->nProtocolBF != W_NFCC_PROTOCOL_READER_P2P_INITIATOR)
               {
                  PDebugTrace("PReaderDriverRegister: sending NFC HAL set reader config command");
                  PNALServiceSetParameter(
                     pContext,
                     pReaderProtocol->pConstant->nServiceIdentifier,
                     &pReaderProtocol->sDetectionConfigurationOperation,
                     NAL_PAR_READER_CONFIG,
                     pReaderProtocol->aSetDetectionConfigurationCommandBuffer,
                     nCommandLength,
                     static_PReaderDriverSetParameterCompleted,
                     pReaderProtocol );
               }
               else
               {
                  PNALServiceSetParameter(
                     pContext,
                     pReaderProtocol->pConstant->nServiceIdentifier,
                     &pReaderProtocol->sDetectionConfigurationOperation,
                     NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS,
                     pReaderProtocol->aSetDetectionConfigurationCommandBuffer,
                     nCommandLength,
                     static_PReaderDriverSetParameterCompleted,
                     pReaderProtocol );
               }

               pReaderProtocol->nDetectionConfigurationLength = 0;
               pReaderProtocol->nSetDetectionConfigurationState = P_READER_CONFIG_STATE_GENERIC_PENDING;
               bSetConfigurationPending = W_TRUE;
            }
         }
      }
   }

   /* Get a reader handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     pDriverListener,
                     P_HANDLE_TYPE_READER_LISTENER_REGISTRY,
                     &hListener) ) != W_SUCCESS )
   {
      PDebugError("PReaderDriverRegister: error on PHandleRegister()");
      goto return_error;
   }

   if (pDriverListener->bFromUser != W_FALSE)
   {
      PDFCDriverFillCallbackContext(
         pContext,
         (tDFCCallback*)pCallback,
         pCallbackParameter,
         &pDriverListener->pListenerDriverCC );
   }
   else
   {
      PDFCFillCallbackContext(
         pContext,
         (tDFCCallback*)pCallback,
         pCallbackParameter,
         &pDriverListener->sListenerCC );
   }

   /* If we are registering a SE listener and the break of the active connection has been requested, close it */

   if ((nPriority == W_PRIORITY_SE_FORCED) && (pReaderDriverRegistry->hCurrentDriverConnection != W_NULL_HANDLE))
   {
      CDebugAssert(bFromUser == W_FALSE);

      PReaderDriverWorkPerformed(pContext, pReaderDriverRegistry->hCurrentDriverConnection, W_FALSE, W_TRUE);
      pReaderDriverRegistry->hCurrentDriverConnection = W_NULL_HANDLE;
   }

   /* Store the registry */
   pDriverListener->pNextDriverListener = pReaderDriverRegistry->pDriverListenerListHead;
   pReaderDriverRegistry->pDriverListenerListHead = pDriverListener;

   /* We need to register to the NFC Controller if there is no detection configuration
    *
    * specific case for P2P_INITIATOR : the reactivation will be postponed later during
    * the configuration of the P2P_TARGET
    */
   if ((bSetConfigurationPending == W_FALSE) && (nRequestedProtocolsBF != W_NFCC_PROTOCOL_READER_P2P_INITIATOR))
   {
      static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);
   }

   *phListenerHandle = hListener;

   PDebugTrace("PReaderDriverRegister: end");
   return W_SUCCESS;

return_error:

   PDebugError("PReaderDriverRegister: returns the error %s", PUtilTraceError(nError));

   if ( phListenerHandle != null )
   {
      *phListenerHandle = W_NULL_HANDLE;
   }

   if ( pDriverListener != W_NULL_HANDLE )
   {
      /* Free the connection buffer */
      CMemoryFree( pDriverListener );
   }

   PDebugTrace("PReaderDriverRegister: end");
   return nError;
}

/* See header file */
uint8_t PReaderDriverGetRFActivity(
                  tContext* pContext)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );

   if(pReaderDriverRegistry->nDetectionState == P_READER_DETECTION_STATE_ACTIVE)
   {
      return W_NFCC_RF_ACTIVITY_ACTIVE;
   }
   else
   {
      if((pReaderDriverRegistry->nCurrentDetectionProtocolBF & W_NFCC_PROTOCOL_READER_ALL) == 0)
      {
         return W_NFCC_RF_ACTIVITY_INACTIVE;
      }
      else
      {
         return W_NFCC_RF_ACTIVITY_DETECTION;
      }
   }
}

/* See Client API Specifications */
W_ERROR PReaderErrorEventRegister(
            tContext* pContext,
            tPBasicGenericEventHandler *pCallback,
            void *pCallbackParameter,
            uint8_t nEventType,
            bool_t bCardDetectionRequested,
            W_HANDLE* phRegistryHandle )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tErrorRegistry* pErrorRegistry;
   W_ERROR nError;
   W_HANDLE hHandle;

   PDebugTrace("PReaderErrorEventRegister(nEventType=%d)", nEventType);

   /* Check the parameters */
   if ( phRegistryHandle == null )
   {
      PDebugError("PReaderErrorEventRegister: phRegistryHandle is null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }
   /* Set the default value */
   *phRegistryHandle = W_NULL_HANDLE;

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PReaderErrorEventRegister: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_error;
   }

   switch ( nEventType )
   {
      case W_READER_ERROR_UNKNOWN:
         pErrorRegistry = &pReaderDriverRegistry->sUnknownRegistry;
         break;

      case W_READER_ERROR_COLLISION:
         pErrorRegistry = &pReaderDriverRegistry->sCollisionRegistry;
         break;

      case W_READER_MULTIPLE_DETECTION:
         pErrorRegistry = &pReaderDriverRegistry->sMultipleDetectionRegistry;
         break;

      default:
         PDebugError("PReaderErrorEventRegister: Bad error type");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
   }

   /* Check if a callback has already been registered */
   if ( pErrorRegistry->bIsRegistered != W_FALSE )
   {
      PDebugError("PReaderErrorEventRegister: callback already registered");
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   /* Get a reader handle */
   if ( ( nError = PHandleRegister(
                        pContext,
                        pErrorRegistry,
                        P_HANDLE_TYPE_ERROR,
                        &hHandle) ) != W_SUCCESS )
   {
      PDebugError("PReaderErrorEventRegister: error in PHandleRegister()" );
      goto return_error;
   }

   pErrorRegistry->bIsRegistered = W_TRUE;

   pErrorRegistry->bCardDetectionRequested = bCardDetectionRequested;

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pErrorRegistry->pErrorListenerDriverCC );

   /* The detection may be started */
   if(bCardDetectionRequested != W_FALSE)
   {
      static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);
   }

   *phRegistryHandle = hHandle;

   PDebugTrace("PReaderErrorEventRegister: end");
   return W_SUCCESS;

return_error:

   PDebugTrace("PReaderErrorEventRegister: end returning error %s", PUtilTraceError(nError));

   return nError;
}

static void static_PReaderDriverInternalWorkPerformed(
                  tContext* pContext,
                  bool_t bGiveToNextListener,
                  bool_t bCardApplicationMatch )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tPRegistryDriverReaderListener* pDriverListener = pReaderDriverRegistry->pCurrentDriverListener;
   tNFCControllerSEPolicy sVolatilePolicy;

   PDebugTrace("      @InternalWorkPerformed( bGiveToNextListener=%s, bCardApplicationMatch=%s )",
      PUtilTraceBool(bGiveToNextListener), PUtilTraceBool(bCardApplicationMatch));

   CDebugAssert(pDriverListener != null);

   /* Special case, if the SE is being used, force the value of the flags */
   /* Use the current volatile policy, because the new policy is modified for the pending close operation */
   if(PNFCControllerGetSESwitchPosition(
         pContext, null, &sVolatilePolicy, null, null) == W_SUCCESS)
   {
      if((sVolatilePolicy.nSESwitchPosition == P_SE_SWITCH_FORCED_HOST_INTERFACE)
      || (sVolatilePolicy.nSESwitchPosition == P_SE_SWITCH_HOST_INTERFACE))
      {
         PDebugTrace("      @InternalWorkPerformed : forcing bCardApplicationMatch and bCardApplicationMatch due to SE presence");

         bGiveToNextListener = W_FALSE;
         bCardApplicationMatch = W_TRUE;
      }
   }

   /* Set the result in the listener instance */
   pDriverListener->nNotifiedProtocolsBF |= pReaderDriverRegistry->pLastCard->nProtocolBF;

   if(bCardApplicationMatch != W_FALSE)
   {
      pReaderDriverRegistry->bCardApplicationMatch = W_TRUE;
   }

   /* If the application polling is requested */
   if ( bGiveToNextListener != W_FALSE )
   {
      if(static_PReaderDriverGetNextListener(
            pContext, pReaderDriverRegistry, pReaderDriverRegistry->pLastCard) == null)
      {
         PDebugTrace("      @InternalWorkPerformed: no other listener");
         bGiveToNextListener = W_FALSE;
      }
   }
   else
   {
      tPRegistryDriverReaderListener* pDriverListenerLoop;

      /* Disable the remaining listeners */
      for(  pDriverListenerLoop = pReaderDriverRegistry->pDriverListenerListHead;
            pDriverListenerLoop != null;
            pDriverListenerLoop = pDriverListenerLoop->pNextDriverListener)
      {
         if (pDriverListenerLoop->nRequestedProtocolsBF & pReaderDriverRegistry->pLastCard->nProtocolBF)
         {
            pDriverListenerLoop->nNotifiedProtocolsBF |= pReaderDriverRegistry->pLastCard->nProtocolBF;
         }
      }
   }

   if ( bGiveToNextListener == W_FALSE )
   {
      static_PReaderDriverNotifyUnknownTargetHandler( pContext, pReaderDriverRegistry );
   }

   static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_TRUE);

   PDebugTrace("      @InternalWorkPerformed: end");
}

/* See header file */
void PReaderDriverSetAntiReplayReference(
            tContext* pContext)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );

   PDebugTrace("PReaderDriverSetAntiReplayReference()");

   /* Reset the replay timer */
   pReaderDriverRegistry->nTimeReference = static_PReaderDriverGetDejaVu(
               pContext, P_READER_DETECTION_TIMEOUT );
}

void static_PReaderDriverSetAntiReplayReferenceIfNotAlreadyElapsed(
            tContext* pContext)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   uint32_t nNewTimeout = pReaderDriverRegistry->nTimeReference;

   if (static_PReaderDriverCheckDejaVu(pContext, P_READER_DETECTION_TIMEOUT, &nNewTimeout) == W_TRUE)
   {
      PDebugTrace("static_PReaderDriverSetAntiReplayReferenceIfNotAlreadyElapsed: restarts the Timeout");
      PReaderDriverSetAntiReplayReference(pContext);
   }
   else
   {
      PDebugTrace("static_PReaderDriverSetAntiReplayReferenceIfNotAlreadyElapsed: DO NOT restart the Timeout");
   }
}

static uint32_t static_PReaderDriverConnectionDestroy(
            tContext* pContext,
            void* pObject )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );

   PDebugTrace("static_PReaderDriverConnectionDestroy");

   pReaderDriverRegistry->hCurrentDriverConnection = W_NULL_HANDLE;

   if(pReaderDriverRegistry->pCurrentDriverListener != null)
   {
      static_PReaderDriverInternalWorkPerformed(pContext, W_TRUE, W_FALSE);
   }

   PDebugTrace("static_PReaderDriverConnectionDestroy: end");

   return P_HANDLE_DESTROY_DONE;
}

W_ERROR PReaderDriverRedetectCard(
                  tContext * pContext,
                  W_HANDLE hConnection)
{
   tPReaderDriverRegistry * pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   void                   * pConnection;
   W_ERROR nError;

   PDebugTrace("PReaderDriverRedetectCard()");

   nError = PHandleGetObject( pContext, hConnection, P_HANDLE_TYPE_READER_DRIVER_CONNECTION, &pConnection);

   if (pConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverRedetectCard: end PHandleGetObject() return %s", PUtilTraceError(nError));
      return nError;
   }

   CDebugAssert(pConnection == &PContextGetReaderDriverRegistry(pContext)->sReaderConnectionObjectHeader);

   /* We want to redetect the same card, so set replay reference - this is typically needed for redetection of cards after timeout */
   PReaderDriverSetAntiReplayReference(pContext);

   static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_TRUE);

   PHandleClose(pContext, hConnection);

   PDebugTrace("PReaderDriverRedetectCard: end");

   return W_SUCCESS;
}

/* See header file */
W_ERROR PReaderDriverWorkPerformed(
                  tContext* pContext,
                  W_HANDLE hConnection,
                  bool_t bGiveToNextListener,
                  bool_t bCardApplicationMatch )
{
   void* pConnection;
   W_ERROR nError = PHandleGetObject( pContext, hConnection,
            P_HANDLE_TYPE_READER_DRIVER_CONNECTION, &pConnection);

   PDebugTrace(
      "PReaderDriverWorkPerformed( bGiveToNextListener=%s, bCardApplicationMatch=%s )",
      PUtilTraceBool(bGiveToNextListener), PUtilTraceBool(bCardApplicationMatch));

   if (pConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverWorkPerformed: end PHandleGetObject() return %s", PUtilTraceError(nError));
      return nError;
   }

   CDebugAssert(pConnection == &PContextGetReaderDriverRegistry(pContext)->sReaderConnectionObjectHeader);

   static_PReaderDriverInternalWorkPerformed(
      pContext, bGiveToNextListener, bCardApplicationMatch );

   PHandleClose(pContext, hConnection);

   PDebugTrace("PReaderDriverWorkPerformed: end");

   return W_SUCCESS;
}

/* See header file */

W_ERROR PReaderDriverSetWorkPerformedAndClose(
                  tContext* pContext,
                  W_HANDLE hDriverListener)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tPRegistryDriverReaderListener* pDriverListener;
   W_ERROR nError;

   PDebugTrace("PReaderDriverSetWorkPerformedAndClose");

   nError = PHandleGetObject(pContext, hDriverListener, P_HANDLE_TYPE_READER_LISTENER_REGISTRY, (void **) & pDriverListener);

   if (nError == W_SUCCESS)
   {
      if ((pReaderDriverRegistry->pCurrentDriverListener == pDriverListener) && (pDriverListener != null))
      {
         PHandleClose(pContext, pReaderDriverRegistry->hCurrentDriverConnection);
         pReaderDriverRegistry->hCurrentDriverConnection = W_NULL_HANDLE;
      }
   }
   else
   {
      PDebugError("PReaderDriverSetWorkPerformedAndClose :  PHandleGetObject returned %s", PUtilTraceError(nError));
      return nError;
   }

   /* Close the handle */
   PHandleClose(pContext, hDriverListener);

   PDebugTrace("PReaderDriverSetWorkPerformedAndClose: end");

   return W_SUCCESS;
}


/**
 * See  PNALServiceRegisterForEvent().
 **/
static void static_PReaderDriverCardEmulationEventHandler(
            tContext* pContext,
            void* pCallbackParameter,
            uint8_t nEventIdentifier,
            const uint8_t* pBuffer,
            uint32_t nLength,
            uint32_t nNALMessageReceptionCounter )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tPRegistryDriverCardProtocol* pCardProtocol = (tPRegistryDriverCardProtocol*)pCallbackParameter;

   PDebugTrace(
      "static_PReaderDriverCardEmulationEventHandler: nEventIdentifier 0x%02X (nNALMessageReceptionCounter %d)",
      nEventIdentifier, nNALMessageReceptionCounter );
   //jim add
   if (nEventIdentifier == 0x8A)
   {
       pGerenalByte.nGerenalByteLength = nLength;
       memcpy(pGerenalByte.aGerenalByte, pBuffer, nLength);
       PDebugTrace("static_PReaderDriverCardEmulationEventHandler[jim]: buffer= ");      
	   PDebugTraceBuffer(pGerenalByte.aGerenalByte, nLength);
   }
   if (pReaderDriverRegistry->nStoredEmulMessageNb >= MAX_STORED_EMUL_MESSAGE_NB)
   {
      /* no room to store this message */
      PDebugWarning("static_PReaderDriverCardEmulationEventHandler: The last card emulation event is ignored#1");
   }

   if(pReaderDriverRegistry->nCardEmulationDetectionCommandMessageCounter != 0)
   {
      if(pReaderDriverRegistry->nCardEmulationDetectionCommandMessageCounter <= nNALMessageReceptionCounter)
      {
         pCardProtocol->pConstant->pEventReceived(
                  pContext, pCardProtocol->pCallbackParameter,
                  pCardProtocol->pConstant->nProtocolBF,
                  nEventIdentifier,
                  pBuffer, nLength );

         /* update replay reference only if not already elapsed */
         static_PReaderDriverSetAntiReplayReferenceIfNotAlreadyElapsed(pContext);
      }
      else
      {
         PDebugWarning("static_PReaderDriverCardEmulationEventHandler: The last card emulation event is ignored#2");
      }
   }
   else
   {
      if(nLength > NAL_MESSAGE_MAX_LENGTH)
      {
         PDebugError("static_PReaderDriverCardEmulationEventHandler: message too loong");
         PNFCControllerNotifyException(pContext, W_NFCC_EXCEPTION_NAL_PROTOCOL_ERROR);
         PDebugTrace("static_PReaderDriverCardEmulationEventHandler: end");
         return;
      }

      if (pReaderDriverRegistry->nStoredEmulMessageNb < MAX_STORED_EMUL_MESSAGE_NB)
      {

         /* Keep the card emulation message until the start completion is received */
         PDebugWarning("static_PReaderDriverCardEmulationEventHandler: store card emulation message");

         pReaderDriverRegistry->sStoredCardEmulationMessage[pReaderDriverRegistry->nStoredEmulMessageNb].nMessageCounter = nNALMessageReceptionCounter;
         pReaderDriverRegistry->sStoredCardEmulationMessage[pReaderDriverRegistry->nStoredEmulMessageNb].pCardProtocol = pCardProtocol;
         pReaderDriverRegistry->sStoredCardEmulationMessage[pReaderDriverRegistry->nStoredEmulMessageNb].nEventIdentifier = nEventIdentifier;
         pReaderDriverRegistry->sStoredCardEmulationMessage[pReaderDriverRegistry->nStoredEmulMessageNb].nBufferLength = nLength;

         if(nLength != 0)
         {
            CMemoryCopy(pReaderDriverRegistry->sStoredCardEmulationMessage[pReaderDriverRegistry->nStoredEmulMessageNb].aBuffer,
               pBuffer, nLength);
         }

         pReaderDriverRegistry->nStoredEmulMessageNb++;
      }
   }

   PDebugTrace("static_PReaderDriverCardEmulationEventHandler: end");
}

/**
 * Frees the protocol lists.
 *
 * @param[in)  pContext  The current context.
 *
 * @param[in]  pReaderDriverRegistry  The registry.
 **/
static void static_PReaderDriverFreeProtocolLists(
         tContext* pContext,
         tPReaderDriverRegistry* pReaderDriverRegistry )
{
   tPRegistryDriverReaderProtocol* pReaderProtocol;
   tPRegistryDriverCardProtocol* pCardProtocol;

   pReaderProtocol = pReaderDriverRegistry->pReaderProtocolListHead;

   while(pReaderProtocol != null)
   {
      tPRegistryDriverReaderProtocol* pNextReaderProtocol = pReaderProtocol->pNext;

      PNALServiceCancelOperation( pContext,
         &pReaderProtocol->sDetectionOperationEvent);

      PNALServiceCancelOperation( pContext,
         &pReaderProtocol->sDetectionConfigurationOperation);

      CMemoryFree(pReaderProtocol);

      pReaderProtocol = pNextReaderProtocol;
   }
   pReaderDriverRegistry->pReaderProtocolListHead = null;

   pCardProtocol = pReaderDriverRegistry->pCardProtocolListHead;

   while(pCardProtocol != null)
   {
      tPRegistryDriverCardProtocol* pNextCardProtocol = pCardProtocol->pNext;

      PNALServiceCancelOperation( pContext,
         &pCardProtocol->sOperationEvent);

      PNALServiceCancelOperation( pContext,
         &pCardProtocol->sRegisterOperation);

      CMemoryFree(pCardProtocol);

      pCardProtocol = pNextCardProtocol;
   }

   pReaderDriverRegistry->pCardProtocolListHead = null;
}

/* See header file */
W_ERROR PReaderDriverConnect(
         tContext* pContext,
         uint32_t nProtocolCapabilitiesBF)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   uint32_t nPos;
   tPRegistryDriverReaderProtocol* pReaderProtocol;
   tPRegistryDriverCardProtocol* pCardProtocol;

   if(pReaderDriverRegistry->bIsConnected == W_FALSE)
   {
      for(nPos = 0; nPos < P_READER_PROTOCOL_NUMBER; nPos++)
      {
         if((g_aReaderProtocolConstantlArray[nPos]->nProtocolBF & nProtocolCapabilitiesBF) != 0)
         {
            pReaderProtocol =
               (tPRegistryDriverReaderProtocol*)CMemoryAlloc(sizeof(tPRegistryDriverReaderProtocol));

            if(pReaderProtocol == null)
            {
               PDebugError("PReaderDriverConnect: Cannot allocate the reader protocol structure");
               static_PReaderDriverFreeProtocolLists(pContext, pReaderDriverRegistry);
               return W_ERROR_OUT_OF_RESOURCE;
            }

            CMemoryFill(pReaderProtocol, 0, sizeof(tPRegistryDriverReaderProtocol));

            pReaderProtocol->pConstant = g_aReaderProtocolConstantlArray[nPos];

            pReaderProtocol->pNext = pReaderDriverRegistry->pReaderProtocolListHead;
            pReaderDriverRegistry->pReaderProtocolListHead = pReaderProtocol;

            /* Register for event */
            PNALServiceRegisterForEvent(
               pContext,
               pReaderProtocol->pConstant->nServiceIdentifier,
               P_NFC_HAL_EVENT_FILTER_ANY,
               &pReaderProtocol->sDetectionOperationEvent,
               static_PReaderDriverTargetDetectionEventHandler,
               PUtilConvertUintToPointer(pReaderProtocol->pConstant->nProtocolBF) );
         }
      }

      for(nPos = 0; nPos < P_CARD_PROTOCOL_NUMBER; nPos++)
      {
         if((g_aCardProtocolConstantlArray[nPos]->nProtocolBF & nProtocolCapabilitiesBF) != 0)
         {
            pCardProtocol =
               (tPRegistryDriverCardProtocol*)CMemoryAlloc(sizeof(tPRegistryDriverCardProtocol));

            if(pCardProtocol == null)
            {
               PDebugError("PReaderDriverConnect: Cannot allocate the card protocol structure");
               static_PReaderDriverFreeProtocolLists(pContext, pReaderDriverRegistry);
               return W_ERROR_OUT_OF_RESOURCE;
            }

            CMemoryFill(pCardProtocol, 0, sizeof(tPRegistryDriverCardProtocol));

            pCardProtocol->pConstant = g_aCardProtocolConstantlArray[nPos];

            pCardProtocol->pNext = pReaderDriverRegistry->pCardProtocolListHead;
            pReaderDriverRegistry->pCardProtocolListHead = pCardProtocol;

            /* Register for event */
            PNALServiceRegisterForEvent(
               pContext,
               pCardProtocol->pConstant->nServiceIdentifier,
               P_NFC_HAL_EVENT_FILTER_ANY,
               &pCardProtocol->sOperationEvent,
               static_PReaderDriverCardEmulationEventHandler, pCardProtocol );
         }
      }

      /* Set the default value for the pulse period */
      pReaderDriverRegistry->sPulsePeriodMonitor.nRegisterValue = NAL_PAR_DETECT_PULSE_DEFAULT_VALUE;

      pReaderDriverRegistry->bIsConnected = W_TRUE;
   }

   return W_SUCCESS;
}

/* See header file */
void PReaderDriverDisconnect(
         tContext* pContext )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tPRegistryDriverReaderListener* pDriverListener;
   uint32_t i;

   if(pReaderDriverRegistry->bIsConnected != W_FALSE)
   {
      PNALServiceCancelOperation( pContext,
         &pReaderDriverRegistry->sDetectionCommandOperation);

      static_PReaderDriverFreeProtocolLists( pContext, pReaderDriverRegistry );

      pReaderDriverRegistry->nCurrentDetectionProtocolBF = 0;
      pReaderDriverRegistry->nReaderDetectionCommandMessageCounter = 0;
      pReaderDriverRegistry->nCardEmulationDetectionCommandMessageCounter = 0;
      pReaderDriverRegistry->nDetectionState = P_READER_DETECTION_STATE_DETECTION;
      pReaderDriverRegistry->bCardApplicationMatch = W_FALSE;
      pReaderDriverRegistry->sStoredTargetDetectionMessage.nMessageCounter = 0;
      pReaderDriverRegistry->nStoredEmulMessageNb = 0;

      PDebugWarning(" nStoredEmulMessageNb = 0");

      for (i=0; i<MAX_STORED_EMUL_MESSAGE_NB; i++)
      {
         pReaderDriverRegistry->sStoredCardEmulationMessage[i].nMessageCounter = 0;
      }

      pDriverListener = pReaderDriverRegistry->pCurrentDriverListener;
      if(pDriverListener != null)
      {
         /* Must be done before decrement, because of the possible destruction of the object */
         pReaderDriverRegistry->pCurrentDriverListener = null;

         /* Decrement the ref count of the listenner */
         /* Warning: this may cause:
              - the destruction of the listener
              - the modification of the listener list
         */
         PHandleDecrementReferenceCount(pContext, pDriverListener);
      }

      pReaderDriverRegistry->pDriverListenerListHead = null;

      PMultiTimerCancel( pContext, TIMER_T10_READER_DETECTION );

      pReaderDriverRegistry->bIsConnected = W_FALSE;
   }
}

/* See header file */
void PReaderDriverRegistryCreate(
            tPReaderDriverRegistry* pReaderDriverRegistry )
{
   CMemoryFill( pReaderDriverRegistry, 0, sizeof(tPReaderDriverRegistry) );
}

/* See header file */
void PReaderDriverRegistryDestroy(
            tPReaderDriverRegistry* pReaderDriverRegistry )
{
   tPRegistryDriverReaderListener* pDriverListenerCurrent;
   tPRegistryDriverReaderListener* pDriverListenerNext;

   if ( pReaderDriverRegistry != null )
   {
      /* Go through the reader instance */
      pDriverListenerCurrent = pReaderDriverRegistry->pDriverListenerListHead;
      while ( pDriverListenerCurrent != null )
      {
         /* Get the next connection */
         pDriverListenerNext = pDriverListenerCurrent->pNextDriverListener;
         /* Free the buffer */
         CMemoryFree( pDriverListenerCurrent );
         pDriverListenerCurrent = pDriverListenerNext;
      }

      CMemoryFill( pReaderDriverRegistry, 0, sizeof(tPReaderDriverRegistry) );
   }
}

/* -----------------------------------------------------------------------------

   Pulse Period Functions

----------------------------------------------------------------------------- */

/* See Client API Specifications */
W_ERROR PReaderGetPulsePeriod(
      tContext* pContext,
      uint32_t * pnTimeout )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tNFCController* pNFCController = PContextGetNFCController( pContext );

   if(pnTimeout == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if (pNFCController->bPulsePeriodSupported == W_FALSE)
   {
      return W_ERROR_FEATURE_NOT_SUPPORTED;
   }

   *pnTimeout = pReaderDriverRegistry->sPulsePeriodMonitor.nRegisterValue;

   return W_SUCCESS;
}

static void static_PReaderDriverSetPulsePeriodCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tNFCControllerPulsePeriodMonitor* pPulse = (tNFCControllerPulsePeriodMonitor*)pCallbackParameter;

   CDebugAssert(pPulse->bOperationPending != W_FALSE);

   pPulse->bOperationPending = W_FALSE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PReaderDriverSetPulsePeriodCompleted: Error %s",
         PUtilTraceError(nError));
   }
   else
   {
      pPulse->nRegisterValue = pPulse->nNewRegisterValue;
   }

   PDFCDriverPostCC2(pPulse->pDriverCC, nError);
}

/* See Client API Specifications */
void PReaderSetPulsePeriodDriver(
      tContext* pContext,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint32_t nPulsePeriod )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tNFCControllerPulsePeriodMonitor* pPulse = &pReaderDriverRegistry->sPulsePeriodMonitor;
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError = W_SUCCESS;

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_function;
   }

   if(nPulsePeriod == pPulse->nRegisterValue)
   {
      PDebugTrace("PReaderSetPulsePeriod: Value already set");
      goto return_function;
   }

   if(pPulse->bOperationPending != W_FALSE)
   {
      PDebugTrace("PReaderSetPulsePeriod: Operation already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_function;
   }

   pPulse->bOperationPending = W_TRUE;

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pPulse->pDriverCC );

   if (nPulsePeriod <= 0xFFFF)
   {
      pPulse->nNewRegisterValue = (uint16_t) nPulsePeriod;
   }
   else
   {
      pPulse->nNewRegisterValue = 0xFFFF;
   }

   PNALWriteUint16ToBuffer(pPulse->nNewRegisterValue, pPulse->aSetParameterData);

   PNALServiceSetParameter(
      pContext,
      NAL_SERVICE_ADMIN,
      &pPulse->sServiceOperation,
      NAL_PAR_DETECT_PULSE,
      pPulse->aSetParameterData, 2,
      static_PReaderDriverSetPulsePeriodCompleted,
      pPulse );

   return;

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderSetPulsePeriod: Error %s", PUtilTraceError(nError));
   }

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pDriverCC );

   PDFCDriverPostCC2(pDriverCC, nError);
}

/* See header file */
W_ERROR PReaderDriverGetConnectionObject(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tHandleType* pExpectedType,
            void** ppObject)
{
   if(hDriverConnection != W_NULL_HANDLE)
   {
      W_ERROR nError = PHandleGetObject(pContext, hDriverConnection, pExpectedType, ppObject);

      if ( ( nError == W_SUCCESS ) && ( *ppObject != null ) )
      {
         return W_SUCCESS;
      }
   }

   return W_ERROR_BAD_HANDLE;
}

/**
 * Receives the set parameter result for NAL_PAR_CARD_CONFIG
 *
 **/

static void static_PReaderDriverCardSetParameterCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tPRegistryDriverCardProtocol* pCardProtocol = (tPRegistryDriverCardProtocol*)pCallbackParameter;
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );

   PDebugTrace("static_PReaderDriverCardSetParameterCompleted()" );

   CDebugAssert(pCardProtocol->bCardEmulationInUse != W_FALSE);
   CDebugAssert(pCardProtocol->bCardEmulationConfigured == W_FALSE);

   if(nError == W_SUCCESS)
   {
      pCardProtocol->bCardEmulationConfigured = W_TRUE;

      static_PReaderDriverReactivateDetection(
            pContext, pReaderDriverRegistry, W_FALSE);
   }
   else
   {
      PDebugTrace("static_PReaderDriverCardSetParameterCompleted: receive nError=%s",
         PUtilTraceError(nError));

      pCardProtocol->bCardEmulationInUse = W_FALSE;
      pCardProtocol->nLastParametersLength = 0;
   }

   PDFCPostContext2(
      &pCardProtocol->sRegisterCallbackContext,
      nError );
}

/* See header file */
void PReaderDriverRegisterCardEmulation(
                  tContext* pContext,
                  uint32_t nProtocolBF,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pParameterBuffer,
                  uint32_t nParameterLength)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tPRegistryDriverCardProtocol* pCardProtocol;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PReaderDriverRegisterCardEmulation( %s )",
      PUtilTraceCardProtocol(pContext, nProtocolBF));

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &sCallbackContext );

   pCardProtocol = pReaderDriverRegistry->pCardProtocolListHead;
   while(pCardProtocol != null)
   {
      if(pCardProtocol->pConstant->nProtocolBF == nProtocolBF)
      {
         break;
      }
      pCardProtocol = pCardProtocol->pNext;
   }

   if(pCardProtocol == null)
   {
      PDebugError("PReaderDriverRegisterCardEmulation: Protocol not supported");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if(pCardProtocol->bCardEmulationInUse != W_FALSE)
   {
      PDebugError("PReaderDriverRegisterCardEmulation: Card emulation already used");
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   pCardProtocol->bCardEmulationInUse = W_TRUE;
   pCardProtocol->bCardEmulationConfigured = W_FALSE;
   pCardProtocol->pCallbackParameter = pCallbackParameter;

   pCardProtocol->sRegisterCallbackContext = sCallbackContext;

   /* Check the last parameters */
   if(pCardProtocol->nLastParametersLength == nParameterLength)
   {
      if(CMemoryCompare(
               pParameterBuffer,
               pCardProtocol->aLastParameters,
               nParameterLength) != 0)
      {
         pCardProtocol->nLastParametersLength = 0;
      }
   }
   else
   {
      pCardProtocol->nLastParametersLength = 0;
   }

   if(pCardProtocol->nLastParametersLength == 0)
   {
      PDebugTrace("PReaderDriverRegisterCardEmulation: New parameters need to be set");

      CMemoryCopy(
               pCardProtocol->aLastParameters,
               pParameterBuffer,
               nParameterLength);

      pCardProtocol->nLastParametersLength = nParameterLength;

      /* Set the parameters */
      PNALServiceSetParameter(
         pContext,
         pCardProtocol->pConstant->nServiceIdentifier,
         &pCardProtocol->sRegisterOperation,
         NAL_PAR_CARD_CONFIG,
         pParameterBuffer, nParameterLength,
         static_PReaderDriverCardSetParameterCompleted, pCardProtocol );
   }
   else
   {
      PDebugTrace("PReaderDriverRegisterCardEmulation: Parameters already set");

      /* Direct call */
      static_PReaderDriverCardSetParameterCompleted(
         pContext, pCardProtocol, W_SUCCESS );
   }

   return;

return_error:

   PDebugError("PReaderDriverRegisterCardEmulation: return %s", PUtilTraceError(nError));

   PDFCPostContext2(
      &sCallbackContext,
      nError );
}

/* See header file */
void PReaderDriverUnregisterCardEmulation(
                  tContext* pContext,
                  uint32_t nProtocolBF,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   tPRegistryDriverCardProtocol* pCardProtocol;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PReaderDriverUnregisterCardEmulation( %s )",
      PUtilTraceCardProtocol(pContext, nProtocolBF));

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &sCallbackContext );

   pCardProtocol = pReaderDriverRegistry->pCardProtocolListHead;
   while(pCardProtocol != null)
   {
      if(pCardProtocol->pConstant->nProtocolBF == nProtocolBF)
      {
         break;
      }
      pCardProtocol = pCardProtocol->pNext;
   }

   if(pCardProtocol == null)
   {
      PDebugError("PReaderDriverUnregisterCardEmulation: Protocol not supported");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_function;
   }

   if(pCardProtocol->bCardEmulationInUse == W_FALSE)
   {
      PDebugError("PReaderDriverUnregisterCardEmulation: Card emulation not used");
      nError = W_ERROR_BAD_STATE;
      goto return_function;
   }

   pCardProtocol->bCardEmulationInUse = W_FALSE;
   pCardProtocol->bCardEmulationConfigured = W_FALSE;

   nError = W_SUCCESS;

   static_PReaderDriverReactivateDetection(
      pContext, pReaderDriverRegistry, W_FALSE);

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError("PReaderDriverUnregisterCardEmulation: return %s", PUtilTraceError(nError));
   }

   PDFCPostContext2(
      &sCallbackContext,
      nError );
}


/* See header file*/
void PReaderDriverRegisterP2PDataIndicationCallback(
                  tContext * pContext,
                  tDFCCallback * pCallback,
                  void * pCallbackParameter)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );

   PDebugTrace("PReaderDriverRegisterP2PDataIndicationCallback");

   PDFCFillCallbackContext(pContext, pCallback, pCallbackParameter, & pReaderDriverRegistry->sReceptionCC);

}

/* See header file*/
void PReaderDriverUnregisterP2PDataIndicationCallback(
                  tContext * pContext
                  )
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );
   PDebugTrace("PReaderDriverUnregisterP2PDataIndicationCallback");

   PDFCFlushCall(& pReaderDriverRegistry->sReceptionCC);
}

/* See header file */
void PReaderDriverDisableAllNonSEListeners(
                  tContext * pContext,
                  tPBasicGenericCompletionFunction * pCallback,
                  void * pCallbackParameter)

{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );

   PDebugTrace("PReaderDriverDisableAllNonSEListeners");

   pReaderDriverRegistry->bStopAllActiveDetection = W_TRUE;
   pReaderDriverRegistry->pStopCallback = pCallback;
   pReaderDriverRegistry->pStopCallbackParameter = pCallbackParameter;

   static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);
}

/* See header file */
void PReaderDriverEnableAllNonSEListeners(
                  tContext * pContext)
{
   tPReaderDriverRegistry* pReaderDriverRegistry = PContextGetReaderDriverRegistry( pContext );

   PDebugTrace("PReaderDriverEnableAllNonSEListeners");

   pReaderDriverRegistry->bStopAllActiveDetection = W_FALSE;
   static_PReaderDriverReactivateDetection(pContext, pReaderDriverRegistry, W_FALSE);
}

/* See header file */
uint8_t PReaderDriverGetNbCardDetected(tContext * pContext)
{
   return  PContextGetReaderDriverRegistry( pContext )->nNbCardDetected;
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

