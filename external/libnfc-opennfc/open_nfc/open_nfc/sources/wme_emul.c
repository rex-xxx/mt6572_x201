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
   Contains the implementation of the Card Emulation functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( EMUL )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See C API Specifications */
bool_t PEmulIsPropertySupported(
         tContext * pContext,
         uint8_t nPropertyIdentifier )
{
   bool_t bValue;
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   uint32_t nProtocols = pNFCControllerInfo->nProtocolCapabilities;

   switch(nPropertyIdentifier)
   {
   case W_PROP_ISO_14443_4_B:
   case W_PROP_NFC_TAG_TYPE_4_B:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_CARD_ISO_14443_4_B) != 0)?W_TRUE:W_FALSE;
      break;
   case W_PROP_ISO_14443_4_A:
   case W_PROP_NFC_TAG_TYPE_4_A:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_CARD_ISO_14443_4_A) != 0)?W_TRUE:W_FALSE;
      break;
   default:
      return W_FALSE;
   }

   return bValue;
}

/* See C API Specifications */
void WEmulOpenConnection(
            tWBasicGenericCallbackFunction* pOpenCallback,
            void* pOpenCallbackParameter,
            tWBasicGenericEventHandler* pEventCallback,
            void* pEventCallbackParameter,
            tWEmulCommandReceived* pCommandCallback,
            void* pCommandCallbackParameter,
            tWEmulConnectionInfo* pEmulConnectionInfo,
            W_HANDLE* phHandle)
{
   if (phHandle != null)
   {
      /* This value may be set to W_NULL_HANDLE in case of error */
      *phHandle = W_NULL_HANDLE;
   }

   WEmulOpenConnectionDriver1(
      pOpenCallback, pOpenCallbackParameter,
      pEmulConnectionInfo, sizeof(tWEmulConnectionInfo),
      phHandle);

   if((phHandle != null) && (*phHandle != W_NULL_HANDLE))
   {
      if(pEventCallback != null)
      {
         WEmulOpenConnectionDriver2(
            *phHandle,
            pEventCallback, pEventCallbackParameter );
      }

      WEmulOpenConnectionDriver3(
         *phHandle,
         pCommandCallback, pCommandCallbackParameter );
   }
}

/* See Client API Specifications */
W_ERROR WEmulOpenConnectionSync(
            tWBasicGenericEventHandler* pEventCallback,
            void* pEventCallbackParameter,
            tWEmulCommandReceived* pCommandCallback,
            void* pCommandCallbackParameter,
            tWEmulConnectionInfo* pEmulConnectionInfo,
            W_HANDLE *phHandle )
{
   tPBasicGenericSyncParameters param;
   W_ERROR nError;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WEmulOpenConnection(
         PBasicGenericSyncCompletion, &param,
         pEventCallback, pEventCallbackParameter,
         pCommandCallback, pCommandCallbackParameter,
         pEmulConnectionInfo,
         phHandle );
   }

   nError = PBasicGenericSyncWaitForResult(&param);

   if ((nError != W_SUCCESS)  && (phHandle != null))
   {
      WBasicCloseHandle(*phHandle);
      * phHandle = W_NULL_HANDLE;
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR WEmulCloseSync(
         W_HANDLE hHandle)
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WEmulClose(
         hHandle,
         PBasicGenericSyncCompletion, &param );
   }

   return PBasicGenericSyncWaitForResult(&param);
}

static void static_PEmulCloseDriverCompleted(
      tContext* pContext,
      void * pCallbackParam,
      W_ERROR nError)
{
   PDFCPostContext2(pCallbackParam, nError);
   CMemoryFree(pCallbackParam);
}


void PEmulClose(
         tContext* pContext,
         W_HANDLE hHandle,
         tPBasicGenericCallbackFunction * pCallback,
         void * pCallbackParameter )
{
   tDFCCallbackContext * pCallbackContext = CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, pCallbackContext);
      PEmulCloseDriver(pContext, hHandle, static_PEmulCloseDriverCompleted, pCallbackContext);

      nError = PContextGetLastIoctlError(pContext);
      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback*) static_PEmulCloseDriverCompleted, pCallbackContext, &sCallbackContext);
         PDFCPostContext2(&sCallbackContext, nError);
      }
   }
   else
   {
      tDFCCallbackContext sCallbackContext;

      PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
   }
}


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static void static_PEmulGenericOpenConnectionDriver1(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pOpenCallback,
            void* pOpenCallbackParameter,
            const tWEmulConnectionInfo* pEmulConnectionInfo,
            uint32_t nSize,
            W_HANDLE* phHandle,
            bool_t bFromUser);

/* see header */

void PEmulOpenConnectionHelper(
   tContext * pContext,
   tPBasicGenericCallbackFunction* pOpenCallback,
   void* pOpenCallbackParameter,
   tPBasicGenericEventHandler* pEventCallback,
   void* pEventCallbackParameter,
   tPEmulCommandReceived* pCommandCallback,
   void* pCommandCallbackParameter,
   tWEmulConnectionInfo* pEmulConnectionInfo,
   W_HANDLE* phHandle)
{
   static_PEmulGenericOpenConnectionDriver1(
      pContext,
      pOpenCallback, pOpenCallbackParameter,
      pEmulConnectionInfo, sizeof(tWEmulConnectionInfo),
      phHandle,
      W_FALSE);

   if((phHandle != null) && (*phHandle != W_NULL_HANDLE))
   {
      if(pEventCallback != null)
      {
         PEmulOpenConnectionDriver2(
            pContext,
            *phHandle,
            pEventCallback, pEventCallbackParameter );
      }

      PEmulOpenConnectionDriver3(
         pContext,
         *phHandle,
         pCommandCallback, pCommandCallbackParameter );
   }
}


/* Service operation state */
#define P_EMUL_DRIVER_CLOSED        0x00
#define P_EMUL_DRIVER_OPEN_PENDING  0x01
#define P_EMUL_DRIVER_ACTIVE        0x02
#define P_EMUL_DRIVER_SENDING_DATA  0x03
#define P_EMUL_DRIVER_CLOSE_PENDING 0x04
#define P_EMUL_DRIVER_ERROR         0x05

/* Card state */
#define P_EMUL_CARD_STATE_DESELECTED 0x00
#define P_EMUL_CARD_STATE_SELECTED   0x01
#define P_EMUL_CARD_STATE_OTHER      0x02

static void static_PEmulDriverEventReceived(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nProtocolBF,
            uint8_t nEventIdentifier,
            const uint8_t* pBuffer,
            uint32_t nLength );

static void static_PEmulDriverP2PEventReceived(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nProtocolBF,
            uint8_t nEventIdentifier,
            const uint8_t* pBuffer,
            uint32_t nLength );

/* The protocol information structure */
tPRegistryDriverCardProtocolConstant g_sEmulCardProtocolConstantTypeA = {
   W_NFCC_PROTOCOL_CARD_ISO_14443_4_A,
   NAL_SERVICE_CARD_14_A_4,
   static_PEmulDriverEventReceived
   };

