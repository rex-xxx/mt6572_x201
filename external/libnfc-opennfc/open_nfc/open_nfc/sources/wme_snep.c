/*
 * Copyright (c) 2012 Inside Secure, All Rights Reserved.
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
   Contains the implementation of the SNEP client and server
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( SNEP )

#include "wme_context.h"

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_SNEP_NPP)

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC && defined P_INCLUDE_SNEP_NPP*/


#if ((P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_SNEP_NPP)

/**
  * Function called when the stack is started.
  * Perform SNEP context specific initialization (if any)
  */

void PSNEPServerDriverInstanceCreate(
   tSNEPServerDriverInstance * pSNEPServer)
{
   CMemoryFill(pSNEPServer, 0, sizeof(tSNEPServerDriverInstance));
}

/**
  * Function called when the stack is terminating
  * Perform SNEP context cleanup (if any)
  */

void PSNEPServerDriverInstanceDestroy(
   tSNEPServerDriverInstance * pSNEPServer)
{
   CMemoryFill(pSNEPServer, 0, sizeof(tSNEPServerDriverInstance));
}


static uint32_t static_PSNEPMessageHandlerDestroyAsync(tContext* pContext, tPBasicGenericCallbackFunction* pCallback, void* pCallbackParameter, void* pObject );

static void static_PSNEPServerLinkEstablishmentCallback( tContext* pContext, void * pCallbackParameter, W_HANDLE hLink, W_ERROR nResult );
static void static_PSNEPServerLinkReleaseCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult);
static void static_PSNEPServerSocketConnected( tContext* pContext, void * pCallbackParameter,  W_ERROR nResult );

static void static_SNEPServerDataReceived( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nResult );
static void static_SNEPServerDataTransmitted( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );

static void static_PSNEPServerShutdownConnectionCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );

#define P_SNEP_SERVER_SAP           4
static const char16_t g_SNEPUri[] = { 'u','r','n',':','n','f','c',':','s','n',':','s','n','e','p', 0 };

/* SNEP Message handler type :
   This structure defines the different functions to be called to process operation on the
   associated handle. Here we only need the destroy function */
tHandleType g_sSNEPMessageHandler = { null, static_PSNEPMessageHandlerDestroyAsync, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_SNEP_SERVER (&g_sSNEPMessageHandler)

#define SNEP_PROTOCOL_HEADER_LENGTH           6
#define SNEP_PROTOCOL_HEADER_VERSION_POS      0
#define SNEP_PROTOCOL_HEADER_REQUEST_POS      1
#define SNEP_PROTOCOL_HEADER_LEN_MSB_POS      2

#define SNEP_DEFAULT_SERVER_MAX_INFO_BYTES 0xFFFF

#define AWAITING_SNEP_HEADER               0x00
#define AWAITING_SNEP_FRAGMENT             0x01

#define SENDING_SNEP_HEADER                0x02
#define SENDING_SNEP_FRAGMENT              0x03
#define AWAITING_SNEP_ANSWER_SUCCESS       0x04
#define AWAITING_SNEP_ANSWER_CONTINUE      0x05



#define SNEP_PROTOCOL_MAJOR_VERSION        0x10
#define SNEP_PROTOCOL_MINOR_VERSION        0x00
#define SNEP_PROTOCOL_MAJOR_VERSION_MASK   0xF0
#define SNEP_PROTOCOL_MINOR_VERSION_MASK   0x0F

/* Request Field Values */
#define REQUEST_CONTINUE                   0x00
#define REQUEST_GET                        0x01
#define REQUEST_PUT                        0x02
#define REQUEST_REJECT                     0x7F

/* Response Field Values */
#define RESPONSE_CONTINUE                  0x80
#define RESPONSE_SUCCESS                   0x81
#define RESPONSE_NOT_FOUND                 0xC0
#define RESPONSE_EXCESS_DATA               0xC1
#define RESPONSE_BAD_REQUEST               0xC2
#define RESPONSE_NOT_IMPLEMENTED           0xE0
#define RESPONSE_UNSUPPORTED_VERSION       0xE1
#define RESPONSE_REJECT                    0xFF


/**
 * Registers a callback function to be called each time a SNEP message is received
 *
 * It creates the server socket and request the P2P link establishment if needed
 *
 * \param phandler the function to be called each time a SNEP message is received
 * \param pHandlerParameter the callback parameter
 * \param phRegistry The handle used to request the unregistration of the callbak function
 *
 */

W_ERROR PNDEFRegisterSNEPMessageHandlerDriver  (
      tContext * pContext,
      tPBasicGenericDataCallbackFunction* pHandler,
      void *  pHandlerParameter,
      uint8_t nPriority,
      W_HANDLE *  phRegistry )
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);
   tPSNEPMessageHandler * pMessageHandler = 0;
   tPSNEPMessageHandler * pLastSNEPMessageHandler, * pCurrentSNEPMessageHandler;
   tDFCDriverCCReference pDriverCC;
   W_ERROR       nError;
   W_HANDLE      hLink;

   PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pHandler, pHandlerParameter, &pDriverCC);

   if (phRegistry != null)
   {
      * phRegistry = W_NULL_HANDLE;
   }
   else
   {
      PDebugError("PNDEFRegisterSNEPMessageHandlerDriver : phRegistry == null");
      nError = W_ERROR_BAD_PARAMETER;
      goto end;
   }

   /* Allocate a data structure that will contain all stuff specific to this handler registration
      the common stuff (socket, P2P link are stored in the context itself...  */

   pMessageHandler = (tPSNEPMessageHandler *) CMemoryAlloc(sizeof(tPSNEPMessageHandler));
   pMessageHandler->pNext = null;

   if (pMessageHandler == null)
   {
      /* either if use of goto usage is quite controversial, we use them for error management in the Open NFC stack */
      PDebugError("PNDEFRegisterSNEPMessageHandlerDriver : pMessageHandler == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto end;
   }

   pMessageHandler->pDriverCC = pDriverCC;
   pMessageHandler->nPriority = nPriority;

   /* Creates a new handle associated to the pMessageHandler. This handle is returned in the phRegistry parameter.
      This handle is used for unregistration of the listener */

   nError = PHandleRegister(pContext, pMessageHandler, P_HANDLE_TYPE_SNEP_SERVER, phRegistry);

   if (nError != W_SUCCESS)
   {
      PDebugError("PNDEFRegisterSNEPMessageHandlerDriver : PHandleRegister failed %d", nError);
      goto end;
   }

   /* create only one socket for all registered handlers */

   if (pSNEPServer->hServerSocket == W_NULL_HANDLE)
   {
      tUserInstance * pCurrentUserInstance;
      tWP2PConfiguration sConfiguration;
      /* We must keep the number of registrations :
         each time a new registration is done, the usage count is incremented.
         the socket must be closed and the link establishment must be destroyed  when the usage count drops to zero */

      CDebugAssert(pSNEPServer->nServerSocketUsageCount == 0);
      CDebugAssert(pSNEPServer->hServerSocket == W_NULL_HANDLE);
      CDebugAssert(pSNEPServer->hLinkOperation == W_NULL_HANDLE);

      pSNEPServer->nServerSocketUsageCount = 1;
      pSNEPServer->bShutdownRequested = W_FALSE;
      pSNEPServer->bConnectionPending = W_TRUE;

      /* Handles are by default associated to the current user instance, meaning they are automatically closed
       * when the user instance is destroyed. Here, we only create one socket and make only one link establishment
       * requets for several clients, so we need to drop temporary the current instance for these operations.
       *
       * NOTE: The user instance must be restored at the end
       */

      pCurrentUserInstance = PContextGetCurrentUserInstance(pContext);

      PContextSetCurrentUserInstance(pContext, null);
      nError = PP2PCreateSocketDriver(pContext, W_P2P_TYPE_SERVER, g_SNEPUri, sizeof(g_SNEPUri), P_SNEP_SERVER_SAP, &pSNEPServer->hServerSocket);

      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFRegisterSNEPMessageHandlerDriver : PP2PCreateSocketDriver failed %d", nError);

         /* Restore the current user instance */
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto end;
      }

      nError = PP2PGetConfigurationDriver(pContext, &sConfiguration, sizeof(sConfiguration));
      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFRegisterSNEPMessageHandlerDriver : PP2PGetConfigurationDriver failed %d", nError);
         goto end;
      }

      nError = PP2PSetSocketParameter(pContext, pSNEPServer->hServerSocket, W_P2P_LOCAL_MIU, sConfiguration.nLocalMIU);
      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFRegisterSNEPMessageHandlerDriver : PP2PSetSocketParameter failed %d", nError);
         goto end;
      }

      /* Request link establishment */

      /* Since we are in the driver part of the stack, we must call internal function instead of PP2PEstablishLinkDriverInternal()
         which can only be called from user part
      */
      hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PSNEPServerLinkEstablishmentCallback, null);

      if (hLink == W_NULL_HANDLE)
      {
         PDebugError("PNDEFRegisterSNEPMessageHandlerDriver : PP2PEstablishLinkDriver1Internal failed");

         /* Restore the current user instance */
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto end;
      }

      PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PSNEPServerLinkReleaseCallback, null, &pSNEPServer->hLinkOperation);

      /* Restore the current user instance */
      PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
   }
   else
   {
      /* Increase the count of the registration */
      pSNEPServer->nServerSocketUsageCount++;
   }

   /* enqueue the message handler in the list of the already registered messages handler,
      accordingly to their priority */

   pLastSNEPMessageHandler = null;
   pCurrentSNEPMessageHandler = pSNEPServer->pFirstMessageHandler;

   while ((pCurrentSNEPMessageHandler != null) && (pCurrentSNEPMessageHandler->nPriority >= nPriority))
   {
      pLastSNEPMessageHandler = pCurrentSNEPMessageHandler;
      pCurrentSNEPMessageHandler = pCurrentSNEPMessageHandler->pNext;
   }

   if (pLastSNEPMessageHandler == null)
   {
      pMessageHandler->pNext = pSNEPServer->pFirstMessageHandler;
      pSNEPServer->pFirstMessageHandler = pMessageHandler;
   }
   else
   {
      pMessageHandler->pNext = pLastSNEPMessageHandler->pNext;
      pLastSNEPMessageHandler->pNext = pMessageHandler;
   }

   pCurrentSNEPMessageHandler = pSNEPServer->pFirstMessageHandler;