/* The protocol information structure */
tPRegistryDriverCardProtocolConstant g_sEmulCardProtocolConstantTypeB = {
   W_NFCC_PROTOCOL_CARD_ISO_14443_4_B,
   NAL_SERVICE_CARD_14_B_4,
   static_PEmulDriverEventReceived
   };

/* The protocol information structure */
tPRegistryDriverCardProtocolConstant g_sEmulCardProtocolConstantP2PTarget = {
   W_NFCC_PROTOCOL_CARD_P2P_TARGET,
   NAL_SERVICE_P2P_TARGET,
   static_PEmulDriverP2PEventReceived
   };

/* Destroy connection callback */
static uint32_t static_PEmulDriverDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Handle connection Card Emulation type */
tHandleType g_sEmulDriverConnection = {  static_PEmulDriverDestroyConnection,
                                         null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_EMUL_DRIVER_CONNECTION (&g_sEmulDriverConnection)

/**
 * @brief   Receives the un-registration result
 **/
static void static_PEmulDriverUnregisterCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tEmulCardInfo* pEmulCardInfo = (tEmulCardInfo*)pCallbackParameter;

   PDebugTrace("static_PEmulDriverUnregisterCompleted()");

   CDebugAssert(pEmulCardInfo->nState == P_EMUL_DRIVER_CLOSE_PENDING);

   if(pEmulCardInfo->bIsCloseCallbackSet)
   {
      if (pEmulCardInfo->bFromUser != W_FALSE)
      {
         PDFCDriverPostCC2(
            pEmulCardInfo->pCloseDriverCC,
            nError );
      }
      else
      {
         PDFCPostContext2(
            & pEmulCardInfo->sCloseCC,
            nError );
      }
   }

   CMemoryFill( pEmulCardInfo, 0, sizeof(tEmulCardInfo) );

   pEmulCardInfo->nState = P_EMUL_DRIVER_CLOSED;
}

/* Destroy connection callback */
static void static_PEmulDriverInitiateDestruction(
            tContext* pContext,
            tEmulCardInfo* pEmulCardInfo )
{
   PDebugTrace("static_PEmulDriverInitiateDestruction()");

   if((pEmulCardInfo->nState == P_EMUL_DRIVER_CLOSE_PENDING)
   || (pEmulCardInfo->nState == P_EMUL_DRIVER_CLOSED))
   {
      PDebugError("static_PEmulDriverInitiateDestruction: destruction already pending");
      return;
   }
   else if((pEmulCardInfo->nState == P_EMUL_DRIVER_OPEN_PENDING)
   || (pEmulCardInfo->nState == P_EMUL_DRIVER_SENDING_DATA))
   {
      PDebugTrace("static_PEmulDriverInitiateDestruction: operation pending, reschedule destruction");
      pEmulCardInfo->bDestructionScheduled = W_TRUE;
      return;
   }

   if (pEmulCardInfo->bFromUser != W_FALSE)
   {
      PDFCDriverFlushCall(pEmulCardInfo->pReceptionDriverCC);
      PDFCDriverFlushCall(pEmulCardInfo->pEventDriverCC);
   }
   else
   {
      PDFCFlushCall(&pEmulCardInfo->sReceptionCC);
      PDFCFlushCall(&pEmulCardInfo->sEventCC);
   }

   PNALServiceCancelOperation(
      pContext,
      &pEmulCardInfo->sServiceOperationSendEvent );

   pEmulCardInfo->nState = P_EMUL_DRIVER_CLOSE_PENDING;
   pEmulCardInfo->bDestructionScheduled = W_FALSE;
   pEmulCardInfo->bReaderDataAvailable = W_FALSE;

   PReaderDriverUnregisterCardEmulation(
         pContext,
         pEmulCardInfo->pConstant->nProtocolBF,
         static_PEmulDriverUnregisterCompleted, pEmulCardInfo );
}

/* Destroy connection callback */
static uint32_t static_PEmulDriverDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tEmulCardInfo* pEmulCardInfo = (tEmulCardInfo*)pObject;

   PDebugTrace("static_PEmulDriverDestroyConnection()");

   if((pEmulCardInfo->nState != P_EMUL_DRIVER_CLOSE_PENDING)
   && (pEmulCardInfo->nState != P_EMUL_DRIVER_CLOSED))
   {
      static_PEmulDriverInitiateDestruction(pContext, pEmulCardInfo);
   }

   return P_HANDLE_DESTROY_DONE;
}

/**
 * See  tPRegistryDriverCardEventDataReceived().
 **/
static void static_PEmulDriverEventReceived(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nProtocolBF,
            uint8_t nEventIdentifier,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tEmulCardInfo* pEmulCardInfo = (tEmulCardInfo*)pCallbackParameter;
   W_ERROR nError;
   uint32_t nEventCode;
   bool_t bPostRFEvent = W_FALSE;

   PDebugTrace(
      "static_PEmulDriverEventReceived: nEventIdentifier 0x%02X",
      nEventIdentifier );

   switch ( nEventIdentifier )
   {
      case NAL_EVT_CARD_SEND_DATA:
         /* Check the length */
         if ( nLength > W_EMUL_DRIVER_MAX_LENGTH )
         {
            PDebugError("static_PEmulDriverEventReceived: buffer to short");
            nError = W_ERROR_NFC_HAL_COMMUNICATION;
            goto return_error;
         }

         if((pEmulCardInfo->nState != P_EMUL_DRIVER_ACTIVE)
         && (pEmulCardInfo->nState != P_EMUL_DRIVER_SENDING_DATA))
         {
            PDebugError("static_PEmulDriverEventReceived: data received while not listenning");
            nError = W_ERROR_BAD_STATE;
            goto return_error;
         }

         /* Store the information */
         if(nLength != 0)
         {
            CMemoryCopy( pEmulCardInfo->aDataBuffer, pBuffer, nLength );
         }

         pEmulCardInfo->nReaderToCardBufferLength = nLength;

         if(pEmulCardInfo->nCardState != P_EMUL_CARD_STATE_SELECTED)
         {
            pEmulCardInfo->nCardState = P_EMUL_CARD_STATE_SELECTED;

            if(pEmulCardInfo->bIsEventCallbackSet != W_FALSE)
            {
               if (pEmulCardInfo->bFromUser != W_FALSE)
               {
                  PDFCDriverPostCC2(
                     pEmulCardInfo->pEventDriverCC,
                     W_EMUL_EVENT_SELECTION );
               }
               else
               {
                  PDFCPostContext2(
                     &pEmulCardInfo->sEventCC,
                     W_EMUL_EVENT_SELECTION );
               }
            }
         }

         if(pEmulCardInfo->nState == P_EMUL_DRIVER_SENDING_DATA)
         {
            if(pEmulCardInfo->bReaderDataAvailable != W_FALSE)
            {
               PDebugWarning("static_PEmulDriverEventReceived: Last event data lost (%d bytes)",
                  pEmulCardInfo->nReaderToCardBufferLength);
            }
            else
            {
               pEmulCardInfo->bReaderDataAvailable = W_TRUE;
            }

            return;
         }

         if (pEmulCardInfo->bFromUser != W_FALSE)
         {
            PDFCDriverPostCC2(
               pEmulCardInfo->pReceptionDriverCC,
               nLength );
         }
         else
         {
            PDFCPostContext2(
               &pEmulCardInfo->sReceptionCC,
               nLength );
         }

         return;

      case NAL_EVT_CARD_SELECTED:
         if (pEmulCardInfo->nCardState == P_EMUL_CARD_STATE_SELECTED)
         {
            PDebugWarning("static_PEmulDriverEventReceived: NAL_EVT_CARD_SELECTED ignored");
            return;
         }
         /* The information returned by the reader are ignored */
         pEmulCardInfo->nCardState = P_EMUL_CARD_STATE_SELECTED;
         nEventCode = W_EMUL_EVENT_SELECTION;
         break;

      case NAL_EVT_CARD_END_OF_TRANSACTION:
         if(nLength != 1)
         {
            PDebugError("static_PEmulDriverEventReceived: data in an event");
            nError = W_ERROR_NFC_HAL_COMMUNICATION;
            goto return_error;
         }

         if ((pEmulCardInfo->nCardState == P_EMUL_CARD_STATE_DESELECTED) ||
            (pEmulCardInfo->nCardState == P_EMUL_CARD_STATE_OTHER))
         {
            PDebugWarning("static_PEmulDriverEventReceived: NAL_EVT_CARD_END_OF_TRANSACTION ignored");
            return;
         }

         pEmulCardInfo->nCardState = P_EMUL_CARD_STATE_DESELECTED;
         nEventCode = W_EMUL_EVENT_DEACTIVATE;

         if (pBuffer[0] == 0x01)
         {
            /* The deselection has bass generated due to loss of RF event */
            bPostRFEvent = W_TRUE;
         }

         break;

      default:
         PDebugError(
            "static_PEmulDriverEventReceived: wrong event identifier 0x%02X",
            nEventIdentifier );
         nError = W_ERROR_NFC_HAL_COMMUNICATION;
         goto return_error;
   }


   if(pEmulCardInfo->bIsEventCallbackSet != W_FALSE)
   {
      if (pEmulCardInfo->bFromUser != W_FALSE)
      {
         PDFCDriverPostCC2(
            pEmulCardInfo->pEventDriverCC,
            nEventCode );
      }
      else
      {
         PDFCPostContext2(
            &pEmulCardInfo->sEventCC,
            nEventCode );
      }
   }

   if (bPostRFEvent)
   {
      PNFCControllerCallMonitorFieldCallback(pContext, W_NFCC_EVENT_FIELD_OFF);
   }

   return;

return_error:
   /* Send the error */
   PDebugError("static_PEmulDriverEventReceived: Error %s, NFC HAL message skipped",
               PUtilTraceError(nError));
   return;
}


/**
 * See  tPRegistryDriverCardEventDataReceived().
 **/
static void static_PEmulDriverP2PEventReceived(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nProtocolBF,
            uint8_t nEventIdentifier,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
   tEmulCardInfo* pEmulCardInfo = (tEmulCardInfo*)pCallbackParameter;
   W_ERROR nError;
   uint32_t nEventCode;

   switch ( nEventIdentifier )
   {
      case NAL_EVT_P2P_INITIATOR_DISCOVERED :

         if (pEmulCardInfo->nCardState == P_EMUL_CARD_STATE_SELECTED)
         {
            PDebugWarning("static_PEmulDriverP2PEventReceived: NAL_EVT_P2P_INITIATOR_DISCOVERED ignored");
            return;
         }

         if ( nLength > W_EMUL_DRIVER_MAX_LENGTH )
         {
            PDebugError("static_PEmulDriverP2PEventReceived: buffer to short");
            nError = W_ERROR_NFC_HAL_COMMUNICATION;
            goto return_error;
         }

         /* Store the information */
         if(nLength != 0)
         {
            CMemoryCopy( pEmulCardInfo->aDataBuffer, pBuffer, nLength );
         }

         pEmulCardInfo->nReaderToCardBufferLength = nLength;

         pEmulCardInfo->nCardState = P_EMUL_CARD_STATE_SELECTED;
         nEventCode = W_EMUL_EVENT_SELECTION;
         goto return_event;


      case NAL_EVT_P2P_SEND_DATA :

         /* Check the length */
         if ( nLength > W_EMUL_DRIVER_MAX_LENGTH )
         {
            PDebugError("static_PEmulDriverEventReceived: buffer too short");
            nError = W_ERROR_NFC_HAL_COMMUNICATION;
            goto return_error;
         }

         if((pEmulCardInfo->nState != P_EMUL_DRIVER_ACTIVE)
         && (pEmulCardInfo->nState != P_EMUL_DRIVER_SENDING_DATA))
         {
            PDebugError("static_PEmulDriverEventReceived: data received while not listenning");
            nError = W_ERROR_BAD_STATE;
            goto return_error;
         }

         if(pEmulCardInfo->nState == P_EMUL_DRIVER_SENDING_DATA)
         {
            if(pEmulCardInfo->bReaderDataAvailable != W_FALSE)
            {
               PDebugWarning("static_PEmulDriverEventReceived: Last event data lost (%d bytes)",
                  pEmulCardInfo->nReaderToCardBufferLength);
            }
            else
            {
               pEmulCardInfo->bReaderDataAvailable = W_TRUE;
            }
         }

         /* Store the information */
         if(nLength != 0)
         {
            CMemoryCopy( pEmulCardInfo->aDataBuffer, pBuffer, nLength );
         }

         pEmulCardInfo->nReaderToCardBufferLength = nLength;

         if(pEmulCardInfo->bReaderDataAvailable == W_FALSE)
         {
            /* Send the success */

            if (pEmulCardInfo->bFromUser != W_FALSE)
            {
               PDFCDriverPostCC2(
                  pEmulCardInfo->pReceptionDriverCC,
                  nLength );
            }
            else
            {
               PDFCPostContext2(
                  &pEmulCardInfo->sReceptionCC,
                  nLength );
            }

            if(pEmulCardInfo->nCardState != P_EMUL_CARD_STATE_SELECTED)
            {
               pEmulCardInfo->nCardState = P_EMUL_CARD_STATE_SELECTED;
               nEventCode = W_EMUL_EVENT_SELECTION;
               goto return_event;
            }
         }
         break;

      case NAL_EVT_CARD_END_OF_TRANSACTION :
         nEventCode = W_EMUL_EVENT_DEACTIVATE;
         goto return_event;
         break;

      default:
         PDebugError(
            "static_PEmulDriverP2PEventReceived: wrong event identifier 0x%02X",
            nEventIdentifier );
         nError = W_ERROR_NFC_HAL_COMMUNICATION;
         goto return_error;
   }

   return;

return_event:

   if(pEmulCardInfo->bIsEventCallbackSet != W_FALSE)
   {
      if (pEmulCardInfo->bFromUser != W_FALSE)
      {
         PDFCDriverPostCC2(
            pEmulCardInfo->pEventDriverCC,
            nEventCode );
      }
      else
      {
         PDFCPostContext2(
            &pEmulCardInfo->sEventCC,
            nEventCode );
      }
   }

   return;

return_error:
   /* Send the error */
   PDebugError("static_PEmulDriverEventReceived: Error %s, NFC HAL message skipped",
               PUtilTraceError(nError));
   return;
}