end:

   if (nError != W_SUCCESS)
   {
      /* Driver's DFC are dynamically allocated and are freed
       *  - when the callback is posted for one-shot callback
       *  - when the PDFCDriverFlushCall is called for event (multi call) callback
       */

      if ((phRegistry != null) && (* phRegistry != W_NULL_HANDLE))
      {
         PBasicCloseHandle(pContext, * phRegistry);
         * phRegistry = W_NULL_HANDLE;
      }
      else if (pMessageHandler != null)
      {
         PDFCDriverFlushCall(pMessageHandler->pDriverCC);
         CMemoryFree(pMessageHandler);
      }
      else
      {
         PDFCDriverFlushCall(pDriverCC);
      }
   }

   return nError;
}


static void static_PNDEFMessageHandlerDestroyAutomaton(tContext* pContext, tPSNEPMessageHandler * pMessageHandler)
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);

   /* Request shutdown of the socket connection */
   if ((pSNEPServer->hServerSocket != W_NULL_HANDLE) && (pSNEPServer->bShutdownRequested == W_FALSE))
   {
      PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : terminating server socket connection");
      PP2PShutdownDriverInternal(pContext, pSNEPServer->hServerSocket, static_PSNEPServerShutdownConnectionCallback, pMessageHandler);
      pSNEPServer->bShutdownRequested = W_TRUE;

      return;
   }
   /* close the server socket */
   if (pSNEPServer->hServerSocket != W_NULL_HANDLE)
   {

      PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : closing server socket");
      PBasicCloseHandle(pContext, pSNEPServer->hServerSocket);
      pSNEPServer->hServerSocket = W_NULL_HANDLE;
   }

   if (pSNEPServer->hLink != W_NULL_HANDLE)
   {
      /* request the termination of the P2P RF link if established */
      PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : closing link");
      PBasicCloseHandle(pContext, pSNEPServer->hLink);
      pSNEPServer->hLink = W_NULL_HANDLE;
   }
   else
   {
      /* Cancel link establishment if the RF link is not established */
      PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : aborting link establishment");
      PBasicCancelOperation(pContext, pSNEPServer->hLinkOperation);
      PBasicCloseHandle(pContext, pSNEPServer->hLinkOperation);

      pSNEPServer->hLinkOperation = W_NULL_HANDLE;
   }

   PDFCDriverFlushCall(pMessageHandler->pDriverCC);

   /* Post the destroy callback */
   PDFCPostContext2(&pMessageHandler->sDestroyCallbackContext, W_SUCCESS);

   /* free the resources */
   CMemoryFree(pMessageHandler);

   PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : completed");
}

/**
 * This function is called when the handle is being closed by the user
 *
 * This function will decrease the usage count of the internal SNEP server
 * If the usage count reaches 0, the socket is destroyed and the P2P link is closed
 */

static uint32_t static_PSNEPMessageHandlerDestroyAsync(tContext* pContext, tPBasicGenericCallbackFunction* pCallback, void* pCallbackParameter, void* pObject )
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);
   tPSNEPMessageHandler * pMessageHandler = (tPSNEPMessageHandler *) pObject;
   tPSNEPMessageHandler * pLastSNEPMessageHandler;
   tPSNEPMessageHandler * pCurrentSNEPMessageHandler = 0;

   /* Save the current context and replace it by null value */
   tUserInstance * pUserInstance = PContextGetCurrentUserInstance(pContext);
   PContextSetCurrentUserInstance(pContext, null);

   /* If the destroyed message handler is blocking the Message queue */
   if(pMessageHandler == pSNEPServer->pLastMessageHandlerCalled)
   {
      PNDEFSetWorkPerformedSNEPDriver(pContext, W_TRUE);
   }

   PDebugTrace("static_PSNEPMessageHandlerDestroyAsync");

   /* Remove the Handler from the linked list within the SNEP Server */

   pLastSNEPMessageHandler = null;
   pCurrentSNEPMessageHandler = pSNEPServer->pFirstMessageHandler;

   while ((pCurrentSNEPMessageHandler != null) && (pCurrentSNEPMessageHandler != pMessageHandler))
   {
      pLastSNEPMessageHandler = pCurrentSNEPMessageHandler;
      pCurrentSNEPMessageHandler  = pCurrentSNEPMessageHandler->pNext;
   }

   if (pLastSNEPMessageHandler == null)
   {
      pSNEPServer->pFirstMessageHandler = pMessageHandler->pNext;
   }
   else
   {
      pLastSNEPMessageHandler->pNext = pMessageHandler->pNext;
   }

   /*
    * Decrement the reference count
    * If it reaches 0, the server socket must be closed
    * else, simply free the resources of the
    */
   if (-- pSNEPServer->nServerSocketUsageCount == 0)
   {
      PDebugTrace("static_PSNEPMessageHandlerDestroyAsync : no more registered handler, terminate link establishment");

      /* Free the received buffer memory if it's always allocated */
      if( pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer != null)
      {
         CMemoryFree(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer);
      }
      pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer = null;
      pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength = 0;
      pSNEPServer->sReceivedNDEFMessage.nNDEFBufferIndex = 0;

      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, & pMessageHandler->sDestroyCallbackContext);

      static_PNDEFMessageHandlerDestroyAutomaton(pContext, pMessageHandler);
   }
   else
   {
      tDFCCallbackContext sCallbackContext;

      /* Flush the driver DFC */
      PDFCDriverFlushCall(pMessageHandler->pDriverCC);

      /* Free the structure */
      CMemoryFree(pObject);

      /* Post the callback */
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, W_SUCCESS);
   }

   PContextSetCurrentUserInstance(pContext, pUserInstance);

   return P_HANDLE_DESTROY_PENDING;
}


/**
 * This function is called when the P2P link is established.
 *
 * On success, we must start accepting client connection on the server socket.
 * On failure, depending on the existence of at least one registered handler, we must request link establishment once again
 */