/**
 * @brief   Type of the fucntion to implement to be notified of the completion of
 * a send event operation initiated by PNALServiceSendEvent().
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nError  The error code:
 *               - W_SUCCESS in case of success.
 *               - W_ERROR_RF_COMMUNICATION  An error of protocol occured.
 *               - W_ERROR_CANCEL if the operation is cancelled.
 *
 * @param[in]  nReceptionCounter  The reception counter of the frame
 *             acknowledging the event message.
 *
 * @see  PNALServiceSendEvent()
 **/
static void static_PEmulDriverSendEventCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError,
            uint32_t nReceptionCounter )
{
   tEmulCardInfo* pEmulCardInfo = (tEmulCardInfo*)pCallbackParameter;

   CDebugAssert(pEmulCardInfo->nState == P_EMUL_DRIVER_SENDING_DATA);

   pEmulCardInfo->nState = P_EMUL_DRIVER_ACTIVE;

   if(pEmulCardInfo->bDestructionScheduled != W_FALSE)
   {
      PDebugTrace("static_PEmulDriverSendEventCompleted: destruction scheduled");
      static_PEmulDriverInitiateDestruction( pContext, pEmulCardInfo );
      return;
   }

   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PDebugError("static_PEmulDriverSendEventCompleted: nError = %s", PUtilTraceError(nError) );
   }
   else
   {
      PDebugTrace("static_PEmulDriverSendEventCompleted: data sent to the Reader");
   }

   if(pEmulCardInfo->bReaderDataAvailable != W_FALSE)
   {
      PDebugWarning("static_PEmulDriverSendEventCompleted: Sending stored reader data");

      pEmulCardInfo->bReaderDataAvailable = W_FALSE;

      /* Send the success */
      if (pEmulCardInfo->bFromUser != W_FALSE)
      {
         PDFCDriverPostCC2(
            pEmulCardInfo->pReceptionDriverCC,
            pEmulCardInfo->nReaderToCardBufferLength );
      }
      else
      {
         PDFCPostContext2(
            &pEmulCardInfo->sReceptionCC,
            pEmulCardInfo->nReaderToCardBufferLength );
      }
   }
}

/**
 * @brief   Receives the registration result
 **/
static void static_PEmulDriverRegisterCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tEmulCardInfo* pEmulCardInfo = (tEmulCardInfo*)pCallbackParameter;

   CDebugAssert(pEmulCardInfo->nState == P_EMUL_DRIVER_OPEN_PENDING);

   pEmulCardInfo->nState = P_EMUL_DRIVER_ACTIVE;

   if(pEmulCardInfo->bDestructionScheduled != W_FALSE)
   {
      PDebugTrace("static_PEmulDriverRegisterCompleted: destruction scheduled");
      static_PEmulDriverInitiateDestruction( pContext, pEmulCardInfo );
      return;
   }

   if(nError != W_SUCCESS)
   {
      PDebugError( "static_PEmulDriverRegisterCompleted: error %s", PUtilTraceError(nError) );

      pEmulCardInfo->nState = P_EMUL_DRIVER_ERROR;
   }

   if (pEmulCardInfo->bFromUser != W_FALSE)
   {
      PDFCDriverPostCC2(
         pEmulCardInfo->pOpenDriverCC,
         nError );
   }
   else
   {
      PDFCPostContext2( &pEmulCardInfo->sOpenCC, nError );
   }
}

void PEmulOpenConnectionDriver2(
            tContext* pContext,
            W_HANDLE hHandle,
            tPEmulDriverEventReceived* pEventCallback,
            void* pEventCallbackParameter )
{
   tEmulCardInfo* pEmulCardInfo;
   W_ERROR nError = PHandleGetObject(pContext, hHandle, P_HANDLE_TYPE_EMUL_DRIVER_CONNECTION, (void**)&pEmulCardInfo);
   if ( ( nError == W_SUCCESS ) && ( pEmulCardInfo != null ) )
   {
         /* P_EMUL_DRIVER_ACTIVE cause when Parameters are already set, PEmulOpenConnectionDriver1's callback
            might be executed before PEmulOpenConnectionDriver2 and PEmulOpenConnectionDriver3 are called */
      if ((pEmulCardInfo->nState == P_EMUL_DRIVER_OPEN_PENDING)
       || (pEmulCardInfo->nState == P_EMUL_DRIVER_ACTIVE))
      {
         pEmulCardInfo->bIsEventCallbackSet = W_TRUE;

         if (pEmulCardInfo->bFromUser != W_FALSE)
         {
            PDFCDriverFillCallbackContext(
               pContext,
               (tDFCCallback*)pEventCallback,
               pEventCallbackParameter,
               &pEmulCardInfo->pEventDriverCC );
         }
         else
         {
            PDFCFillCallbackContext(
               pContext,
               (tDFCCallback*)pEventCallback,
               pEventCallbackParameter,
               &pEmulCardInfo->sEventCC );
         }
      }
      else
      {
         PDebugError("PEmulOpenConnectionDriver2: bad state");
      }
   }
   else
   {
      PDebugError("PEmulOpenConnectionDriver2: wrong handle");
   }
}

/* Wrapper for Virtual Tag */
void PEmulOpenConnectionDriver2Ex(
            tContext* pContext,
            W_HANDLE hHandle,
            tPEmulDriverEventReceived* pEventCallback,
            void* pEventCallbackParameter )
{
   PEmulOpenConnectionDriver2( pContext, hHandle, pEventCallback, pEventCallbackParameter );
}