static void static_PSNEPServerLinkEstablishmentCallback( tContext* pContext, void * pCallbackParameter, W_HANDLE hLink, W_ERROR nResult )
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);  /* can not be null */

   PBasicCloseHandle(pContext, pSNEPServer->hLinkOperation);
   pSNEPServer->hLinkOperation = W_NULL_HANDLE;

   if (nResult == W_SUCCESS)
   {
      /* Cache the hink handle */
      pSNEPServer->hLink = hLink;

      PDebugTrace("static_PSNEPServerLinkEstablishmentCallback : request SNEP server connection");

      /* The link has been activated, we must start accepting connections on the server socket */
      PP2PConnectDriverInternal(pContext, pSNEPServer->hServerSocket, hLink, static_PSNEPServerSocketConnected, null);
   }
   else
   {
      PDebugError("static_PSNEPServerLinkEstablishmentCallback : P2P link establishment failed %d", nResult);

      if (pSNEPServer->nServerSocketUsageCount > 0)
      {
         W_HANDLE hTempLink;

         PDebugTrace("static_PSNEPServerLinkEstablishmentCallback : request new link establishment");

         hTempLink = PP2PEstablishLinkDriver1Internal(pContext, static_PSNEPServerLinkEstablishmentCallback, null);

         if (hTempLink != W_NULL_HANDLE)
         {
            PP2PEstablishLinkDriver2Internal(pContext, hTempLink, static_PSNEPServerLinkReleaseCallback, null, &pSNEPServer->hLinkOperation);
         }
         else
         {
            PDebugError("static_PSNEPServerLinkEstablishmentCallback : unable to request new link establishemnt : TROUBLE AHEAD");
         }
      }
      else
      {
         PDebugTrace("static_PSNEPServerLinkEstablishmentCallback : no more listener");
      }
   }
}


/*
 * This function is called when the P2P link is broken
 *
 * Depending on the existence of at least one registered handler, we must request link establishment once again
 * or perform final cleanup (if any)
 */

static void static_PSNEPServerLinkReleaseCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult)
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);  /* can not be null */

   PDebugTrace("static_PSNEPServerLinkReleaseCallback %d | ServerSocketUsageCount : %d\n", nResult, pSNEPServer->nServerSocketUsageCount);

   if(pSNEPServer->bConnectionPending != W_FALSE)
   {
      PDebugTrace("static_PSNEPServerLinkReleaseCallback : Connection pending");
      return;
   }

   if (pSNEPServer->hLink != W_NULL_HANDLE)
   {
      PBasicCloseHandle(pContext, pSNEPServer->hLink);
      pSNEPServer->hLink = W_NULL_HANDLE;
   }

   if (pSNEPServer->nServerSocketUsageCount > 0)
   {

      if(pSNEPServer->pLastMessageHandlerCalled == null)
      {
         W_HANDLE hLink;

         pSNEPServer->bConnectionPending = W_TRUE;
         PDebugTrace("static_PSNEPServerLinkReleaseCallback : request new link establishment");
         hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PSNEPServerLinkEstablishmentCallback, null);

         if (hLink != W_NULL_HANDLE)
         {
            PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PSNEPServerLinkReleaseCallback, null, &pSNEPServer->hLinkOperation);
         }
         else
         {
            PDebugError("static_PSNEPServerLinkReleaseCallback : unable to request new link establishemnt : TROUBLE AHEAD");
         }
      }
      else
      {
         PDebugTrace("static_PSNEPServerLinkReleaseCallback : the WorkPerformed must be done");
      }
   }
   else
   {
      /* Perform final cleanup */
      PDebugTrace("static_PSNEPServerLinkReleaseCallback : no more listener");
   }
}

/*
 * This function is called when a P2P SNEP client connected to the server
 *
 * On success, typically starts receiving data.
 */

static void static_PSNEPServerSocketConnected( tContext* pContext, void * pCallbackParameter,  W_ERROR nResult )
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);

   /* Connection done */
   pSNEPServer->bConnectionPending = W_FALSE;

   if (nResult == W_SUCCESS)
   {
      /* The socket is now connected : the SNEP protocol starts here */

      /* Set the SNEP reception State to Awaiting the SNEP Header */
      pSNEPServer->nSNEPReceptionState = AWAITING_SNEP_HEADER;

      /* Start receiving DATA on the socket */
      PP2PReadDriverInternal(pContext, pSNEPServer->hServerSocket, static_SNEPServerDataReceived, null, pSNEPServer->aReceiveBuffer, sizeof(pSNEPServer->aReceiveBuffer), &pSNEPServer->hReceiveOperation);
   }
   else
   {
      PDebugError("static_PSNEPServerSocketConnected: nResult = 0x%08X", nResult);

      /* possible reasons to reach this point are :
       * - P2P link failure     : all stuff is managed in the link release callback
       * - P2P sockert connection aborted  : we are stopping the SNEP server
       */
   }
}


static void static_SNEPServerDataReceived( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nResult )
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);
   bool_t bPostCallback = W_FALSE;
   bool_t bReadMoreData = W_FALSE;
   bool_t bPostResponse = W_FALSE;
   uint32_t nReceivedDataLength = 0;

   if ( (W_SUCCESS != nResult) || (pSNEPServer->bShutdownRequested == W_TRUE))
   {
      PDebugError("static_SNEPServerDataReceived: nResult = 0x%08X", nResult);

      /* possible reasons to reach this point are :
       * - P2P link failure     : all stuff is managed in the link release callback
       * - P2P socket connection has been closed
       * - the shutdown of the connection has been asked
       */

      if (pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer != null)
      {
         /* Free the allocated message (if any) */
         CMemoryFree(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer);
         pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer = null;

         pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength = 0;
      }

      return;
   }

   /* Setup the common parts of the transmit buffer in preperation for response */

   /* First add the version of the SNEP protocol that is supported */
   pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_VERSION_POS] = (SNEP_PROTOCOL_MAJOR_VERSION & SNEP_PROTOCOL_MAJOR_VERSION_MASK) |
               (SNEP_PROTOCOL_MINOR_VERSION & SNEP_PROTOCOL_MINOR_VERSION_MASK);

   /* Add the information field length which is always 0 for the default server responses */
   PUtilWriteUint32ToBigEndianBuffer(0x00000000, &(pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_LEN_MSB_POS]));

   /* Process the received data according to the SNEP protocol... */
   switch(pSNEPServer->nSNEPReceptionState)
   {
      case AWAITING_SNEP_HEADER:

         PDebugTrace("static_SNEPServerDataReceived: AWAITING_SNEP_HEADER");

         /* The SNEP data received must be at least the size of the SNEP protocol header for this to be valid */
         if (nDataLength < SNEP_PROTOCOL_HEADER_LENGTH)
         {
            PDebugError("static_SNEPServerDataReceived: AWAITING_SNEP_HEADER : data too short %d", nResult);

            /* Invalid data length so reject the packet as a Bad Request */
            pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS] = RESPONSE_BAD_REQUEST;
            bPostResponse = W_TRUE;
         }
         else
         {
            uint32_t nSNEPInfoLength = 0;

            /* The received data is a valid length, check the header */
            if ((pSNEPServer->aReceiveBuffer[SNEP_PROTOCOL_HEADER_VERSION_POS] & SNEP_PROTOCOL_MAJOR_VERSION_MASK) != SNEP_PROTOCOL_MAJOR_VERSION)
            {
               /* The major version differs between the server and the client. Section 2.3 of the specification states that server may
                  return unsupported version response*/

               PDebugError("static_SNEPServerDataReceived: AWAITING_SNEP_HEADER : unsupported version %d", nResult);

               pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS] = RESPONSE_UNSUPPORTED_VERSION;
               bPostResponse = W_TRUE;
            }
            else
            {
               /* Save the length of the NDEF message to be received and a allocate a buffer large enough to contain it */
               nReceivedDataLength = PUtilReadUint32FromBigEndianBuffer(&pSNEPServer->aReceiveBuffer[SNEP_PROTOCOL_HEADER_LEN_MSB_POS]);

               /* Check that the length is not greater than that we support*/

               CDebugAssert(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer == null);

               if ((nReceivedDataLength <= SNEP_DEFAULT_SERVER_MAX_INFO_BYTES) &&
                   (pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer = (uint8_t* ) CMemoryAlloc(nReceivedDataLength)) != null)
               {
                  pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength = nReceivedDataLength;

                  /* Check if any data has been received within this fragment */
                  nSNEPInfoLength = nDataLength - SNEP_PROTOCOL_HEADER_LENGTH;

                  if (nSNEPInfoLength != 0)
                  {
                     /* Copy the data to the received buffer that was allocated */
                     CMemoryCopy(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer,
                        &(pSNEPServer->aReceiveBuffer[SNEP_PROTOCOL_HEADER_LENGTH]),
                        nSNEPInfoLength);

                     /* Update the buffer position index */
                     pSNEPServer->sReceivedNDEFMessage.nNDEFBufferIndex = nSNEPInfoLength;

                     if (nSNEPInfoLength == pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength)
                     {
                        /* All of the NDEF data was contained within this packet so set the Success response */
                        pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS] = RESPONSE_SUCCESS;
                        pSNEPServer->nSNEPReceptionState = AWAITING_SNEP_HEADER;

                        bPostCallback = W_TRUE;
                        bPostResponse = W_TRUE;
                     }
                     else
                     {
                        /* Further fragments of data are expected so set the Continue response */
                        pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS] = RESPONSE_CONTINUE;
                        pSNEPServer->nSNEPReceptionState = AWAITING_SNEP_FRAGMENT;

                        bReadMoreData = W_TRUE;
                     }
                  }
               }
               else
               {
                  PDebugError("static_SNEPServerDataReceived: AWAITING_SNEP_HEADER : data too large %d", nResult);

                  /* There is more data than we support */
                  pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS] = RESPONSE_REJECT;
                  bPostResponse = W_TRUE;
               }
            }
         }

         bPostResponse = W_TRUE;
         break;

      case AWAITING_SNEP_FRAGMENT:


         PDebugTrace("static_SNEPServerDataReceived: AWAITING_SNEP_FRAGMENT");

         /* If the amount of data received can be placed within the NDEF buffer do so */
         if ((pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength - pSNEPServer->sReceivedNDEFMessage.nNDEFBufferIndex) >= nDataLength)
         {
            /* Copy the data to the received NDEF buffer */
            CMemoryCopy(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer + pSNEPServer->sReceivedNDEFMessage.nNDEFBufferIndex, pSNEPServer->aReceiveBuffer, nDataLength);

            /* Update the buffer position index */
            pSNEPServer->sReceivedNDEFMessage.nNDEFBufferIndex += nDataLength;

            if (pSNEPServer->sReceivedNDEFMessage.nNDEFBufferIndex == pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength)
            {
               PDebugTrace("static_SNEPServerDataReceived: AWAITING_SNEP_FRAGMENT : message completed");

               /* The complete NDEF message has been received */
               pSNEPServer->nSNEPReceptionState = AWAITING_SNEP_HEADER;

               pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS] = RESPONSE_SUCCESS;
               bPostResponse = W_TRUE;
               bPostCallback = W_TRUE;
            }
            else
            {
               bReadMoreData = W_TRUE;
            }
         }
         else
         {
            PDebugTrace("static_SNEPServerDataReceived: AWAITING_SNEP_FRAGMENT : data too large");

            /* The client has sent too much data, send a response to indicate this */
            pSNEPServer->aTransmitBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS] = RESPONSE_EXCESS_DATA;
            bPostResponse = W_TRUE;

            /* Free the allocated message (if any) */
            CMemoryFree(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer);
            pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer = null;

            pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength = 0;
         }
         break;

      default:
         /* Should not get here as the value of pSNEPServer->nSNEPReceptionState should always be AWAITING_SNEP_HEADER or AWAITING_SNEP_FRAGMENT */
         CDebugAssert(0);
         break;
   }

   if (bPostResponse != W_FALSE)
   {
      /* Send the Response Packet */
      PP2PWriteDriverInternal(pContext,pSNEPServer->hServerSocket, static_SNEPServerDataTransmitted, null, pSNEPServer->aTransmitBuffer, SNEP_PROTOCOL_HEADER_LENGTH, &pSNEPServer->hTransmitOperation);
   }

   /* Setup another read if we are expecting further data */
   if (bReadMoreData == W_TRUE)
   {
      PP2PReadDriverInternal(pContext, pSNEPServer->hServerSocket, static_SNEPServerDataReceived, null, pSNEPServer->aReceiveBuffer, sizeof(pSNEPServer->aReceiveBuffer), &pSNEPServer->hReceiveOperation);
   }

   if (bPostCallback == W_TRUE)
   {
      /* When a NDEF message has been received, call all the registered callbacks
         The message will be freed only once all handlers have successfully retreived the message content
         Once the message is freed, restart reading data from the socket connection */

      tPSNEPMessageHandler * pCurrentSNEPMessageHandler;

      pCurrentSNEPMessageHandler = pSNEPServer->pFirstMessageHandler;

      while (pCurrentSNEPMessageHandler != null)
      {
         pCurrentSNEPMessageHandler->bIsCalled = W_FALSE;
         pCurrentSNEPMessageHandler = pCurrentSNEPMessageHandler->pNext;
      }

      pCurrentSNEPMessageHandler = pSNEPServer->pFirstMessageHandler;

      /* Save the current Message Handler called */
      pSNEPServer->pLastMessageHandlerCalled = pCurrentSNEPMessageHandler;

      if (pCurrentSNEPMessageHandler != null)
      {
         pCurrentSNEPMessageHandler->bIsCalled = W_TRUE;
         PDFCDriverPostCC3(pCurrentSNEPMessageHandler->pDriverCC, pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength, W_SUCCESS);
      }
      else
      {
         CMemoryFree(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer);
         pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer = null;
         pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength = 0;
         pSNEPServer->sReceivedNDEFMessage.nNDEFBufferIndex = 0;
      }
   }
}

static void static_SNEPServerDataTransmitted( tContext* pContext, void * pCallbackParameter, W_ERROR nResult )
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);

   PDebugTrace("static_SNEPServerDataTransmitted %d", nResult);

   if ( (W_SUCCESS != nResult) || (pSNEPServer->bShutdownRequested == W_TRUE))
   {

      /* Possible causes to reach this point are :
         - RF connection has been broken : will be handled on new link establishment
         - P2P socket connection has been closed : will be handled on new link establishment
         - The shutdown of this connection has been asked
         */

      PDebugError("static_SNEPServerDataTransmitted: nResult = 0x%08X", nResult);

      /* Free the allocated message (if any) */
      CMemoryFree(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer);
      pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer = null;

      pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength = 0;
   }
}

static void static_PSNEPServerShutdownConnectionCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult )
{
   PDebugTrace("static_PSNEPServerShutdownConnectionCallback %d", nResult);

   static_PNDEFMessageHandlerDestroyAutomaton(pContext, (tPSNEPMessageHandler *) pCallbackParameter);
}

/** See header */
W_ERROR PNDEFRetrieveSNEPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength)
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);

   PDebugTrace("PNDEFRetrieveSNEPMessageDriver");

   if (pBuffer != null)
   {
      if (nBufferLength >= pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength)
      {
         /* copy the message content in the buffer */
         CMemoryCopy(pBuffer, pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer, pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength);
      }
      else
      {
         PDebugError("PNDEFRetrieveSNEPMessageDriver : buffer too short");

         /* The supplied buffer is not large enough to handle the received NDEF Message */
         return W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   return W_SUCCESS;
}

/** See header */
void PNDEFSetWorkPerformedSNEPDriver(
      tContext * pContext,
      bool_t     bGiveToNextListener)
{
   tSNEPServerDriverInstance * pSNEPServer = PContextGetSNEPServerDriverInstance(pContext);
   tPSNEPMessageHandler * pCurrentSNEPMessageHandler = null;

   /* set the SNEPMessageHandler to call */
   if (bGiveToNextListener != W_FALSE)
   {
      pCurrentSNEPMessageHandler = pSNEPServer->pFirstMessageHandler;

      while ((pCurrentSNEPMessageHandler != null) && (pCurrentSNEPMessageHandler->bIsCalled != W_FALSE))
      {
         pCurrentSNEPMessageHandler = pCurrentSNEPMessageHandler->pNext;
      }
   }

   /* Store the new last Message Handler */
   pSNEPServer->pLastMessageHandlerCalled = pCurrentSNEPMessageHandler;

   if (pCurrentSNEPMessageHandler != null)
   {
      PDebugTrace("PNDEFSetWorkPerformedSNEPDriver : call next handler");

      pCurrentSNEPMessageHandler->bIsCalled = W_TRUE;
      PDFCDriverPostCC3(pCurrentSNEPMessageHandler->pDriverCC, pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength, W_SUCCESS);
   }
   else
   {
      tUserInstance * pCurrentUserInstance;
      PDebugTrace("PNDEFSetWorkPerformedSNEPDriver : no more handler to be called");

      pCurrentUserInstance = PContextGetCurrentUserInstance(pContext);
      PContextSetCurrentUserInstance(pContext, null);

      /* Free off the memory */
      CMemoryFree(pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer);
      pSNEPServer->sReceivedNDEFMessage.pReceiveBuffer = null;
      pSNEPServer->sReceivedNDEFMessage.nNDEFMessageLength = 0;
      pSNEPServer->sReceivedNDEFMessage.nNDEFBufferIndex = 0;

      PDebugTrace("PNDEFSetWorkPerformedSNEPDriver : continue processing new messages");

      if(pSNEPServer->hLink == W_NULL_HANDLE)
      {
          W_HANDLE hLink;

         PDebugTrace("PNDEFSetWorkPerformedSNEPDriver : request new link establishment");

         hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PSNEPServerLinkEstablishmentCallback, null);

         if (hLink != W_NULL_HANDLE)
         {
            PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PSNEPServerLinkReleaseCallback, null, &pSNEPServer->hLinkOperation);
         }
         else
         {
            PDebugError("PNDEFSetWorkPerformedSNEPDriver : unable to request new link establishemnt : TROUBLE AHEAD");
         }
      }
      else
      {
         PDebugTrace("PNDEFSetWorkPerformedSNEPDriver : do PP2PReadDriverInternal => %x", pSNEPServer->hServerSocket);
         PP2PReadDriverInternal(pContext, pSNEPServer->hServerSocket, static_SNEPServerDataReceived, null, pSNEPServer->aReceiveBuffer, sizeof(pSNEPServer->aReceiveBuffer), &pSNEPServer->hReceiveOperation);
      }

      PContextSetCurrentUserInstance(pContext, pCurrentUserInstance);
   }
}