void PEmulOpenConnectionDriver3(
            tContext* pContext,
            W_HANDLE hHandle,
            tPEmulDriverCommandReceived* pCommandCallback,
            void* pCommandCallbackParameter )
{
   tEmulCardInfo* pEmulCardInfo;
   W_ERROR nError = PHandleGetObject(pContext, hHandle, P_HANDLE_TYPE_EMUL_DRIVER_CONNECTION, (void**)&pEmulCardInfo);
   if ( ( nError == W_SUCCESS ) && ( pEmulCardInfo != null ) )
   {
         /* P_EMUL_DRIVER_ACTIVE cause when Parameters are already set, PEmulOpenConnectionDriver1's callback
            might be executed before PEmulOpenConnectionDriver2 and PEmulOpenConnectionDriver3 are called */
      if ((pEmulCardInfo->nState == P_EMUL_DRIVER_OPEN_PENDING)
       || (pEmulCardInfo->nState == P_EMUL_DRIVER_ACTIVE))
      {
         if (pEmulCardInfo->bFromUser != W_FALSE)
         {
            PDFCDriverFillCallbackContext(
               pContext,
               (tDFCCallback*)pCommandCallback,
               pCommandCallbackParameter,
               &pEmulCardInfo->pReceptionDriverCC );
         }
         else
         {
            PDFCFillCallbackContext(
               pContext,
               (tDFCCallback*)pCommandCallback,
               pCommandCallbackParameter,
               &pEmulCardInfo->sReceptionCC );
         }
      }
      else
      {
         PDebugError("PEmulOpenConnectionDriver3: bad state");
      }
   }
   else
   {
      PDebugError("PEmulOpenConnectionDriver3: wrong handle");
   }
}

/* Wrapper for Virtual Tag */
void PEmulOpenConnectionDriver3Ex(
            tContext* pContext,
            W_HANDLE hHandle,
            tPEmulDriverCommandReceived* pCommandCallback,
            void* pCommandCallbackParameter )
{
   PEmulOpenConnectionDriver3( pContext, hHandle, pCommandCallback, pCommandCallbackParameter );
}

/* See Client API Specifications */
void PEmulCloseDriver(
            tContext* pContext,
            W_HANDLE hHandle,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter )
{
   tEmulCardInfo* pEmulCardInfo;
   tDFCDriverCCReference pCloseDriverCC;
   tDFCCallbackContext   CloseCC;

   W_ERROR nError = PHandleGetConnectionObject(pContext, hHandle, P_HANDLE_TYPE_EMUL_DRIVER_CONNECTION, (void**)&pEmulCardInfo);

   PDebugTrace("PEmulClose()");

   if ( nError == W_SUCCESS )
   {
      if(pEmulCardInfo != null)
      {
         /* Cancel asynchronous opening, if it is pending */
         if(pEmulCardInfo->nState == P_EMUL_DRIVER_OPEN_PENDING)
         {
            if (pEmulCardInfo->bFromUser != W_FALSE)
            {
               PDFCDriverPostCC2(
                  pEmulCardInfo->pOpenDriverCC,
                  W_ERROR_CANCEL );
            }
            else
            {
               PDFCPostContext2(
                  &pEmulCardInfo->sOpenCC,
                  W_ERROR_CANCEL );
            }
         }

         if((pEmulCardInfo->nState != P_EMUL_DRIVER_CLOSED)
         && (pEmulCardInfo->nState != P_EMUL_DRIVER_CLOSE_PENDING))
         {
            if (pEmulCardInfo->bFromUser != W_FALSE)
            {
               PDFCDriverFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback,
                  pCallbackParameter,
                  &pEmulCardInfo->pCloseDriverCC );
            }
            else
            {
               PDFCFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback,
                  pCallbackParameter,
                  &pEmulCardInfo->sCloseCC );
            }
            pEmulCardInfo->bIsCloseCallbackSet = W_TRUE;

            static_PEmulDriverInitiateDestruction(pContext, pEmulCardInfo);
         }
         else
         {
            nError = W_ERROR_BAD_STATE;
         }
      }
      else
      {
         nError = W_ERROR_BAD_HANDLE;
      }

      PHandleClose(pContext, hHandle);
   }

   if ( nError != W_SUCCESS )
   {
      if(pEmulCardInfo != null)
      {
         if (pEmulCardInfo->bFromUser != W_FALSE)
         {
            PDFCDriverFillCallbackContext(
               pContext,
               (tDFCCallback*)pCallback,
               pCallbackParameter,
               &pCloseDriverCC );

            PDFCDriverPostCC2(
               pCloseDriverCC,
               nError );

            return;
         }
      }

      if (pCallback != null)
      {
         PDFCFillCallbackContext(
            pContext,
            (tDFCCallback*)pCallback,
            pCallbackParameter,
            &CloseCC );

         PDFCPostContext2(
            &CloseCC,
            nError );
      }
      else
      {
         PDFCDriverFillCallbackContext(
            pContext,
            (tDFCCallback*)pCallback,
            pCallbackParameter,
            &pCloseDriverCC );

         PDFCDriverPostCC2(
            pCloseDriverCC,
            nError );
      }
   }
}

/* See header file */

void PEmulOpenConnectionDriver1(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pOpenCallback,
            void* pOpenCallbackParameter,
            const tWEmulConnectionInfo* pEmulConnectionInfo,
            uint32_t nSize,
            W_HANDLE* phHandle)
{

   /* This function is simply a wrapper to the internal
      generic function which support calls from user and from kernel */

   static_PEmulGenericOpenConnectionDriver1(
      pContext,
      pOpenCallback,
      pOpenCallbackParameter,
      pEmulConnectionInfo,
      nSize,
      phHandle,
      W_TRUE);      /* called from user */
}

/* Wrapper for Virtual Tag */
void PEmulOpenConnectionDriver1Ex(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pOpenCallback,
            void* pOpenCallbackParameter,
            const tWEmulConnectionInfo* pEmulConnectionInfo,
            uint32_t nSize,
            W_HANDLE* phHandle)
{
   PEmulOpenConnectionDriver1( pContext, pOpenCallback, pOpenCallbackParameter, pEmulConnectionInfo, nSize, phHandle );
}