/*******************************************************************************
   Below the implementation of the SNEP client
*******************************************************************************/


static void static_PNDEFSendSNEPMessageDriverCancel(tContext* pContext, void* pCancelParameter, bool_t bIsClosing);

static void static_PSNEPClientLinkEstablishmentCallback( tContext* pContext, void * pCallbackParameter, W_HANDLE hLink, W_ERROR nResult );
static void static_PSNEPClientLinkReleaseCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult);
static void static_PSNEPClientSocketConnected( tContext* pContext, void * pCallbackParameter,  W_ERROR nResult );

static void static_SNEPClientDataReceived( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nResult );
static void static_SNEPClientDataTransmitted( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );
static void static_PSNEPClientShutdownConnectionCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );
static void static_SNEPClientAutomaton( tContext* pContext);
static void static_SNEPClientPostResult(tContext* pContext, tPSNEPMessage * pMessage, W_ERROR nError);
static void static_PSNEPClientGeneralFailure(tContext * pContext,  void * pCallbackParameter, W_ERROR nResult);

static void    static_SNEPClientReconnect( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );



W_HANDLE PNDEFSendSNEPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);
   tPSNEPMessage * pMessage;
   tPSNEPMessage * pCurrentSNEPMessage = pSNEPClient->pFirstMessage;
   W_HANDLE        hOperation = W_NULL_HANDLE;
   tDFCDriverCCReference pDriverCC;
   W_ERROR       nError;

   PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pDriverCC);

   /* Allocate a data structure that will contain all stuff specific to this message
      the common stuff (socket, P2P link are stored in the context itself...  */

   pMessage = (tPSNEPMessage *) CMemoryAlloc(sizeof(tPSNEPMessage));

   if (pMessage == null)
   {
      /* either if use of goto usage is quite controversial, we use them for error management in the Open NFC stack */
      PDebugError("PNDEFSendSNEPMessageDriver : pMessage == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   CMemoryFill(pMessage, 0, sizeof(tPSNEPMessage));

   pMessage->nLength = nBufferLength;

   if (nBufferLength != 0)
   {
      pMessage->pBuffer = (uint8_t *) CMemoryAlloc(nBufferLength);

      if (pMessage->pBuffer == null)
      {
        /* either if use of goto usage is quite controversial, we use them for error management in the Open NFC stack */
        PDebugError("PNDEFSendSNEPMessageDriver : pMessage->pBuffer == null");
        nError = W_ERROR_OUT_OF_RESOURCE;
        goto error;
      }

      CMemoryCopy(pMessage->pBuffer, pBuffer, nBufferLength);
   }

   pMessage->pDriverCC = pDriverCC;

   /* Creates a new handle associated to the send message operation. This handle is returned in the phRegistry parameter.
      This handle is used for unregistration of the listener */

   hOperation = PBasicCreateOperation(pContext, static_PNDEFSendSNEPMessageDriverCancel, pMessage);

   if (hOperation == W_NULL_HANDLE)
   {
      PDebugError("PNDEFSendSNEPMessageDriver : PBasicCreateOperation failed");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   pMessage->hOperation = hOperation;

   /* create only one socket for all registered handlers */

   if (pSNEPClient->hClientSocket == W_NULL_HANDLE)
   {
      tUserInstance * pCurrentUserInstance;
      tWP2PConfiguration sConfiguration;
      W_HANDLE hLink;

      /* We must keep the number of registrations :
         each time a new registration is done, the usage count is incremented.
         the socket must be closed and the link establishment must be destroyed  when the usage count drops to zero */

      CDebugAssert(pSNEPClient->hClientSocket == W_NULL_HANDLE);
      CDebugAssert(pSNEPClient->hLinkOperation == W_NULL_HANDLE);
      CDebugAssert(pSNEPClient->hLink == W_NULL_HANDLE);
      CDebugAssert(pSNEPClient->hReceiveOperation == W_NULL_HANDLE);
      CDebugAssert(pSNEPClient->hTransmitOperation== W_NULL_HANDLE);


      /* Handles are by default associated to the current user instance, meaning they are automatically closed
         * when the user instance is destroyed. Here, we only create one socket and make only one link establishment
         * requets for several clients, so we need to drop temporary the current instance for these operations.
         *
         * NOTE: The user instance must be restored at the end
         */

      pCurrentUserInstance = PContextGetCurrentUserInstance(pContext);
      PContextSetCurrentUserInstance(pContext, null);

      PDebugTrace("PNDEFSendSNEPMessageDriver : starting new session");

      pSNEPClient->bShutdownRequested = W_FALSE;
      pSNEPClient->bConnectionPending = W_TRUE;
      pSNEPClient->bClientConnected   = W_FALSE;

      nError = PP2PCreateSocketDriver(pContext, W_P2P_TYPE_CLIENT, g_SNEPUri, sizeof(g_SNEPUri), 0, &pSNEPClient->hClientSocket);

      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFSendSNEPMessageDriver : PP2PCreateSocketDriver failed %d", nError);
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto error;
      }

      nError = PP2PGetConfigurationDriver(pContext, &sConfiguration, sizeof(sConfiguration));
      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFSendSNEPMessageDriver : PP2PGetConfigurationDriver failed %d", nError);
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto error;
      }

      nError = PP2PSetSocketParameter(pContext, pSNEPClient->hClientSocket, W_P2P_LOCAL_MIU, sConfiguration.nLocalMIU);
      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFSendSNEPMessageDriver : PP2PSetSocketParameter filed %d", nError);
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto error;
      }

      /* Request link establishment */

      /* Since we are in the driver part of the stack, we must call internal function instead of PP2PEstablishLinkDriverInternal()
         which can only be called from user part
      */
      hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PSNEPClientLinkEstablishmentCallback, null);

      if (hLink == W_NULL_HANDLE)
      {
         PDebugError("PNDEFSendSNEPMessageDriver : PP2PEstablishLinkDriver1Internal failed");
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto error;
      }

      PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PSNEPClientLinkReleaseCallback, null, &pSNEPClient->hLinkOperation);

      /* Restore the current user instance */
      PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
   }

   pMessage->nError = W_SUCCESS;


   /* enqueue the newly created pMessageHandler in the context list */
   if (pSNEPClient->pFirstMessage == null)
   {

     PDebugTrace("PNDEFSendSNEPMessageDriver : pending message list is empty");

      pSNEPClient->pFirstMessage = pMessage;

      if (pSNEPClient->bClientConnected != W_FALSE)
      {
       PDebugTrace("PNDEFSendSNEPMessageDriver : start message transmission");

         /* the SNEP protocol starts here */
         pSNEPClient->nSNEPTransmissionState = SENDING_SNEP_HEADER;
         static_SNEPClientAutomaton(pContext);
      }
   }
   else
   {

     PDebugTrace("PNDEFSendSNEPMessageDriver : enqueue the message");

      pCurrentSNEPMessage = pSNEPClient->pFirstMessage;

      while (pCurrentSNEPMessage->pNext != null)
      {
         pCurrentSNEPMessage = pCurrentSNEPMessage->pNext;
      }

      pCurrentSNEPMessage->pNext = pMessage;
   }

   return hOperation;

error:
   if (hOperation == W_NULL_HANDLE)
   {
      if (pMessage != null)
      {
         if (pMessage->pBuffer != null)
         {
            CMemoryFree(pMessage->pBuffer);
            pMessage->pBuffer = null;

         }

         CMemoryFree(pMessage);
      }

      PDFCDriverPostCC2(pDriverCC, nError);
   }
   else
   {
      PBasicCloseHandle(pContext, hOperation);
      hOperation = W_NULL_HANDLE;

      pMessage->nError = nError;
      static_SNEPClientPostResult(pContext, pMessage, nError);
   }

   return W_NULL_HANDLE;
}

/*
 * This is the automaton of the destruction of the send message operation
 * The callback is called once all cleanup have been performed.
 */

static void static_PNDEFMessageDestroyAutomaton(tContext* pContext, tPSNEPMessage * pMessage)
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);

   /* Request shutdown of the socket connection */
   if ((pSNEPClient->hClientSocket != W_NULL_HANDLE) && (pSNEPClient->bShutdownRequested == W_FALSE))
   {
      PDebugTrace("static_PNDEFMessageDestroyAutomaton : terminating client socket connection");
      PP2PShutdownDriverInternal(pContext, pSNEPClient->hClientSocket, static_PSNEPClientShutdownConnectionCallback, pMessage);
      pSNEPClient->bShutdownRequested = W_TRUE;

      return;
   }
   /* close the server socket */
   if (pSNEPClient->hClientSocket != W_NULL_HANDLE)
   {
      PDebugTrace("static_PNDEFMessageDestroyAutomaton : closing client socket");
      PBasicCloseHandle(pContext, pSNEPClient->hClientSocket);
      pSNEPClient->hClientSocket = W_NULL_HANDLE;
   }

   if (pSNEPClient->hLink != W_NULL_HANDLE)
   {
      /* request the termination of the P2P RF link if established */
      PDebugTrace("static_PNDEFMessageDestroyAutomaton : closing link");
      PBasicCloseHandle(pContext, pSNEPClient->hLink);
      pSNEPClient->hLink = W_NULL_HANDLE;
   }

   if(pSNEPClient->hLinkOperation != W_NULL_HANDLE)
   {
         /* Cancel link establishment if the RF link is not established */
         PDebugTrace("static_PNDEFMessageDestroyAutomaton : aborting link establishment");
         PBasicCancelOperation(pContext, pSNEPClient->hLinkOperation);
         PBasicCloseHandle(pContext, pSNEPClient->hLinkOperation);

         pSNEPClient->hLinkOperation = W_NULL_HANDLE;
   }

   if (pMessage != null)
   {
      /* Post the callback */
      PDFCDriverPostCC2(pMessage->pDriverCC, W_ERROR_CANCEL);

      if (pMessage->pBuffer != null)
      {
        CMemoryFree(pMessage->pBuffer);
      }

      CMemoryFree(pMessage);
   }

   PDebugTrace("static_PNDEFMessageDestroyAutomaton : completed");
}


static void static_PSNEPClientShutdownConnectionCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult )
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);

   pSNEPClient->bClientConnected = W_FALSE;

   PDebugTrace("static_PSNEPClientShutdownConnectionCallback %d", nResult);

   CDebugAssert(pSNEPClient->pFirstMessage == null);
   static_PNDEFMessageDestroyAutomaton(pContext, (tPSNEPMessage *) pCallbackParameter);
}


/*
 * This function is called when an error occurs or when the user cancels the operation
 */
static void static_PNDEFSendSNEPMessageDriverCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);
   tPSNEPMessage * pMessage = (tPSNEPMessage *) pCancelParameter;

   /* Save and replace the current instance by null */
   /* The current instance will be restored at the end of the process */
   tUserInstance * pUserInstance = PContextGetCurrentUserInstance( pContext );
   PContextSetCurrentUserInstance(pContext, null);

   CDebugAssert(pCancelParameter != null);

   PDebugTrace("static_PNDEFSendSNEPMessageDriverCancel %d", bIsClosing);

   if ((bIsClosing == W_FALSE) /* Cancel operation done */ || (pMessage->nError != W_SUCCESS))
   {
      /* Remove the pMessage if it was called before */
      if( pSNEPClient->pFirstMessage == pMessage)
      {
         pSNEPClient->pFirstMessage = pMessage->pNext;
      }
      else
      {
         tPSNEPMessage * pTmp = pSNEPClient->pFirstMessage;

         CDebugAssert(pSNEPClient->pFirstMessage != null);

         /* Search the pMessage in the list */
         while(pTmp->pNext != null)
         {
            /* if the next message is the message to remove */
            if(pTmp->pNext == pMessage)
            {
               pTmp->pNext = pMessage->pNext;
               break;
            }
         }
      }

      /* Post the callback now */
      if (pMessage->nError != W_SUCCESS)
      {
         PDFCDriverPostCC2(pMessage->pDriverCC, pMessage->nError);
      }
      else
      {
         PDFCDriverPostCC2(pMessage->pDriverCC, W_ERROR_CANCEL);
      }

      /* Free the resources */
      if (pMessage->pBuffer != null)
      {
         CMemoryFree(pMessage->pBuffer);
      }

      CMemoryFree(pMessage);

      if (pSNEPClient->pFirstMessage == null)
      {
         PDebugTrace("static_PNDEFSendSNEPMessageDriverCancel : no more client registered");
         static_PNDEFMessageDestroyAutomaton(pContext, null);
      }

   }

   PContextSetCurrentUserInstance(pContext, pUserInstance);
}

/**
 * This function is called when the P2P link is established.
 *
 * On success, we must try to establish a socket connection with the peer device
 * On failure, depending on the existence of at least one registered handler, we must request link establishment once again
 */

static void static_PSNEPClientLinkEstablishmentCallback( tContext* pContext, void * pCallbackParameter, W_HANDLE hLink, W_ERROR nResult )
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);  /* can not be null */

   PBasicCloseHandle(pContext, pSNEPClient->hLinkOperation);
   pSNEPClient->hLinkOperation = W_NULL_HANDLE;

   if (nResult == W_SUCCESS)
   {
      /* Cache the hink handle */
      pSNEPClient->hLink = hLink;

      PDebugTrace("static_PSNEPClientLinkEstablishmentCallback : request SNEP client connection");

      /* The link has been activated, we must start accepting connections on the server socket */
      PP2PConnectDriverInternal(pContext, pSNEPClient->hClientSocket, hLink, static_PSNEPClientSocketConnected, null);
   }
   else
   {
      PDebugError("static_PSNEPClientLinkEstablishmentCallback : P2P link establishment failed %d", nResult);

      static_PSNEPClientGeneralFailure(pContext, null, nResult);
   }
}


/*
 * This function is called when the P2P link is broken
 *
 * Depending on the existence of at least one registered handler, we must request link establishment once again
 * or perform final cleanup (if any)
 */

static void static_PSNEPClientLinkReleaseCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult)
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);  /* can not be null */

   PDebugTrace("static_PSNEPClientLinkReleaseCallback %d\n", nResult);

   pSNEPClient->bClientConnected = W_FALSE;

   static_PSNEPClientGeneralFailure(pContext, null, nResult);
}

/*
 * This function is called when the P2P socket is connected
 *
 * Depending on the existence of at least one registered handler, we must request link establishment once again
 * or perform final cleanup (if any)
 */

static void static_PSNEPClientSocketConnected( tContext* pContext, void * pCallbackParameter,  W_ERROR nResult )
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);
   W_ERROR nError;

   PDebugTrace("static_PSNEPClientSocketConnected %d", nResult);


   pSNEPClient->bClientConnected = W_TRUE;
   if (nResult == W_SUCCESS)
   {
      /* The socket is now connected : */

      /* retreive the maximum packet size that we can send to the peer */
      nError = PP2PGetSocketParameterDriver(pContext, pSNEPClient->hClientSocket, W_P2P_REMOTE_MIU, &pSNEPClient->nSocketMIU);

      if (nError != W_SUCCESS)
      {
         PDebugError("static_PSNEPClientSocketConnected: PP2PGetSocketParameterDriver failed %d", nError);
         pSNEPClient->nSocketMIU = 128;
      }

      if (pSNEPClient->nSocketMIU > sizeof(pSNEPClient->aTransmitBuffer))
      {
         pSNEPClient->nSocketMIU = sizeof(pSNEPClient->aTransmitBuffer);
      }

      pSNEPClient->bClientConnected = W_TRUE;
      pSNEPClient->bConnectionPending = W_FALSE;

      /* the SNEP protocol starts here */
      pSNEPClient->nSNEPTransmissionState = SENDING_SNEP_HEADER;
      static_SNEPClientAutomaton(pContext);
   }
   else
   {
      PDebugError("static_PSNEPClientSocketConnected: nResult = 0x%08X", nResult);

      static_PSNEPClientGeneralFailure(pContext, null, nResult);
   }
}