/* See header file */
static void static_PEmulGenericOpenConnectionDriver1(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pOpenCallback,
            void* pOpenCallbackParameter,
            const tWEmulConnectionInfo* pEmulConnectionInfo,
            uint32_t nSize,
            W_HANDLE* phHandle,
            bool_t bFromUser)
{
   tEmulInstance* pEmulInstance = PContextGetEmulInstance( pContext );
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   tDFCDriverCCReference  pOpenDriverCC;
   tDFCCallbackContext    OpenCC;
   tEmulCardInfo* pEmulCardInfo;
   W_HANDLE hHandle;
   W_ERROR nError;
   uint32_t nSetParameterLength = 0;
   tPRegistryDriverCardProtocolConstant* pConstant;

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("static_PEmulGenericOpenConnectionDriver1: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_error;
   }

   /* Check the parameters */
   if ( (pEmulConnectionInfo == null) || (nSize != sizeof(tWEmulConnectionInfo)) || (phHandle == null) )
   {
      PDebugError("static_PEmulGenericOpenConnectionDriver1: error of parameter");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the tag type */
   switch ( pEmulConnectionInfo->nCardType )
   {
      case W_PROP_ISO_14443_4_A:

         pEmulCardInfo = &pEmulInstance->s14ACardInfo;
         pConstant = &g_sEmulCardProtocolConstantTypeA;

         /* Check the parameters */
         if ( pEmulConnectionInfo->sCardInfo.s14A.nApplicationDataLength > W_EMUL_APPLICATION_DATA_MAX_LENGTH )
         {
            PDebugError("static_PEmulGenericOpenConnectionDriver1: Type A application data too large");
            nError = W_ERROR_BUFFER_TOO_LARGE;
            goto return_error;
         }
         if ( pEmulConnectionInfo->sCardInfo.s14A.nDataRateMax > 3 )
         {
            PDebugError("static_PEmulGenericOpenConnectionDriver1: Wrong data max rate");
            nError = W_ERROR_BAD_PARAMETER;
            goto return_error;
         }
         if((pEmulConnectionInfo->sCardInfo.s14A.nUIDLength != 1)
         && (pEmulConnectionInfo->sCardInfo.s14A.nUIDLength != 4)
         && (pEmulConnectionInfo->sCardInfo.s14A.nUIDLength != 7)
         && (pEmulConnectionInfo->sCardInfo.s14A.nUIDLength != 10))
         {
            PDebugError("static_PEmulGenericOpenConnectionDriver1: Wrong UID length");
            nError = W_ERROR_BAD_PARAMETER;
            goto return_error;
         }
         break;

      case W_PROP_ISO_14443_4_B:

         pEmulCardInfo = &pEmulInstance->s14BCardInfo;
         pConstant = &g_sEmulCardProtocolConstantTypeB;

         /* Check the parameters */
         if ( pEmulConnectionInfo->sCardInfo.s14B.nHigherLayerResponseLength > W_EMUL_HIGHER_LAYER_RESPONSE_MAX_LENGTH )
         {
            PDebugError("static_PEmulGenericOpenConnectionDriver1: Type B higher response data too large");
            nError = W_ERROR_BUFFER_TOO_LARGE;
            goto return_error;
         }
         if((pEmulConnectionInfo->sCardInfo.s14B.nPUPILength != 0)
         && (pEmulConnectionInfo->sCardInfo.s14B.nPUPILength != 4))
         {
            PDebugError("static_PEmulGenericOpenConnectionDriver1: Wrong PUPI length");
            nError = W_ERROR_BAD_PARAMETER;
            goto return_error;
         }
         break;

      case W_PROP_P2P_TARGET :

         if (bFromUser != W_FALSE)
         {
            /* This mode is not supported when requested from user API ! */
            PDebugError("static_PEmulGenericOpenConnectionDriver1: W_PROP_P2P_TARGET not supported when requested from user API");
            nError = W_ERROR_BAD_PARAMETER;
            goto return_error;
         }

         pEmulCardInfo = &pEmulInstance->s18CardInfo;
         pConstant = &g_sEmulCardProtocolConstantP2PTarget;

         /* check the parameters */
         if ( pEmulConnectionInfo->sCardInfo.sP2PTarget.nLLCPParameterSize > W_EMUL_LLCP_PARAMETER_MAX_LENGTH )
         {
            PDebugError("static_PEmulGenericOpenConnectionDriver1 : P2PTarget parameter data too large");
            nError = W_ERROR_BUFFER_TOO_LARGE;
            goto return_error;
         }
         break;

      default:
         PDebugError("static_PEmulGenericOpenConnectionDriver1: unknown card type");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
   }

   if((pNFCControllerInfo->nProtocolCapabilities & pConstant->nProtocolBF) == 0)
   {
      PDebugError("static_PEmulGenericOpenConnectionDriver1: card emulation protocol not supported");
      nError = W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
      goto return_error;
   }

   /* Check if a connection has not been started yet */
   if ( pEmulCardInfo->nState != P_EMUL_DRIVER_CLOSED )
   {
      PDebugError("static_PEmulGenericOpenConnectionDriver1: Service already in use");
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   /* Check if the card protocol policy */
   if((pEmulConnectionInfo->nCardType == W_PROP_ISO_14443_4_A)
   || (pEmulConnectionInfo->nCardType == W_PROP_ISO_14443_4_B))
   {
      uint32_t nProtocols = PEmulGetActiveProtocol(pContext);
      nProtocols |= pConstant->nProtocolBF;

      nError = PNFCControllerCheckCardEmulPolicy(pContext, nProtocols);

      if(nError != W_SUCCESS)
      {
         PDebugError("static_PEmulGenericOpenConnectionDriver1: Card protocol policy error");
         goto return_error;
      }
   }

   pEmulCardInfo->pConstant = pConstant;

   /* Get a registry handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     pEmulCardInfo,
                     P_HANDLE_TYPE_EMUL_DRIVER_CONNECTION,
                     &hHandle ) ) != W_SUCCESS )
   {
      PDebugError("static_PEmulGenericOpenConnectionDriver1: error registering the connection object");
      goto return_error;
   }

      /* Store the callback information */

   if (bFromUser != W_FALSE)
   {
      PDFCDriverFillCallbackContext(
         pContext,
         (tDFCCallback*)pOpenCallback,
         pOpenCallbackParameter,
         &pEmulCardInfo->pOpenDriverCC );
   }
   else
   {
      PDFCFillCallbackContext(
         pContext,
         (tDFCCallback*)pOpenCallback,
         pOpenCallbackParameter,
         &pEmulCardInfo->sOpenCC );
   }

   /* Build the set parameter buffer */
   switch ( pConstant->nProtocolBF )
   {
   case W_NFCC_PROTOCOL_CARD_ISO_14443_4_A:
      /* Store the parameters */
      /* - UID */
      CMemoryFill( &pEmulCardInfo->aDataBuffer[nSetParameterLength], 0, 10 );

      if ( pEmulConnectionInfo->sCardInfo.s14A.nUIDLength != 1 )
      {
         CMemoryCopy(
            &pEmulCardInfo->aDataBuffer[nSetParameterLength],
            pEmulConnectionInfo->sCardInfo.s14A.UID,
            pEmulConnectionInfo->sCardInfo.s14A.nUIDLength );

         nSetParameterLength += 10;
         pEmulCardInfo->aDataBuffer[nSetParameterLength++] = pEmulConnectionInfo->sCardInfo.s14A.nUIDLength;
      }
      else
      {
         /* Set UID length to zero to force NFCC to generate random UID*/
         nSetParameterLength += 10;
         pEmulCardInfo->aDataBuffer[nSetParameterLength++] = 0;
      }
      /* - ATQA MSB */
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = (uint8_t)((pEmulConnectionInfo->sCardInfo.s14A.nATQA >> 8) & 0xFF);
      /* - T0 */
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = 0x7F; /* frame 256 bytes */
      /* - TA */
      pEmulCardInfo->aDataBuffer[nSetParameterLength] = 0x80; /* same divisor in both directions */
      switch (pEmulConnectionInfo->sCardInfo.s14A.nDataRateMax)
      {
         case 0x01:
            pEmulCardInfo->aDataBuffer[nSetParameterLength] |= 0x11;
            break;
         case 0x02:
            pEmulCardInfo->aDataBuffer[nSetParameterLength] |= 0x22;
            break;
         case 0x03:
            pEmulCardInfo->aDataBuffer[nSetParameterLength] |= 0x44;
            break;
      }
      nSetParameterLength++;
      /* - TB */
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = pEmulConnectionInfo->sCardInfo.s14A.nFWI_SFGI;
      /* - TC */
      pEmulCardInfo->aDataBuffer[nSetParameterLength] = 0x00;
      if ( pEmulConnectionInfo->sCardInfo.s14A.bSetCIDSupport != W_FALSE )
      {
         pEmulCardInfo->aDataBuffer[nSetParameterLength] |= 0x02;
      }
      if (pEmulConnectionInfo->sCardInfo.s14A.nNAD != 0)
      {
         uint32_t nCapabilities = pNFCControllerInfo->nFirmwareCapabilities;
         if ((nCapabilities & NAL_CAPA_CARD_ISO_14443_A_NAD) != 0)
         {
            pEmulCardInfo->aDataBuffer[nSetParameterLength] |= 0x01;
         }
      }
      nSetParameterLength++;
      /* - Application data */
      if ( pEmulConnectionInfo->sCardInfo.s14A.nApplicationDataLength != 0 )
      {
         CMemoryCopy(
            &pEmulCardInfo->aDataBuffer[nSetParameterLength],
            pEmulConnectionInfo->sCardInfo.s14A.aApplicationData,
            pEmulConnectionInfo->sCardInfo.s14A.nApplicationDataLength );
         nSetParameterLength += pEmulConnectionInfo->sCardInfo.s14A.nApplicationDataLength;
      }
      break;

   case W_NFCC_PROTOCOL_CARD_ISO_14443_4_B:
      /* Store the parameters */
      /* - ATQB */
      if ( pEmulConnectionInfo->sCardInfo.s14B.nPUPILength != 0 )
      {
         CMemoryCopy(
            &pEmulCardInfo->aDataBuffer[nSetParameterLength],
            pEmulConnectionInfo->sCardInfo.s14B.PUPI,
            4 );
      }
      else
      {
         CMemoryFill(
            &pEmulCardInfo->aDataBuffer[nSetParameterLength],
            0, 4);
      }
      nSetParameterLength += 4;
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = pEmulConnectionInfo->sCardInfo.s14B.nAFI;
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = (uint8_t)((pEmulConnectionInfo->sCardInfo.s14B.nATQB >> 24) & 0xFF);
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = (uint8_t)((pEmulConnectionInfo->sCardInfo.s14B.nATQB >> 16) & 0xFF);
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = (uint8_t)((pEmulConnectionInfo->sCardInfo.s14B.nATQB >> 8 ) & 0xFF);
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = 0x80; /* same bit rate from PCD to PICC and from PICC to PCD */
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = 0x81; /* compliant ISO 14443-4 and for 256 bytes size frame */
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = (uint8_t)(pEmulConnectionInfo->sCardInfo.s14B.nATQB & 0xFF);
      /* - Higher layer response data */
      if ( pEmulConnectionInfo->sCardInfo.s14B.nHigherLayerResponseLength != 0 )
      {
         CMemoryCopy(
            &pEmulCardInfo->aDataBuffer[nSetParameterLength],
            pEmulConnectionInfo->sCardInfo.s14B.aHigherLayerResponse,
            pEmulConnectionInfo->sCardInfo.s14B.nHigherLayerResponseLength );
         nSetParameterLength += pEmulConnectionInfo->sCardInfo.s14B.nHigherLayerResponseLength;
      }
      break;

   case W_NFCC_PROTOCOL_CARD_P2P_TARGET :

      PNALWriteUint32ToBuffer(pEmulConnectionInfo->sCardInfo.sP2PTarget.nTimeOutRTX, &pEmulCardInfo->aDataBuffer[nSetParameterLength]);
      nSetParameterLength += 4;

      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = (uint8_t)pEmulConnectionInfo->sCardInfo.sP2PTarget.bAllowTypeATargetProtocol;
      pEmulCardInfo->aDataBuffer[nSetParameterLength++] = (uint8_t)pEmulConnectionInfo->sCardInfo.sP2PTarget.bAllowActiveMode;

      if ( pEmulConnectionInfo->sCardInfo.sP2PTarget.nLLCPParameterSize != 0)
      {
         CMemoryCopy(
            &pEmulCardInfo->aDataBuffer[nSetParameterLength],
            pEmulConnectionInfo->sCardInfo.sP2PTarget.aLLCPParameter,
            pEmulConnectionInfo->sCardInfo.sP2PTarget.nLLCPParameterSize
            );

         nSetParameterLength += pEmulConnectionInfo->sCardInfo.sP2PTarget.nLLCPParameterSize;
      }
      break;
   }

   pEmulCardInfo->nState = P_EMUL_DRIVER_OPEN_PENDING;
   pEmulCardInfo->bFromUser = bFromUser;

   *phHandle = hHandle;

   PReaderDriverRegisterCardEmulation(
         pContext,
         pEmulCardInfo->pConstant->nProtocolBF,
         static_PEmulDriverRegisterCompleted, pEmulCardInfo,
         pEmulCardInfo->aDataBuffer, nSetParameterLength);

   return;

return_error:

   PDebugError("PEmulOpenConnectionDriver1: returning error %s", PUtilTraceError(nError));

   if(phHandle != null)
   {
      *phHandle = W_NULL_HANDLE;
   }

   if (bFromUser != W_FALSE)
   {

      PDFCDriverFillCallbackContext(
         pContext,
         (tDFCCallback*)pOpenCallback,
         pOpenCallbackParameter,
         &pOpenDriverCC );

      PDFCDriverPostCC2(
         pOpenDriverCC,
         nError );
   }
   else
   {
      PDFCFillCallbackContext(
         pContext,
         (tDFCCallback*)pOpenCallback,
         pOpenCallbackParameter,
         &OpenCC );

      PDFCPostContext2(
         &OpenCC,
         nError );
   }
}

/* See Specification */
W_ERROR PEmulGetMessageData(
                  tContext* pContext,
                  W_HANDLE hDriverHandle,
                  uint8_t* pDataBuffer,
                  uint32_t nDataLength,
                  uint32_t* pnActualDataLength)
{
   tEmulCardInfo* pEmulCardInfo;
   W_ERROR nError;

   /* Check the parameters */
   if(( pnActualDataLength == null )
   || ( pDataBuffer == null )
   || ( nDataLength == 0 ) )
   {
      PDebugError("WEmulGetMessageData: bad parameter");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("WEmulGetMessageData: bad NFC Controller mode");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_error;
   }

   nError = PHandleGetConnectionObject(pContext, hDriverHandle, P_HANDLE_TYPE_EMUL_DRIVER_CONNECTION, (void**)&pEmulCardInfo);
   if ( ( nError == W_SUCCESS ) && ( pEmulCardInfo != null ) )
   {
      if(pEmulCardInfo->nState != P_EMUL_DRIVER_ACTIVE)
      {
         PDebugError("WEmulGetMessageData: bad state");
         nError = W_ERROR_BAD_STATE;
         goto return_error;
      }

      if ( nDataLength < pEmulCardInfo->nReaderToCardBufferLength )
      {
         PDebugError("WEmulGetMessageData: buffer too short");
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_error;
      }

      /* Copy the buffer */
      CMemoryCopy(
         pDataBuffer,
         pEmulCardInfo->aDataBuffer,
         pEmulCardInfo->nReaderToCardBufferLength );

      *pnActualDataLength = pEmulCardInfo->nReaderToCardBufferLength;
      return W_SUCCESS;
   }
   else
   {
      PDebugError("WEmulGetMessageData: could not get pEmulCardInfo buffer");
   }

return_error:

   if(pnActualDataLength != null)
   {
      *pnActualDataLength = 0;
   }

   PDebugError("WEmulGetMessageData: returning error %s", PUtilTraceError(nError));

   return nError;
}

/* See Client API Specifications */
W_ERROR PEmulSendAnswer(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            const uint8_t* pDataBuffer,
            uint32_t nDataLength )
{
   tEmulCardInfo* pEmulCardInfo;
   W_ERROR nError;

   /* Check the parameters */
   if (( ( pDataBuffer == null ) && ( nDataLength != 0 ) )
   ||  ( ( pDataBuffer != null ) && ( nDataLength == 0 ) ))
   {
      PDebugError("WEmulSendAnswer: bad parameter");
      return W_ERROR_BAD_PARAMETER;
   }
   if (  nDataLength > W_EMUL_DRIVER_MAX_LENGTH )
   {
      PDebugError("WEmulSendAnswer: buffer too long");
      return W_ERROR_BUFFER_TOO_LARGE;
   }

   if (PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("WEmulSendAnswer: bad NFC Controller mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   nError = PHandleGetConnectionObject(pContext, hDriverConnection, P_HANDLE_TYPE_EMUL_DRIVER_CONNECTION, (void**)&pEmulCardInfo);
   if ( ( nError == W_SUCCESS ) && ( pEmulCardInfo != null ) )
   {
      if(pEmulCardInfo->nState != P_EMUL_DRIVER_ACTIVE)
      {
         PDebugError("WEmulSendAnswer: bad state");
         return W_ERROR_BAD_STATE;
      }

      pEmulCardInfo->nState = P_EMUL_DRIVER_SENDING_DATA;

      if(nDataLength != 0)
      {
         /* Prepare the command */
         CMemoryCopy(
            pEmulCardInfo->aDataBuffer,
            pDataBuffer,
            nDataLength );
      }

      if (pEmulCardInfo->pConstant->nProtocolBF != W_NFCC_PROTOCOL_CARD_P2P_TARGET)
      {
         /* Send the data */
         PNALServiceSendEvent(
            pContext,
            pEmulCardInfo->pConstant->nServiceIdentifier,
            &pEmulCardInfo->sServiceOperationSendEvent,
            NAL_EVT_CARD_SEND_DATA,
            pEmulCardInfo->aDataBuffer,
            nDataLength,
            static_PEmulDriverSendEventCompleted,
            pEmulCardInfo );
      }
      else
      {
         /* Send the data */
         PNALServiceSendEvent(
            pContext,
            pEmulCardInfo->pConstant->nServiceIdentifier,
            &pEmulCardInfo->sServiceOperationSendEvent,
            NAL_EVT_P2P_SEND_DATA,
            pEmulCardInfo->aDataBuffer,
            nDataLength,
            static_PEmulDriverSendEventCompleted,
            pEmulCardInfo );
      }

      return W_SUCCESS;
   }
   else
   {
      PDebugError("WEmulSendAnswer: could not get pEmulCardInfo buffer");
      return nError;
   }
}

/* See header file */
void PEmulCreate(
            tEmulInstance* pEmulInstance )
{
   CMemoryFill( pEmulInstance, 0, sizeof(tEmulInstance) );
}

/* See header file */
void PEmulDestroy(
            tEmulInstance* pEmulInstance )
{
   if ( pEmulInstance != null )
   {
      CMemoryFill( pEmulInstance, 0, sizeof(tEmulInstance) );
   }
}

/* See header file */
uint32_t PEmulGetActiveProtocol(
                  tContext* pContext)
{
   tEmulInstance* pEmulInstance = PContextGetEmulInstance( pContext );
   uint32_t nProtocol = 0;

   if(pEmulInstance->s14ACardInfo.nState != P_EMUL_DRIVER_CLOSED)
   {
      nProtocol |= g_sEmulCardProtocolConstantTypeA.nProtocolBF;
   }
   if(pEmulInstance->s14BCardInfo.nState != P_EMUL_DRIVER_CLOSED)
   {
      nProtocol |= g_sEmulCardProtocolConstantTypeB.nProtocolBF;
   }
   if(pEmulInstance->s18CardInfo.nState != P_EMUL_DRIVER_CLOSED)
   {
      nProtocol |= g_sEmulCardProtocolConstantP2PTarget.nProtocolBF;
   }

   return nProtocol;
}

/* See header file */
uint8_t PEmulGetRFActivity(
                  tContext* pContext)
{
   tEmulInstance* pEmulInstance = PContextGetEmulInstance( pContext );
   uint8_t nActivity = W_NFCC_RF_ACTIVITY_INACTIVE;
   tEmulCardInfo* pEmulCardInfo;

   pEmulCardInfo = &pEmulInstance->s14ACardInfo;

   if(pEmulCardInfo->nState != P_EMUL_DRIVER_CLOSED)
   {
      if (pEmulCardInfo->nCardState == P_EMUL_CARD_STATE_SELECTED)
      {
         nActivity = W_NFCC_RF_ACTIVITY_ACTIVE;
      }
      else
      {
         nActivity = W_NFCC_RF_ACTIVITY_DETECTION;
      }
   }

   pEmulCardInfo = &pEmulInstance->s14BCardInfo;
   if(pEmulCardInfo->nState != P_EMUL_DRIVER_CLOSED)
   {
      if (pEmulCardInfo->nCardState  == P_EMUL_CARD_STATE_SELECTED)
      {
         nActivity = W_NFCC_RF_ACTIVITY_ACTIVE;
      }
      else
      {
         if(nActivity == W_NFCC_RF_ACTIVITY_INACTIVE)
         {
            nActivity = W_NFCC_RF_ACTIVITY_DETECTION;
         }
      }
   }

   return nActivity;
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