/*
 * This function implements the SNEP client PUT automaton
 *
 */

static void static_SNEPClientAutomaton( tContext* pContext)
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);
   tUserInstance * pUserInstance;
   bool_t bSendPacket = W_FALSE;
   bool_t bReceivePacket = W_FALSE;
   uint32_t nLength = 0;

   CDebugAssert(pSNEPClient->pFirstMessage != null);

   /* First add the version of the SNEP protocol that is supported */
   pSNEPClient->aTransmitBuffer[SNEP_PROTOCOL_HEADER_VERSION_POS] = (SNEP_PROTOCOL_MAJOR_VERSION & SNEP_PROTOCOL_MAJOR_VERSION_MASK) |
               (SNEP_PROTOCOL_MINOR_VERSION & SNEP_PROTOCOL_MINOR_VERSION_MASK);

   /* Process the received data according to the SNEP protocol... */
   switch (pSNEPClient->nSNEPTransmissionState)
   {
      case SENDING_SNEP_HEADER:

         PDebugTrace("static_SNEPClientAutomaton: SENDING_SNEP_HEADER");

       if (pSNEPClient->pFirstMessage->nLength != 0)
       {

          pSNEPClient->aTransmitBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS] = REQUEST_PUT;
          PUtilWriteUint32ToBigEndianBuffer(pSNEPClient->pFirstMessage->nLength, &(pSNEPClient->aTransmitBuffer[SNEP_PROTOCOL_HEADER_LEN_MSB_POS]));

          nLength = (SNEP_PROTOCOL_HEADER_LENGTH + pSNEPClient->pFirstMessage->nLength <= pSNEPClient->nSocketMIU) ? pSNEPClient->pFirstMessage->nLength : pSNEPClient->nSocketMIU - SNEP_PROTOCOL_HEADER_LENGTH;

          CMemoryCopy(& pSNEPClient->aTransmitBuffer[SNEP_PROTOCOL_HEADER_LENGTH], pSNEPClient->pFirstMessage->pBuffer, nLength);
          pSNEPClient->pFirstMessage->nIndex = nLength;

          if (pSNEPClient->pFirstMessage->nIndex == pSNEPClient->pFirstMessage->nLength)
          {
            PDebugTrace("static_SNEPClientAutomaton: SENDING_SNEP_HEADER : complete message");
            pSNEPClient->nSNEPTransmissionState = AWAITING_SNEP_ANSWER_SUCCESS;
          }
          else
          {
            PDebugTrace("static_SNEPClientAutomaton : SENDING_SNEP_HEADER : fragmented message");
            pSNEPClient->nSNEPTransmissionState = AWAITING_SNEP_ANSWER_CONTINUE;
          }

          nLength += SNEP_PROTOCOL_HEADER_LENGTH;
          bSendPacket = W_TRUE;
          bReceivePacket = W_TRUE;
       }
       else
       {
          static_SNEPClientPostResult(pContext, pSNEPClient->pFirstMessage, W_SUCCESS);
       }

         break;

      case SENDING_SNEP_FRAGMENT:

         PDebugTrace("static_SNEPClientAutomaton: SENDING_SNEP_FRAGMENT");

         PDebugTrace("static_SNEPClientAutomaton: current index %d - total length %d", pSNEPClient->pFirstMessage->nIndex, pSNEPClient->pFirstMessage->nLength);
         nLength = pSNEPClient->pFirstMessage->nLength - pSNEPClient->pFirstMessage->nIndex;

         if (nLength > pSNEPClient->nSocketMIU)
         {
            nLength = pSNEPClient->nSocketMIU;
         }

         CMemoryCopy(pSNEPClient->aTransmitBuffer, pSNEPClient->pFirstMessage->pBuffer + pSNEPClient->pFirstMessage->nIndex, nLength);
         pSNEPClient->pFirstMessage->nIndex += nLength;
         bSendPacket = W_TRUE;

         if (pSNEPClient->pFirstMessage->nIndex == pSNEPClient->pFirstMessage->nLength)
         {
            PDebugTrace("static_SNEPClientAutomaton: SENDING_SNEP_FRAGMENT : complete message");
            bReceivePacket = W_TRUE;

            pSNEPClient->nSNEPTransmissionState = AWAITING_SNEP_ANSWER_SUCCESS;
         }
         break;

      default:
         /* Should not get here as the value of pSNEClient->nSNEPTransmissionState should always be SENDING_SNEP_HEADER or SENDING_SNEP_FRAGMENT */
         CDebugAssert(0);
         break;
   }

   /* temporary drop current instance since socket is not associated to a specific user */
   pUserInstance = PContextGetCurrentUserInstance(pContext);
   PContextSetCurrentUserInstance(pContext, null);

   if (bSendPacket != W_FALSE)
   {
      PP2PWriteDriverInternal(pContext, pSNEPClient->hClientSocket, static_SNEPClientDataTransmitted, pSNEPClient, pSNEPClient->aTransmitBuffer, nLength, &pSNEPClient->hTransmitOperation);
   }

   if (bReceivePacket != W_FALSE)
   {
      PP2PReadDriverInternal(pContext, pSNEPClient->hClientSocket, static_SNEPClientDataReceived, pSNEPClient, pSNEPClient->aReceiveBuffer, sizeof(pSNEPClient->aReceiveBuffer), &pSNEPClient->hReceiveOperation);
   }

   /* restore the current instance */
   PContextSetCurrentUserInstance(pContext, pUserInstance);
}

/*
 * This function implements the SNEP client PUT automaton
 *
 */

static void static_SNEPClientDataReceived( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nResult )
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);
   uint8_t nResponse;

   PBasicCloseHandle(pContext, pSNEPClient->hReceiveOperation);
   pSNEPClient->hReceiveOperation = W_NULL_HANDLE;

   PDebugTrace("static_SNEPClientDataReceived");

   if( pSNEPClient->bShutdownRequested == W_TRUE)
   {
      PDebugTrace("static_SNEPClientDataReceived shutdown pending");
      /* Do nothing, the suhtdown is pending*/
      return;
   }

   if(pSNEPClient->bConnectionPending)
   {
      PDebugTrace("static_SNEPClientDataReceived connection pending");
      /* Do nothing, the connection is pending*/
      return;
   }

   if (nResult != W_SUCCESS)
   {
      if(nResult == W_ERROR_BAD_STATE)
      {
         /* Server closed the connection */
         static_SNEPClientReconnect(pContext, null, W_SUCCESS);
      }
      else
      {
         /* A read operation failure means the client is no longer connected */
         static_PSNEPClientGeneralFailure(pContext, null, nResult);
      }
      return;
   }

   if (nDataLength != SNEP_PROTOCOL_HEADER_LENGTH)
   {
      static_PSNEPClientGeneralFailure(pContext, null, W_ERROR_BAD_TAG_FORMAT);
      return;
   }

   /* The received data is a valid length, check the header */
   if ((pSNEPClient->aReceiveBuffer[SNEP_PROTOCOL_HEADER_VERSION_POS] & SNEP_PROTOCOL_MAJOR_VERSION_MASK) != SNEP_PROTOCOL_MAJOR_VERSION)
   {
      /* The major version differs between the server and the client */
      static_PSNEPClientGeneralFailure(pContext, null, W_ERROR_BAD_TAG_FORMAT);
      return;
   }

   nResponse = pSNEPClient->aReceiveBuffer[SNEP_PROTOCOL_HEADER_REQUEST_POS];

   switch(pSNEPClient->nSNEPTransmissionState)
   {
      case AWAITING_SNEP_ANSWER_SUCCESS:

         if (nResponse == RESPONSE_SUCCESS)
         {
            PDebugTrace("static_SNEPClientDataReceived : SNEP message successfully sent");
            static_SNEPClientPostResult(pContext, pSNEPClient->pFirstMessage, W_SUCCESS);
         }
         else
         {
            PDebugError("static_SNEPClientDataReceived : unexpected answer %d\n", nResponse);
            static_PSNEPClientGeneralFailure(pContext, null, W_ERROR_BAD_TAG_FORMAT);
         }

         break;

      case AWAITING_SNEP_ANSWER_CONTINUE:

         if (nResponse == RESPONSE_CONTINUE)
         {
            PDebugTrace("static_SNEPClientDataReceived : continue to send next fragments");
            pSNEPClient->nSNEPTransmissionState = SENDING_SNEP_FRAGMENT;
            static_SNEPClientAutomaton(pContext);
         }
         else if (nResponse == RESPONSE_REJECT)
         {
            PDebugTrace("static_SNEPClientDataReceived : Tag FULL");
            static_SNEPClientPostResult(pContext, pSNEPClient->pFirstMessage, W_ERROR_TAG_FULL);
         }
         break;

      default:
         /* This should not occur */
         CDebugAssert(0);
         break;
   }
}


static void static_SNEPClientDataTransmitted( tContext* pContext, void * pCallbackParameter, W_ERROR nResult )
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);

   PBasicCloseHandle(pContext, pSNEPClient->hTransmitOperation);
   pSNEPClient->hTransmitOperation = W_NULL_HANDLE;

   PDebugTrace("static_SNEPClientDataTransmitted %d", nResult);

   if( pSNEPClient->bShutdownRequested == W_TRUE)
   {
      PDebugTrace("static_SNEPClientDataTransmitted shutdown pending");
      /* Do nothing, the suhtdown is pending*/
      return;
   }

   if(pSNEPClient->bConnectionPending)
   {
      PDebugTrace("static_SNEPClientDataTransmitted connection pending");
      /* Do nothing, the connection is pending*/
      return;
   }


   if (nResult == W_SUCCESS)
   {
      /* go on message transmission if fragmented */
      if (pSNEPClient->nSNEPTransmissionState == SENDING_SNEP_FRAGMENT)
      {
         static_SNEPClientAutomaton(pContext);
      }
   }
   else

   {
      PDebugError("static_SNEPClientDataTransmitted %d", nResult);

      if(nResult == W_ERROR_BAD_STATE)
      {
         /* Restart the Connection */
         static_SNEPClientReconnect(pContext, null, W_SUCCESS);
      }else
      {
         /* A write operation failure means the client is no longer connected */
         static_PSNEPClientGeneralFailure(pContext, null, nResult);
      }
   }
}


static void static_SNEPClientPostResult(tContext* pContext, tPSNEPMessage * pMessage, W_ERROR nError)
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);
   tPSNEPMessage * pCurrentMessage;
   tPSNEPMessage * pLastMessage;

   PDebugTrace("static_SNEPClientPostResult %d", nError);

   if (pMessage != null)
   {
      /* Remove the message from the list (if present) */

      pCurrentMessage = pSNEPClient->pFirstMessage;
      pLastMessage = null;

      while ((pCurrentMessage != null) && (pCurrentMessage != pMessage))
      {
         pLastMessage = pCurrentMessage;
         pCurrentMessage = pCurrentMessage->pNext;
      }

      if (pCurrentMessage == pMessage)
      {

         if (pLastMessage == null)
         {
            pSNEPClient->pFirstMessage = pMessage->pNext;
         }
         else
         {
            pLastMessage->pNext = pMessage->pNext;
         }
      }

      if (pMessage->hOperation != W_NULL_HANDLE)
      {
         PBasicSetOperationCompleted(pContext, pMessage->hOperation);
      }

      PDFCDriverPostCC2(pMessage->pDriverCC, nError);

      if (pMessage->pBuffer != null)
      {
         CMemoryFree(pMessage->pBuffer);
      }

      CMemoryFree(pMessage);
   }

   /* Send the next message, if any */
   if (pSNEPClient->pFirstMessage != null)
   {

     PDebugTrace("Sending next pending message");
      pSNEPClient->nSNEPTransmissionState = SENDING_SNEP_HEADER;
      static_SNEPClientAutomaton(pContext);
   }
}


static void static_PSNEPClientGeneralFailure(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nResult)
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);

   tPSNEPMessage * pMessage = null;
   tPSNEPMessage * pMessageNext = null;
   bool_t bAtLeastOneMessage = W_FALSE;

   PDebugTrace("static_PSNEPClientGeneralFailure %d", nResult);

   pMessage = pSNEPClient->pFirstMessage;

   while (pMessage != null)
   {
      /* Save the next pMessage */
      pMessageNext = pMessage->pNext;

      pMessage->nError = nResult;

      PBasicCancelOperation(pContext, pMessage->hOperation);
      /* The close of the cancel operation handle must be done by the caller */

      bAtLeastOneMessage = W_TRUE;

      /* Set the current message to the value of the next message */
      pMessage = pMessageNext;
   }

   if (bAtLeastOneMessage == W_FALSE)
   {
      static_PNDEFMessageDestroyAutomaton(pContext, null);
   }
}


/**
 * @brief try to reconnect the P2P Socket
 *
 * @param pContext The current context
 * @param pCallbackParameter
 * @param nResult the result code of the callback
 *
 **/
static void static_SNEPClientReconnect(tContext * pContext, void * pCallbackParameter, W_ERROR nResult)
{
   tSNEPClientDriverInstance * pSNEPClient = PContextGetSNEPClientDriverInstance(pContext);
   tWP2PConfiguration sConfiguration;

   PDebugTrace("static_SNEPClientReconnect : Close P2P Socket ");

   if ((pSNEPClient->hClientSocket != W_NULL_HANDLE) && (pSNEPClient->bConnectionPending == W_FALSE))
   {
      PDebugTrace("static_SNEPClientReconnect : terminating client socket connection");
      PP2PShutdownDriverInternal(pContext, pSNEPClient->hClientSocket, static_SNEPClientReconnect, null);
      pSNEPClient->bConnectionPending = W_TRUE;
      return;
   }

   if(pSNEPClient->hClientSocket != W_NULL_HANDLE)
   {
      PHandleClose(pContext, pSNEPClient->hClientSocket);
      pSNEPClient->hClientSocket = W_NULL_HANDLE;
   }

   if(pSNEPClient->bClientConnected == W_TRUE)
   {
      nResult = PP2PCreateSocketDriver(pContext, W_P2P_TYPE_CLIENT, g_SNEPUri, sizeof(g_SNEPUri), 0, &pSNEPClient->hClientSocket);

      if (nResult != W_SUCCESS)
      {
         PDebugError("static_SNEPClientReconnect : PP2PCreateSocketDriver failed %d", nResult);
         goto error;
      }

      nResult = PP2PGetConfigurationDriver(pContext, &sConfiguration, sizeof(sConfiguration));
      if (nResult != W_SUCCESS)
      {
         PDebugError("static_SNEPClientReconnect : PP2PGetConfigurationDriver failed %d", nResult);
         goto error;
      }

      nResult = PP2PSetSocketParameter(pContext, pSNEPClient->hClientSocket, W_P2P_LOCAL_MIU, sConfiguration.nLocalMIU);
      if (nResult != W_SUCCESS)
      {
         PDebugError("static_SNEPClientReconnect : PP2PSetSocketParameter failed %d", nResult);
         goto error;
      }

      PDebugTrace("static_SNEPClientReconnect : Reconnect");
      /* The link is always activated, we must start accepting connections on the server socket */
      PP2PConnectDriverInternal(pContext, pSNEPClient->hClientSocket, pSNEPClient->hLink, static_PSNEPClientSocketConnected, null);
      return;
   }

error:
   PDebugTrace("static_SNEPClientReconnect : Link released, cannot reconnect the link");
      static_PSNEPClientGeneralFailure(pContext, null, W_ERROR_CANCEL);
}


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC && defined defined P_INCLUDE_SNEP_NPP */

#if ((P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (! defined P_INCLUDE_SNEP_NPP)

/* Dummy functions to allow proper link of the solution even if P_INCLUDE_SNEP_NPP is not defined
   (autogen limitation workaround */

W_ERROR PNDEFRegisterSNEPMessageHandlerDriver  (
      tContext * pContext,
      tPBasicGenericDataCallbackFunction* pHandler,
      void *  pHandlerParameter,
      uint8_t nPriority,
      W_HANDLE *  phRegistry )
{
   if (phRegistry != null)
   {
      * phRegistry = W_NULL_HANDLE;
   }

   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

W_ERROR PNDEFRetrieveSNEPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

W_HANDLE PNDEFSendSNEPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   return W_NULL_HANDLE;
}

void PNDEFSetWorkPerformedSNEPDriver(
      tContext * pContext,
      bool_t     bGiveToNextListener)
{
}

#endif

/* EOF */
