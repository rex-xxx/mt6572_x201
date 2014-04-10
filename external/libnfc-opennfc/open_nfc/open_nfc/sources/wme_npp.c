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
   Contains the implementation of the NPP server
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( NPP )

#include "wme_context.h"


#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_SNEP_NPP)

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC  && defined P_INCLUDE_SNEP_NPP */


#if ((P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_SNEP_NPP)

static uint32_t static_PNPPMessageHandlerDestroyAsync(tContext* pContext, tPBasicGenericCallbackFunction* pCallback, void* pCallbackParameter, void* pObject );


static void static_PNPPServerLinkEstablishmentCallback( tContext* pContext, void * pCallbackParameter, W_HANDLE hLink, W_ERROR nResult );
static void static_PNPPServerLinkReleaseCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult);
static void static_PNPPServerSocketConnected( tContext* pContext, void * pCallbackParameter,  W_ERROR nResult );

static void static_NPPServerDataReceived( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nResult );

static void static_PNPPServerShutdownConnectionCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );


#define P_NPP_SERVER_SAP           16
static const char16_t g_NPPUri[] = { 'c','o','m','.','a','n','d','r','o','i','d','.','n','p','p', 0 };

/* NPP Message handler type :
   This structure defines the different functions to be called to process operation on the
   associated handle. Here we only need the destroy function */
tHandleType g_sNPPMessageHandler = { null, static_PNPPMessageHandlerDestroyAsync, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_NPP_SERVER          (&g_sNPPMessageHandler)

#define NPP_ACTION_PUSH                   0x01
#define NPP_NUMBER_OF_NDEF_ENTRIES        (uint32_t) 1

#define NPP_PROTOCOL_MAJOR_VERSION        0x00
#define NPP_PROTOCOL_MINOR_VERSION        0x01
#define NPP_PROTOCOL_MAJOR_VERSION_MASK   0xF0
#define NPP_PROTOCOL_MINOR_VERSION_MASK   0x0F

#define AWAITING_NPP_HEADER               0
#define AWAITING_NPP_NDEF_MESSAGE         1
#define SENDING_NPP_HEADER                2
#define SENDING_NPP_NDEF_MESSAGE          3


#define NPP_PROTOCOL_HEADER_LENGTH           5
#define NPP_PROTOCOL_HEADER_VERSION_POS      0
#define NPP_PROTOCOL_HEADER_NDEF_ENTRIES_POS 1

#define NPP_PROTOCOL_NDEF_ENTRY_HEADER_LENGTH      5
#define NPP_PROTOCOL_NDEF_ENTRY_HEADER_ACTION_POS  0
#define NPP_PROTOCOL_NDEF_ENTRY_HEADER_NDEF_LENGTH_POS 1

#define NPP_PROTOCOL_FIRST_NDEF_ENTRY_POS    NPP_PROTOCOL_HEADER_LENGTH


#define NPP_MIN_NDEF_ENTRIES_SUPPORTED    1
#define NPP_MAX_NDEF_ENTRIES_SUPPORTED    1

#define NPP_DEFAULT_SERVER_MAX_INFO_BYTES 0xFFFF

/* See header */
void PNPPServerDriverInstanceCreate(
   tNPPServerDriverInstance * pNPPServer)
{
   CMemoryFill(pNPPServer, 0, sizeof(tNPPServerDriverInstance));
}


/* See header */
void PNPPServerDriverInstanceDestroy(
   tNPPServerDriverInstance * pNPPServer)
{
   CMemoryFill(pNPPServer, 0, sizeof(tNPPServerDriverInstance));
}

/** See header */
W_ERROR PNDEFRegisterNPPMessageHandlerDriver  (
      tContext * pContext,
      tPBasicGenericDataCallbackFunction* pHandler,
      void *  pHandlerParameter,
      uint8_t nPriority,
      W_HANDLE *  phRegistry )
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);
   tPNPPMessageHandler * pMessageHandler = 0;
   tPNPPMessageHandler * pLastNPPMessageHandler, * pCurrentNPPMessageHandler;
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
      PDebugError("PNDEFRegisterNPPMessageHandlerDriver : phRegistry == null");
      nError = W_ERROR_BAD_PARAMETER;
      goto end;
   }

   /* Allocate a data structure that will contain all stuff specific to this handler registration
      the common stuff (socket, P2P link are stored in the context itself...  */

   pMessageHandler = (tPNPPMessageHandler *) CMemoryAlloc(sizeof(tPNPPMessageHandler));
   if (pMessageHandler == null)
   {
      /* either if use of goto usage is quite controversial, we use them for error management in the Open NFC stack */
      PDebugError("PNDEFRegisterNPPMessageHandlerDriver : pMessageHandler == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto end;
   }
   pMessageHandler->pNext = null;

   pMessageHandler->pDriverCC = pDriverCC;
   pMessageHandler->nPriority = nPriority;

   /* Creates a new handle associated to the pMessageHandler. This handle is returned in the phRegistry parameter.
      This handle is used for unregistration of the listener */

   nError = PHandleRegister(pContext, pMessageHandler, P_HANDLE_TYPE_NPP_SERVER, phRegistry);
   if (nError != W_SUCCESS)
   {
      PDebugError("PNDEFRegisterNPPMessageHandlerDriver : PHandleRegister failed %d", nError);
      goto end;
   }

   /* create only one socket for all registred handlers */
   if(pNPPServer->hServerSocket == W_NULL_HANDLE)
   {
      tUserInstance * pCurrentUserInstance;
      tWP2PConfiguration sConfiguration;
      /* We must keep the number of registrations :
         each time a new registration is done, the usage count is incremented.
         the socket must be closed and the link establishment must be destroyed  when the usage count drops to zero */

      CDebugAssert(pNPPServer->nServerSocketUsageCount == 0);
      CDebugAssert(pNPPServer->hServerSocket == W_NULL_HANDLE);
      CDebugAssert(pNPPServer->hLinkOperation == W_NULL_HANDLE);

      pNPPServer->nServerSocketUsageCount = 1;
      pNPPServer->bShutdownRequested = W_FALSE;
      pNPPServer->bConnectionPending = W_TRUE;

      /* Handles are by default associated to the current user instance, meaning they are automatically closed
       * when the user instance is destroyed. Here, we only create one socket and make only one link establishment
       * requets for several clients, so we need to drop temporary the current instance for these operations.
       *
       * NOTE: The user instance must be restored at the end
       */

      pCurrentUserInstance = PContextGetCurrentUserInstance(pContext);

      PContextSetCurrentUserInstance(pContext, null);
      nError = PP2PCreateSocketDriver(pContext, W_P2P_TYPE_SERVER, g_NPPUri, sizeof(g_NPPUri), P_NPP_SERVER_SAP, &pNPPServer->hServerSocket);

      if (nError != W_SUCCESS)
      {
         PDebugError("PNPPRegisterNPPMessageHandlerDriver : PP2PCreateSocketDriver failed %d", nError);

         /* Restore the current user instance */
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto end;
      }

      nError = PP2PGetConfigurationDriver(pContext, &sConfiguration, sizeof(sConfiguration));
      if (nError != W_SUCCESS)
      {
         PDebugError("PNPPRegisterNPPMessageHandlerDriver : PP2PGetConfigurationDriver failed %d", nError);
         goto end;
      }

      nError = PP2PSetSocketParameter(pContext, pNPPServer->hServerSocket, W_P2P_LOCAL_MIU, sConfiguration.nLocalMIU);
      if (nError != W_SUCCESS)
      {
         PDebugError("PNPPRegisterNPPMessageHandlerDriver : PP2PSetSocketParameter failed %d", nError);
         goto end;
      }

      /* Request link establishment */

      /* Since we are in the driver part of the stack, we must call internal function instead of PP2PEstablishLinkDriverInternal()
         which can only be called from user part
      */
      hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PNPPServerLinkEstablishmentCallback, null);

      if (hLink == W_NULL_HANDLE)
      {
         PDebugError("PNDEFRegisterNPPMessageHandlerDriver : PP2PEstablishLinkDriver1Internal failed");

         /* Restore the current user instance */
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto end;
      }

      PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PNPPServerLinkReleaseCallback, null, &pNPPServer->hLinkOperation);

      /* Restore the current user instance */
      PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
   }
   else
   {
      pNPPServer->nServerSocketUsageCount ++;
   }

   /* enqueue the message handler in the list of the already registered messages handler,
      accordingly to their priority */

   pLastNPPMessageHandler = null;
   pCurrentNPPMessageHandler = pNPPServer->pFirstMessageHandler;

   while ((pCurrentNPPMessageHandler != null) && (pCurrentNPPMessageHandler->nPriority >= nPriority))
   {
      pLastNPPMessageHandler = pCurrentNPPMessageHandler;
      pCurrentNPPMessageHandler = pCurrentNPPMessageHandler->pNext;
   }

   if (pLastNPPMessageHandler == null)
   {
      pMessageHandler->pNext = pNPPServer->pFirstMessageHandler;
      pNPPServer->pFirstMessageHandler = pMessageHandler;
   }
   else
   {
      pMessageHandler->pNext = pLastNPPMessageHandler->pNext;
      pLastNPPMessageHandler->pNext = pMessageHandler;
   }

   pCurrentNPPMessageHandler = pNPPServer->pFirstMessageHandler;


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

/**
 * This function is called when the P2P link is established.
 *
 * On success, we must start accepting client connection on the server socket.
 * On failure, depending on the existence of at least one registered handler, we must request link establishment once again
 */

static void static_PNPPServerLinkEstablishmentCallback( tContext* pContext, void * pCallbackParameter, W_HANDLE hLink, W_ERROR nResult )
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);  /* can not be null */

   PBasicCloseHandle(pContext, pNPPServer->hLinkOperation);
   pNPPServer->hLinkOperation = W_NULL_HANDLE;

   if (nResult == W_SUCCESS)
   {
      /* Cache the hink handle */
      pNPPServer->hLink = hLink;

      PDebugTrace("static_PNPPServerLinkEstablishmentCallback : request NPP server connection");

      /* The link has been activated, we must start accepting connections on the server socket */
      PP2PConnectDriverInternal(pContext, pNPPServer->hServerSocket, hLink, static_PNPPServerSocketConnected, null);
   }
   else
   {
      PDebugError("static_PNPPServerLinkEstablishmentCallback : P2P link establishment failed %d", nResult);

      if (pNPPServer->nServerSocketUsageCount > 0)
      {
         W_HANDLE hTempLink;

         PDebugTrace("static_PNPPServerLinkEstablishmentCallback : request new link establishment");

         hTempLink = PP2PEstablishLinkDriver1Internal(pContext, static_PNPPServerLinkEstablishmentCallback, null);

         if (hTempLink != W_NULL_HANDLE)
         {
            PP2PEstablishLinkDriver2Internal(pContext, hTempLink, static_PNPPServerLinkReleaseCallback, null, &pNPPServer->hLinkOperation);
         }
         else
         {
            PDebugError("static_PNPPServerLinkEstablishmentCallback : unable to request new link establishemnt : TROUBLE AHEAD");
         }
      }
      else
      {
         PDebugTrace("static_PNPPServerLinkEstablishmentCallback : no more listener");
      }
   }
}

/*
 * This function is called when a P2P NPP client connected to the server
 *
 * On success, typically starts receiving data.
 */

static void static_PNPPServerSocketConnected( tContext* pContext, void * pCallbackParameter,  W_ERROR nResult )
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);

   if (nResult == W_SUCCESS)
   {
      /* The socket is now connected : the NPP protocol starts here */
      pNPPServer->bConnectionPending = W_FALSE;

      /* Set the NPP reception State to Awaiting the NPP Header */
      pNPPServer->nNPPReceptionState = AWAITING_NPP_HEADER;

      /* Start receiving DATA on the socket */
      PP2PReadDriverInternal(pContext, pNPPServer->hServerSocket, static_NPPServerDataReceived, null, pNPPServer->aReceiveBuffer, sizeof(pNPPServer->aReceiveBuffer), &pNPPServer->hReceiveOperation);
   }
   else
   {
      PDebugError("static_PNPPServerSocketConnected: nResult = 0x%08X", nResult);

      /* possible reasons to reach this point are :
       * - P2P link failure     : all stuff is managed in the link release callback
       * - P2P sockert connection aborted  : we are stopping the NPP server
       */
   }
}

/*
 * This function is called when the P2P link is broken
 *
 * Depending on the existence of at least one registered handler, we must request link establishment once again
 * or perform final cleanup (if any)
 */

static void static_PNPPServerLinkReleaseCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult)
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);  /* can not be null */

   PDebugTrace("static_PNPPServerLinkReleaseCallback %d\n", nResult);

   if(pNPPServer->bConnectionPending != W_FALSE)
   {
      PDebugTrace("static_PSNEPServerLinkReleaseCallback : Connection pending");
      return;
   }

   if (pNPPServer->hLink != W_NULL_HANDLE)
   {
      PBasicCloseHandle(pContext, pNPPServer->hLink);
      pNPPServer->hLink = W_NULL_HANDLE;
   }

   if(pNPPServer->nServerSocketUsageCount > 0)
   {
      /* The Last Message Handler Has been called a performed before */
      if( pNPPServer->pLastMessageHandlerCalled == null )
      {
         W_HANDLE hLink;

         pNPPServer->bConnectionPending = W_TRUE;
         PDebugTrace("static_PNPPServerLinkReleaseCallback : request new link establishment");

         hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PNPPServerLinkEstablishmentCallback, null);

         if (hLink != W_NULL_HANDLE)
         {
            PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PNPPServerLinkReleaseCallback, null, &pNPPServer->hLinkOperation);
         }
         else
         {
            PDebugError("static_PNPPServerLinkReleaseCallback : unable to request new link establishemnt : TROUBLE AHEAD");
         }
      }
      else
      {
         /* Link release while Clients have received callback*/
         PDebugTrace("static_PNPPServerLinkReleaseCallback : Client need to do a workperformed");
      }
   }
   else
   {
      /* Perform final cleanup */
      PDebugTrace("static_PNPPServerLinkReleaseCallback : no more listener");
   }
}


static void static_NPPServerDataReceived( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nResult )
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);
   bool_t bPostCallback = W_FALSE;
   bool_t bReadMoreData = W_FALSE;
   uint32_t nNDEFLength = 0;
   uint32_t nNDEFEntries = 0;

   if ( (W_SUCCESS != nResult) || ( pNPPServer->bShutdownRequested == W_TRUE))
   {
      PDebugError("static_NPPServerDataReceived: nResult = 0x%08X", nResult);

      /* possible reasons to reach this point are :
       * - P2P link failure     : all stuff is managed in the link release callback
       * - P2P socket connection has been closed
       * - The shutdown has been requested
       */
      goto error;
   }

   /* Process the received data according to the NPP protocol... */
   switch(pNPPServer->nNPPReceptionState)
   {
      case AWAITING_NPP_HEADER:

         PDebugTrace("static_NPPServerDataReceived: AWAITING_NPP_HEADER");

         /* The NPP data received must be at least the size of the NPP protocol header + the size of one for this to be valid */
         if (nDataLength < (NPP_PROTOCOL_HEADER_LENGTH + NPP_PROTOCOL_NDEF_ENTRY_HEADER_LENGTH))
         {
            nResult = W_ERROR_BAD_TAG_FORMAT;
            PDebugError("static_NPPServerDataReceived: AWAITING_NPP_HEADER : W_ERROR_BAD_TAG_FORMAT" );
            goto error;
         }

         /* The received data is a valid length, check the header */
         if ((pNPPServer->aReceiveBuffer[NPP_PROTOCOL_HEADER_VERSION_POS] & NPP_PROTOCOL_MAJOR_VERSION_MASK) != NPP_PROTOCOL_MAJOR_VERSION)
         {
            /* The major version differs between the server and the client. Section 2.3 of the specification states that server may
               return unsupported version response*/

            nResult = W_ERROR_VERSION_NOT_SUPPORTED;
            PDebugError("static_NPPServerDataReceived: AWAITING_NPP_HEADER : unsupported version");
            goto error;
         }

         /* check the number of NDEF Entries */
         nNDEFEntries = PUtilReadUint32FromBigEndianBuffer(&pNPPServer->aReceiveBuffer[NPP_PROTOCOL_HEADER_NDEF_ENTRIES_POS]);

         if(nNDEFEntries < NPP_MIN_NDEF_ENTRIES_SUPPORTED)
         {
            nResult = W_ERROR_ITEM_NOT_FOUND;
            goto error;
         }

         if(nNDEFEntries > NPP_MAX_NDEF_ENTRIES_SUPPORTED)
         {
            PDebugError("static_NPPServerDataReceived: AWAITING_NPP_HEADER : %d NDEF Entries received, Only the first will be returned", nNDEFEntries);
         }


         /* Read the first NDEF Entry */
         /* Check action */
         if(pNPPServer->aReceiveBuffer[NPP_PROTOCOL_FIRST_NDEF_ENTRY_POS + NPP_PROTOCOL_NDEF_ENTRY_HEADER_ACTION_POS] != NPP_ACTION_PUSH)
         {
            nResult = W_ERROR_FEATURE_NOT_SUPPORTED;
            PDebugError("static_NPPServerDataReceived: AWAITING_NPP_HEADER : Action invalid");
            goto error;
         }


         /* Keep length */
         nNDEFLength = PUtilReadUint32FromBigEndianBuffer(&pNPPServer->aReceiveBuffer[ NPP_PROTOCOL_FIRST_NDEF_ENTRY_POS
                                                                                       + NPP_PROTOCOL_NDEF_ENTRY_HEADER_NDEF_LENGTH_POS]);

         /* Check that the length is not greater than that we support*/
         CDebugAssert(pNPPServer->sReceivedNDEFMessage.pReceiveBuffer == null);

         if ((nNDEFLength <= NPP_DEFAULT_SERVER_MAX_INFO_BYTES) &&
               (pNPPServer->sReceivedNDEFMessage.pReceiveBuffer = (uint8_t* ) CMemoryAlloc(nNDEFLength)) != null)
         {
            uint32_t nNDEFMessageReceivedLength = nDataLength - ( NPP_PROTOCOL_HEADER_LENGTH + NPP_PROTOCOL_NDEF_ENTRY_HEADER_LENGTH);
            pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength = nNDEFLength;

            if (nNDEFMessageReceivedLength != 0)
            {
               /* Copy the data to the received buffer that was allocated */
               CMemoryCopy(pNPPServer->sReceivedNDEFMessage.pReceiveBuffer,
                           &(pNPPServer->aReceiveBuffer[NPP_PROTOCOL_HEADER_LENGTH + NPP_PROTOCOL_NDEF_ENTRY_HEADER_LENGTH]),
                           nNDEFMessageReceivedLength);

               /* Update the buffer position index */
               pNPPServer->sReceivedNDEFMessage.nNDEFBufferIndex = nNDEFMessageReceivedLength;

               if (nNDEFMessageReceivedLength == nNDEFLength)
               {
                  /* All of the NDEF data was contained within this packet so set the Success response */
                  pNPPServer->nNPPReceptionState = AWAITING_NPP_HEADER;

                  bPostCallback = W_TRUE;
               }
               else
               {
                  /* Further fragments of data are expected so set the Continue response */
                  pNPPServer->nNPPReceptionState = AWAITING_NPP_NDEF_MESSAGE;
                  bReadMoreData = W_TRUE;
               }
            }
         }
         else
         {
            nResult = W_ERROR_BUFFER_TOO_LARGE;
            PDebugError("static_NPPServerDataReceived: AWAITING_NPP_HEADER : data too large %d", nResult);
            goto error;
            /* There is more data than we support */
         }

         break;

      case AWAITING_NPP_NDEF_MESSAGE:
         PDebugTrace("static_NPPServerDataReceived: AWAITING_NPP_NDEF_MESSAGE");


         /* If the amount of data received can be placed within the NDEF buffer do so */
         if ((pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength - pNPPServer->sReceivedNDEFMessage.nNDEFBufferIndex) <= nDataLength)
         {
            nNDEFLength = (pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength - pNPPServer->sReceivedNDEFMessage.nNDEFBufferIndex);
         }else
         {
            nNDEFLength = nDataLength;
         }


         /* Copy the data to the received NDEF buffer */
         CMemoryCopy(pNPPServer->sReceivedNDEFMessage.pReceiveBuffer + pNPPServer->sReceivedNDEFMessage.nNDEFBufferIndex,
                     pNPPServer->aReceiveBuffer,
                     nDataLength);

         /* Update the buffer position index */
         pNPPServer->sReceivedNDEFMessage.nNDEFBufferIndex += nDataLength;

         if (pNPPServer->sReceivedNDEFMessage.nNDEFBufferIndex == pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength)
         {
            PDebugTrace("static_NPPServerDataReceived: AWAITING_NPP_NDEF_MESSAGE : message completed");

            /* The complete NDEF message has been received */
            pNPPServer->nNPPReceptionState = AWAITING_NPP_HEADER;
            bPostCallback = W_TRUE;
         }
         else
         {
            bReadMoreData = W_TRUE;
         }
         break;

      default:
         /* Should not get here as the value of pNPPServer->nNPPReceptionState should always be AWAITING_NPP_HEADER or AWAITING_NPP_FRAGMENT */
         CDebugAssert(0);
         break;
   }

   /* Setup another read if we are expecting further data */
   if (bReadMoreData == W_TRUE)
   {
      PP2PReadDriverInternal(pContext, pNPPServer->hServerSocket, static_NPPServerDataReceived, null, pNPPServer->aReceiveBuffer, sizeof(pNPPServer->aReceiveBuffer), &pNPPServer->hReceiveOperation);
   }

   if (bPostCallback == W_TRUE)
   {
      /* When a NDEF message has been received, call all the registered callbacks
         The message will be freed only once all handlers have successfully retreived the message content
         Once the message is freed, restart reading data from the socket connection */

      tPNPPMessageHandler * pCurrentNPPMessageHandler;

      pCurrentNPPMessageHandler = pNPPServer->pFirstMessageHandler;

      while (pCurrentNPPMessageHandler != null)
      {
         pCurrentNPPMessageHandler->bIsCalled = W_FALSE;
         pCurrentNPPMessageHandler = pCurrentNPPMessageHandler->pNext;
      }

      pCurrentNPPMessageHandler = pNPPServer->pFirstMessageHandler;

      /* Save the last object called */
      pNPPServer->pLastMessageHandlerCalled = pCurrentNPPMessageHandler;

      if (pCurrentNPPMessageHandler != null)
      {
         pCurrentNPPMessageHandler->bIsCalled = W_TRUE;
         PDFCDriverPostCC3(pCurrentNPPMessageHandler->pDriverCC, pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength, W_SUCCESS);
      }
      else
      {
         CMemoryFree(pNPPServer->sReceivedNDEFMessage.pReceiveBuffer);
         pNPPServer->sReceivedNDEFMessage.pReceiveBuffer       = null;
         pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength   = 0;
         pNPPServer->sReceivedNDEFMessage.nNDEFBufferIndex     = 0;
      }
   }

   return;
error:
   if (pNPPServer->sReceivedNDEFMessage.pReceiveBuffer != null)
   {
      /* Free the allocated message (if any) */
      CMemoryFree(pNPPServer->sReceivedNDEFMessage.pReceiveBuffer);
      pNPPServer->sReceivedNDEFMessage.pReceiveBuffer = null;

      pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength = 0;
   }
}

static void static_PNDEFMessageHandlerDestroyAutomaton(tContext* pContext, tPNPPMessageHandler * pMessageHandler)
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);

   /* Request shutdown of the socket connection */
   if ((pNPPServer->hServerSocket != W_NULL_HANDLE) && (pNPPServer->bShutdownRequested == W_FALSE))
   {
      PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : terminating server socket connection");
      PP2PShutdownDriverInternal(pContext, pNPPServer->hServerSocket, static_PNPPServerShutdownConnectionCallback, pMessageHandler);
      pNPPServer->bShutdownRequested = W_TRUE;

      return;
   }
   /* close the server socket */
   if (pNPPServer->hServerSocket != W_NULL_HANDLE)
   {

      PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : closing server socket");
      PBasicCloseHandle(pContext, pNPPServer->hServerSocket);
      pNPPServer->hServerSocket = W_NULL_HANDLE;
   }

   if (pNPPServer->hLink != W_NULL_HANDLE)
   {
      /* request the termination of the P2P RF link if established */
      PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : closing link");
      PBasicCloseHandle(pContext, pNPPServer->hLink);
      pNPPServer->hLink = W_NULL_HANDLE;
   }
   else
   {
      /* Cancel link establishment if the RF link is not established */
      PDebugTrace("static_PNDEFMessageHandlerDestroyAutomaton : aborting link establishment");
      PBasicCancelOperation(pContext, pNPPServer->hLinkOperation);
      PBasicCloseHandle(pContext, pNPPServer->hLinkOperation);

      pNPPServer->hLinkOperation = W_NULL_HANDLE;
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
 * This function will decrease the usage count of the internal NPP server
 * If the usage count reaches 0, the socket is destroyed and the P2P link is closed
 */

static uint32_t static_PNPPMessageHandlerDestroyAsync(tContext* pContext, tPBasicGenericCallbackFunction* pCallback, void* pCallbackParameter, void* pObject )
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);
   tPNPPMessageHandler * pMessageHandler = (tPNPPMessageHandler *) pObject;

   tPNPPMessageHandler * pLastNPPMessageHandler;
   tPNPPMessageHandler * pCurrentNPPMessageHandler = 0;

   /* Save the current User Instance to restore it at the end of the process */
   tUserInstance * pUserInstance = PContextGetCurrentUserInstance(pContext);
   PContextSetCurrentUserInstance(pContext, null);

   PDebugTrace("static_PNPPMessageHandlerDestroyAsync");

   if(pMessageHandler == pNPPServer->pLastMessageHandlerCalled)
   {
      PDebugTrace("the destroyed Message is the last Message Called : WorkPerformed must be done");
      PNDEFSetWorkPerformedNPPDriver(pContext, W_TRUE);
   }

   /* Remove the Handler from the linked list within the NPP Server */
   pLastNPPMessageHandler = null;
   pCurrentNPPMessageHandler = pNPPServer->pFirstMessageHandler;

   while ((pCurrentNPPMessageHandler != null) && (pCurrentNPPMessageHandler != pMessageHandler))
   {
      pLastNPPMessageHandler = pCurrentNPPMessageHandler;
      pCurrentNPPMessageHandler  = pCurrentNPPMessageHandler->pNext;
   }

   if (pLastNPPMessageHandler == null)
   {
      pNPPServer->pFirstMessageHandler = pMessageHandler->pNext;
   }
   else
   {
      pLastNPPMessageHandler->pNext = pMessageHandler->pNext;
   }

   /*
    * Decrement the reference count
    * If it reaches 0, the server socket must be closed
    * else, simply free the resources of the
    */
   if (-- pNPPServer->nServerSocketUsageCount == 0)
   {
      PDebugTrace("static_PNPPMessageHandlerDestroyAsync : no more registered handler, terminate link establishment");

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

static void static_PNPPServerShutdownConnectionCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult )
{
   PDebugTrace("static_PNPPServerShutdownConnectionCallback %d", nResult);

   static_PNDEFMessageHandlerDestroyAutomaton(pContext, (tPNPPMessageHandler *) pCallbackParameter);
}


/** See header */
W_ERROR PNDEFRetrieveNPPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength)
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);

   PDebugTrace("PNDEFRetrieveNPPMessageDriver");

   if (pBuffer != null)
   {
      if (nBufferLength >= pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength)
      {
         /* copy the message content in the buffer */
         CMemoryCopy(pBuffer, pNPPServer->sReceivedNDEFMessage.pReceiveBuffer, pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength);
      }
      else
      {
         PDebugError("PNDEFRetrieveNPPMessageDriver : buffer too short");

         /* The supplied buffer is not large enough to handle the received NDEF Message */
         return W_ERROR_BUFFER_TOO_SHORT;
      }
   }

   return W_SUCCESS;
}

/** See header */
void PNDEFSetWorkPerformedNPPDriver(
      tContext * pContext,
      bool_t     bGiveToNextListener)
{
   tNPPServerDriverInstance * pNPPServer = PContextGetNPPServerDriverInstance(pContext);
   tPNPPMessageHandler * pCurrentNPPMessageHandler = null;

   if (bGiveToNextListener != W_FALSE)
   {
      pCurrentNPPMessageHandler = pNPPServer->pFirstMessageHandler;

      while ((pCurrentNPPMessageHandler != null) && (pCurrentNPPMessageHandler->bIsCalled != W_FALSE))
      {
         pCurrentNPPMessageHandler = pCurrentNPPMessageHandler->pNext;
      }
   }

   pNPPServer->pLastMessageHandlerCalled = pCurrentNPPMessageHandler;

   if (pCurrentNPPMessageHandler != null)
   {
      PDebugTrace("PNDEFSetWorkPerformedNPPDriver : call next handler");

      pCurrentNPPMessageHandler->bIsCalled = W_TRUE;
      PDFCDriverPostCC3(pCurrentNPPMessageHandler->pDriverCC, pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength, W_SUCCESS);
   }
   else
   {
      tUserInstance * pCurrentUserInstance;
      PDebugTrace("PNDEFSetWorkPerformedNPPDriver : no more handler to be called");

      pCurrentUserInstance = PContextGetCurrentUserInstance(pContext);
      PContextSetCurrentUserInstance(pContext, null);

      /* Free off the memory */
      CMemoryFree(pNPPServer->sReceivedNDEFMessage.pReceiveBuffer);
      pNPPServer->sReceivedNDEFMessage.pReceiveBuffer = null;
      pNPPServer->sReceivedNDEFMessage.nNDEFMessageLength = 0;
      pNPPServer->sReceivedNDEFMessage.nNDEFBufferIndex = 0;

      PDebugTrace("PNDEFSetWorkPerformedNPPDriver : continue processing new messages");

      /* If the link has been released we establish it again */
      if(pNPPServer->hLink == W_NULL_HANDLE)
      {
         W_HANDLE hLink;

         PDebugTrace("PNDEFSetWorkPerformedNPPDriver : request new link establishment");

         hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PNPPServerLinkEstablishmentCallback, null);

         if (hLink != W_NULL_HANDLE)
         {
            PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PNPPServerLinkReleaseCallback, null, &pNPPServer->hLinkOperation);
         }
         else
         {
            PDebugError("PNDEFSetWorkPerformedNPPDriver : unable to request new link establishemnt : TROUBLE AHEAD");
         }
      }
      else
      {
         /* The link and the socket are already connected , do a simple read*/
         PP2PReadDriverInternal(pContext, pNPPServer->hServerSocket, static_NPPServerDataReceived, null, pNPPServer->aReceiveBuffer, sizeof(pNPPServer->aReceiveBuffer), &pNPPServer->hReceiveOperation);
      }
      PContextSetCurrentUserInstance(pContext, pCurrentUserInstance);
   }
}


/*******************************************************************************
   Below the implementation of the NPP client
*******************************************************************************/


static void static_PNDEFSendNPPMessageDriverCancel(tContext* pContext, void* pCancelParameter, bool_t bIsClosing);

static void static_PNPPClientLinkEstablishmentCallback( tContext* pContext, void * pCallbackParameter, W_HANDLE hLink, W_ERROR nResult );
static void static_PNPPClientLinkReleaseCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult);
static void static_PNPPClientSocketConnected( tContext* pContext, void * pCallbackParameter,  W_ERROR nResult );

static void static_NPPClientDataTransmitted( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );
static void static_PNPPClientShutdownConnectionCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );
static void static_NPPClientAutomaton( tContext* pContext);
static void static_NPPClientPostResult(tContext* pContext, tPNPPMessage * pMessage, W_ERROR nError);
static void static_PNPPClientGeneralFailure(tContext * pContext,  void * pCallbackParameter, W_ERROR nResult);
static void static_PNDEFMessageDestroyAutomaton(tContext* pContext, tPNPPMessage * pMessage);


W_HANDLE PNDEFSendNPPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);

   tPNPPMessage * pMessage;
   tPNPPMessage * pCurrentNPPMessage = pNPPClient->pFirstMessage;
   W_HANDLE        hOperation = W_NULL_HANDLE;
   tDFCDriverCCReference pDriverCC;
   W_ERROR       nError;
   W_HANDLE      hLink;

   PDFCDriverFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pDriverCC);

   /* Allocate a data structure that will contain all stuff specific to this message
      the common stuff (socket, P2P link are stored in the context itself...  */

   pMessage = (tPNPPMessage *) CMemoryAlloc(sizeof(tPNPPMessage));

   if (pMessage == null)
   {
      /* either if use of goto usage is quite controversial, we use them for error management in the Open NFC stack */
      PDebugError("PNDEFSendNPPMessageDriver : pMessage == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   CMemoryFill(pMessage, 0, sizeof(tPNPPMessage));

   pMessage->nLength = nBufferLength;

   if (nBufferLength != 0)
   {
      pMessage->pBuffer = (uint8_t *) CMemoryAlloc(nBufferLength);

      if (pMessage->pBuffer == null)
      {
        /* either if use of goto usage is quite controversial, we use them for error management in the Open NFC stack */
        PDebugError("PNDEFSendNPPMessageDriver : pMessage->pBuffer == null");
        nError = W_ERROR_OUT_OF_RESOURCE;
        goto error;
      }

      CMemoryCopy(pMessage->pBuffer, pBuffer, nBufferLength);
   }

   pMessage->pDriverCC = pDriverCC;

   /* Creates a new handle associated to the send message operation. This handle is returned in the phRegistry parameter.
      This handle is used for unregistration of the listener */

   hOperation = PBasicCreateOperation(pContext, static_PNDEFSendNPPMessageDriverCancel, pMessage);

   if (hOperation == W_NULL_HANDLE)
   {
      PDebugError("PNDEFSendNPPMessageDriver : PBasicCreateOperation failed");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   pMessage->hOperation = hOperation;

   /* create only one socket for all registered handlers */

   if ( (pNPPClient->hClientSocket == W_NULL_HANDLE) && (pNPPClient->hLink == W_NULL_HANDLE))
   {
      tUserInstance * pCurrentUserInstance;
      tWP2PConfiguration sConfiguration;

      /* We must keep the number of registrations :
         each time a new registration is done, the usage count is incremented.
         the socket must be closed and the link establishment must be destroyed  when the usage count drops to zero */

      CDebugAssert(pNPPClient->hClientSocket == W_NULL_HANDLE);
      CDebugAssert(pNPPClient->hLinkOperation == W_NULL_HANDLE);
      CDebugAssert(pNPPClient->hLink == W_NULL_HANDLE);
      CDebugAssert(pNPPClient->hTransmitOperation== W_NULL_HANDLE);
      CDebugAssert(pNPPClient->pFirstMessage == null);


      PDebugTrace("PNDEFSendNPPMessageDriver : starting new session");

      pNPPClient->bShutdownRequested = W_FALSE;
      pNPPClient->bClientConnected = W_FALSE;

      /* Handles are by default associated to the current user instance, meaning they are automatically closed
       * when the user instance is destroyed. Here, we only create one socket and make only one link establishment
       * requets for several clients, so we need to drop temporary the current instance for these operations.
       *
       * NOTE: The user instance must be restored at the end
       */

      pCurrentUserInstance = PContextGetCurrentUserInstance(pContext);

      PContextSetCurrentUserInstance(pContext, null);
      nError = PP2PCreateSocketDriver(pContext, W_P2P_TYPE_CLIENT, g_NPPUri, sizeof(g_NPPUri), 0, &pNPPClient->hClientSocket);

      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFSendNPPMessageDriver : PP2PCreateSocketDriver failed %d", nError);

         /* Restore the current user instance */
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto error;
      }

      nError = PP2PGetConfigurationDriver(pContext, &sConfiguration, sizeof(sConfiguration));
      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFSendNPPMessageDriver : PP2PGetConfigurationDriver failed %d", nError);
         goto error;
      }

      nError = PP2PSetSocketParameter(pContext, pNPPClient->hClientSocket, W_P2P_LOCAL_MIU, sConfiguration.nLocalMIU);
      if (nError != W_SUCCESS)
      {
         PDebugError("PNDEFSendNPPMessageDriver : PP2PSetSocketParameter failed %d", nError);
         goto error;
      }

      /* Request link establishment */

      /* Since we are in the driver part of the stack, we must call internal function instead of PP2PEstablishLinkDriverInternal()
         which can only be called from user part
      */
      hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PNPPClientLinkEstablishmentCallback, null);

      if (hLink == W_NULL_HANDLE)
      {
         PDebugError("PNDEFSendNPPMessageDriver : PP2PEstablishLinkDriver1Internal failed");

         /* Restore the current user instance */
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto error;
      }

      PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PNPPClientLinkReleaseCallback, null, &pNPPClient->hLinkOperation);

      /* Restore the current user instance */
      PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
   }

   pMessage->nError = W_SUCCESS;


   /* enqueue the newly created pMessageHandler in the context list */
   if (pNPPClient->pFirstMessage == null)
   {

     PDebugTrace("PNDEFSendNPPMessageDriver : pending message list is empty");

      pNPPClient->pFirstMessage = pMessage;

      if (pNPPClient->bClientConnected != W_FALSE)
      {
         PDebugTrace("PNDEFSendNPPMessageDriver : start message transmission");

         /* the NPP protocol starts here */
         static_NPPClientAutomaton(pContext);
      }
   }
   else
   {

     PDebugTrace("PNDEFSendNPPMessageDriver : enqueue the message");

      pCurrentNPPMessage = pNPPClient->pFirstMessage;

      while (pCurrentNPPMessage->pNext != null)
      {
         pCurrentNPPMessage = pCurrentNPPMessage->pNext;
      }

      pCurrentNPPMessage->pNext = pMessage;
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
      static_NPPClientPostResult(pContext, pMessage, nError);
   }

   return W_NULL_HANDLE;
}


/**
 * This function is called when the P2P link is established.
 *
 * On success, we must try to establish a socket connection with the peer device
 * On failure, depending on the existence of at least one registered handler, we must request link establishment once again
 */

static void static_PNPPClientLinkEstablishmentCallback( tContext* pContext, void * pCallbackParameter, W_HANDLE hLink, W_ERROR nResult )
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);  /* can not be null */

   PBasicCloseHandle(pContext, pNPPClient->hLinkOperation);
   pNPPClient->hLinkOperation = W_NULL_HANDLE;

   if (nResult == W_SUCCESS)
   {
      /* Cache the hink handle */
      pNPPClient->hLink = hLink;

      PDebugTrace("static_PNPPClientLinkEstablishmentCallback : request NPP client connection");

      /* The link has been activated, we must start accepting connections on the server socket */
      PP2PConnectDriverInternal(pContext, pNPPClient->hClientSocket, hLink, static_PNPPClientSocketConnected, null);
   }
   else
   {
      PDebugError("static_PNPPClientLinkEstablishmentCallback : P2P link establishment failed %d", nResult);

      static_PNPPClientGeneralFailure(pContext, null, nResult);
   }
}

/*
 * This function is called when the P2P socket is connected
 *
 * Depending on the existence of at least one registered handler, we must request link establishment once again
 * or perform final cleanup (if any)
 */

static void static_PNPPClientSocketConnected( tContext* pContext, void * pCallbackParameter,  W_ERROR nResult )
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);
   W_ERROR nError;

   PDebugTrace("static_PNPPClientSocketConnected %d", nResult);

   if (nResult == W_SUCCESS)
   {
      /* The socket is now connected : */

      /* retreive the maximum packet size that we can send to the peer */
      nError = PP2PGetSocketParameterDriver(pContext, pNPPClient->hClientSocket, W_P2P_REMOTE_MIU, &pNPPClient->nSocketMIU);

      PDebugError("pNPPClient->nSocketMIU %d", pNPPClient->nSocketMIU);
      if (nError != W_SUCCESS)
      {
         PDebugError("static_PNPPClientSocketConnected: PP2PGetSocketParameterDriver failed %d", nError);
         pNPPClient->nSocketMIU = 128;
      }

      pNPPClient->bClientConnected = W_TRUE;

      /* the NPP protocol starts here */
      pNPPClient->nTransmissionState = SENDING_NPP_HEADER;
      static_NPPClientAutomaton(pContext);
   }
   else
   {
      PDebugError("static_PNPPClientSocketConnected: nResult = 0x%08X", nResult);

      static_PNPPClientGeneralFailure(pContext, null, nResult);
   }
}

/*
 * This function implements the NPP client PUSH automaton
 *
 */
static void static_NPPClientAutomaton( tContext* pContext)
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);
   tUserInstance * pUserInstance;
   bool_t bSendPacket = W_FALSE;
   uint32_t nLength = 0;

   CDebugAssert(pNPPClient->pFirstMessage != null);

   /* Process the received data according to the NPP protocol... */
   switch (pNPPClient->nTransmissionState)
   {
      case SENDING_NPP_HEADER:

         PDebugTrace("static_NPPClientAutomaton: SENDING_NPP_HEADER");

         if (pNPPClient->pFirstMessage->nLength != 0)
         {
            /**** NPP Header ****/
            /* Version */
            pNPPClient->aTransmitBuffer[NPP_PROTOCOL_HEADER_VERSION_POS] = (NPP_PROTOCOL_MAJOR_VERSION & NPP_PROTOCOL_MAJOR_VERSION_MASK)
                                                                         | (NPP_PROTOCOL_MINOR_VERSION & NPP_PROTOCOL_MINOR_VERSION_MASK);

            /* Number of NDEF entries : only one ndef entry is upported */
            PUtilWriteUint32ToBigEndianBuffer(NPP_NUMBER_OF_NDEF_ENTRIES , &(pNPPClient->aTransmitBuffer[NPP_PROTOCOL_HEADER_NDEF_ENTRIES_POS]));


            /**** Current NDEF Entry ****/
            /* Action Code : Push */
            pNPPClient->aTransmitBuffer[ NPP_PROTOCOL_FIRST_NDEF_ENTRY_POS
                                       + NPP_PROTOCOL_NDEF_ENTRY_HEADER_ACTION_POS] = NPP_ACTION_PUSH;

            /* Copy the NDEF Length */
            PUtilWriteUint32ToBigEndianBuffer(pNPPClient->pFirstMessage->nLength , &(pNPPClient->aTransmitBuffer[ NPP_PROTOCOL_FIRST_NDEF_ENTRY_POS
                                                                                                                + NPP_PROTOCOL_NDEF_ENTRY_HEADER_NDEF_LENGTH_POS]));


            nLength = ( ( NPP_PROTOCOL_HEADER_LENGTH
                        + NPP_PROTOCOL_NDEF_ENTRY_HEADER_LENGTH
                        + pNPPClient->pFirstMessage->nLength ) <= pNPPClient->nSocketMIU) ? pNPPClient->pFirstMessage->nLength
                                                                                          : pNPPClient->nSocketMIU - ( NPP_PROTOCOL_HEADER_LENGTH
                                                                                                                     + NPP_PROTOCOL_NDEF_ENTRY_HEADER_LENGTH);

            CMemoryCopy(&pNPPClient->aTransmitBuffer[NPP_PROTOCOL_HEADER_LENGTH + NPP_PROTOCOL_NDEF_ENTRY_HEADER_LENGTH], pNPPClient->pFirstMessage->pBuffer, nLength);
            pNPPClient->pFirstMessage->nIndex = nLength;

            if (pNPPClient->pFirstMessage->nIndex == pNPPClient->pFirstMessage->nLength)
            {
               PDebugTrace("static_NPPClientAutomaton: SENDING_NPP_HEADER : complete message");
            }
            else
            {
               pNPPClient->nTransmissionState = SENDING_NPP_NDEF_MESSAGE;
               PDebugTrace("static_NPPClientAutomaton : SENDING_NPP_HEADER : fragmented message");
            }

            nLength   += NPP_PROTOCOL_HEADER_LENGTH
                       + NPP_PROTOCOL_NDEF_ENTRY_HEADER_LENGTH;

            bSendPacket = W_TRUE;
         }
         else
         {
            static_NPPClientPostResult(pContext, pNPPClient->pFirstMessage, W_SUCCESS);
         }

         break;

      case SENDING_NPP_NDEF_MESSAGE:

         PDebugTrace("static_NPPClientAutomaton: SENDING_NPP_NDEF_MESSAGE");

         PDebugTrace("static_NPPClientAutomaton: current index %d - total length %d", pNPPClient->pFirstMessage->nIndex, pNPPClient->pFirstMessage->nLength);
         nLength = pNPPClient->pFirstMessage->nLength - pNPPClient->pFirstMessage->nIndex;

         if (nLength > pNPPClient->nSocketMIU)
         {
            nLength = pNPPClient->nSocketMIU;
         }

         CMemoryCopy(pNPPClient->aTransmitBuffer, pNPPClient->pFirstMessage->pBuffer + pNPPClient->pFirstMessage->nIndex, nLength);
         pNPPClient->pFirstMessage->nIndex += nLength;

         if(pNPPClient->pFirstMessage->nIndex == pNPPClient->pFirstMessage->nLength)
         {
            pNPPClient->nTransmissionState = SENDING_NPP_HEADER;
         }

         bSendPacket = W_TRUE;

         break;

      default:
         /* Should not get here as the value of pSNEClient->nTransmissionState should always be SENDING_NPP_HEADER or SENDING_NPP_FRAGMENT */
         CDebugAssert(0);
         break;
   }

   /* temporary drop current instance since socket is not associated to a specific user */
   pUserInstance = PContextGetCurrentUserInstance(pContext);
   PContextSetCurrentUserInstance(pContext, null);

   if (bSendPacket != W_FALSE)
   {
      PP2PWriteDriverInternal(pContext, pNPPClient->hClientSocket, static_NPPClientDataTransmitted, pNPPClient, pNPPClient->aTransmitBuffer, nLength, &pNPPClient->hTransmitOperation);
   }

   /* restore the current instance */
   PContextSetCurrentUserInstance(pContext, pUserInstance);
}

static void static_NPPClientShutdown(tContext * pContext, void * pCallbackParameter, W_ERROR nError)
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);
   PBasicCloseHandle(pContext, pNPPClient->hClientSocket);
   pNPPClient->hClientSocket = W_NULL_HANDLE;
   pNPPClient->bClientConnected = W_FALSE;

   /* request the termination of the P2P RF link if established */
   PBasicCloseHandle(pContext, pNPPClient->hLink);
   pNPPClient->hLink = W_NULL_HANDLE;

   PDebugTrace("static_NPPClientShutdown");
}

static void static_NPPClientDataTransmitted( tContext* pContext, void * pCallbackParameter, W_ERROR nResult )
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);

   PBasicCloseHandle(pContext, pNPPClient->hTransmitOperation);
   pNPPClient->hTransmitOperation = W_NULL_HANDLE;

   PDebugTrace("static_NPPClientDataTransmitted %d", nResult);

   /* If the shutdown is requested we stop the process */
   if(pNPPClient->bShutdownRequested == W_TRUE)
   {
      PDebugTrace("static_NPPClientDataTransmitted shutdown pending");
      /* Do nothing, the shutdown is pending */
      return;
   }

   if (nResult == W_SUCCESS)
   {
      /* go on message transmission if fragmented */
      if (pNPPClient->nTransmissionState == SENDING_NPP_NDEF_MESSAGE)
      {
         static_NPPClientAutomaton(pContext);
      }else
      {
         /* Post the callback and continue sending message */
         static_NPPClientPostResult(pContext,
                                    pNPPClient->pFirstMessage,
                                    W_SUCCESS);

         PP2PShutdownDriverInternal(pContext,
                                    pNPPClient->hClientSocket,
                                    static_NPPClientShutdown,
                                    null);
      }
   }
   else

   {
      PDebugError("static_NPPClientDataTransmitted %d", nResult);

      /* A write operation failure means the client is no longer connected */
      static_PNPPClientGeneralFailure(pContext, null, nResult);
   }
}

/*
 * This function is called when the P2P link is broken
 *
 * Depending on the existence of at least one registered handler, we must request link establishment once again
 * or perform final cleanup (if any)
 */

static void static_PNPPClientLinkReleaseCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult)
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);  /* can not be null */

   PDebugTrace("static_PNPPClientLinkReleaseCallback %d\n", nResult);

   if (pNPPClient->hLink != W_NULL_HANDLE)
   {
      PBasicCloseHandle(pContext, pNPPClient->hLink);
      pNPPClient->hLink = W_NULL_HANDLE;
   }


   /* Other NDEF Message have to be sent, we need to create the Socket again */
   if(pNPPClient->pFirstMessage != null)
   {

      tUserInstance * pCurrentUserInstance;
      tWP2PConfiguration sConfiguration;
      W_ERROR nError;
      W_HANDLE hLink;

      /* We must keep the number of registrations :
         each time a new registration is done, the usage count is incremented.
         the socket must be closed and the link establishment must be destroyed  when the usage count drops to zero */

      CDebugAssert(pNPPClient->hClientSocket == W_NULL_HANDLE);
      CDebugAssert(pNPPClient->hLinkOperation == W_NULL_HANDLE);
      CDebugAssert(pNPPClient->hLink == W_NULL_HANDLE);
      CDebugAssert(pNPPClient->hTransmitOperation== W_NULL_HANDLE);


      PDebugTrace("static_PNPPClientLinkReleaseCallback : starting new session, other Message have to be sent");

      pNPPClient->bShutdownRequested = W_FALSE;
      pNPPClient->bClientConnected = W_FALSE;

      /* Handles are by default associated to the current user instance, meaning they are automatically closed
         * when the user instance is destroyed. Here, we only create one socket and make only one link establishment
         * requets for several clients, so we need to drop temporary the current instance for these operations.
         *
         * NOTE: The user instance must be restored at the end
         */

      pCurrentUserInstance = PContextGetCurrentUserInstance(pContext);

      PContextSetCurrentUserInstance(pContext, null);
      nError = PP2PCreateSocketDriver(pContext, W_P2P_TYPE_CLIENT, g_NPPUri, sizeof(g_NPPUri), 0, &pNPPClient->hClientSocket);

      if (nError != W_SUCCESS)
      {
         PDebugError("static_PNPPClientLinkReleaseCallback : PP2PCreateSocketDriver failed %d", nError);

         /* Restore the current user instance */
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto error;
      }

      nError = PP2PGetConfigurationDriver(pContext, &sConfiguration, sizeof(sConfiguration));
      if (nError != W_SUCCESS)
      {
         PDebugError("static_PNPPClientLinkReleaseCallback : PP2PGetConfigurationDriver failed %d", nError);
         goto error;
      }

      nError = PP2PSetSocketParameter(pContext, pNPPClient->hClientSocket, W_P2P_LOCAL_MIU, sConfiguration.nLocalMIU);
      if (nError != W_SUCCESS)
      {
         PDebugError("static_PNPPClientLinkReleaseCallback : PP2PSetSocketParameter failed %d", nError);
         goto error;
      }

      /* Request link establishment */

      /* Since we are in the driver part of the stack, we must call internal function instead of PP2PEstablishLinkDriverInternal()
         which can only be called from user part
      */
      hLink = PP2PEstablishLinkDriver1Internal(pContext, static_PNPPClientLinkEstablishmentCallback, null);

      if (hLink == W_NULL_HANDLE)
      {
         PDebugError("PNDEFSendNPPMessageDriver : PP2PEstablishLinkDriver1Internal failed");

         /* Restore the current user instance */
         PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);
         goto error;
      }

      PP2PEstablishLinkDriver2Internal(pContext, hLink, static_PNPPClientLinkReleaseCallback, null, &pNPPClient->hLinkOperation);

      /* Restore the current user instance */
      PContextSetCurrentUserInstance(pContext,  pCurrentUserInstance);

      pNPPClient->bClientConnected = W_FALSE;

error:
      static_PNPPClientGeneralFailure(pContext,
                                      null,
                                      W_SUCCESS);
   }
}


static void static_PNPPClientGeneralFailure(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nResult)
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);
   tPNPPMessage * pMessage = null;
   tPNPPMessage * pMessageNext = null;
   bool_t bAtLeastOneMessage = W_FALSE;

   PDebugTrace("static_PNPPClientGeneralFailure %d", nResult);


   pMessage = pNPPClient->pFirstMessage;
   while (pMessage != null)
   {
      /* Save the next pMessage */
      pMessageNext = pMessage->pNext;

      pMessage->nError = nResult;
      PBasicCancelOperation(pContext, pMessage->hOperation);
      bAtLeastOneMessage = W_TRUE;

      /* Set the current message to the value of the next message */
      pMessage = pMessageNext;
   }

   if(bAtLeastOneMessage == W_FALSE)
   {
      static_PNDEFMessageDestroyAutomaton(pContext, null);
   }
}


/*
 * This is the automaton of the destruction of the send message operation
 * The callback is called once all cleanup have been performed.
 */

static void static_PNDEFMessageDestroyAutomaton(tContext* pContext, tPNPPMessage * pMessage)
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);

   /* Request shutdown of the socket connection */
   if ((pNPPClient->hClientSocket != W_NULL_HANDLE) && (pNPPClient->bShutdownRequested == W_FALSE))
   {
      PDebugTrace("static_PNDEFMessageDestroyAutomaton : terminating client socket connection");
      PP2PShutdownDriverInternal(pContext, pNPPClient->hClientSocket, static_PNPPClientShutdownConnectionCallback, pMessage);
      pNPPClient->bShutdownRequested = W_TRUE;

      return;
   }
   /* close the server socket */
   if (pNPPClient->hClientSocket != W_NULL_HANDLE)
   {
      PDebugTrace("static_PNDEFMessageDestroyAutomaton : closing client socket");
      PBasicCloseHandle(pContext, pNPPClient->hClientSocket);
      pNPPClient->hClientSocket = W_NULL_HANDLE;
   }

   if (pNPPClient->hLink != W_NULL_HANDLE)
   {
      /* request the termination of the P2P RF link if established */
      PDebugTrace("static_PNDEFMessageDestroyAutomaton : closing link");
      PBasicCloseHandle(pContext, pNPPClient->hLink);
      pNPPClient->hLink = W_NULL_HANDLE;
   }

   if(pNPPClient->hLinkOperation != W_NULL_HANDLE)
   {
      /* Cancel link establishment if the RF link is not established */
      PDebugTrace("static_PNDEFMessageDestroyAutomaton : aborting link establishment");
      PBasicCancelOperation(pContext, pNPPClient->hLinkOperation);
      PBasicCloseHandle(pContext, pNPPClient->hLinkOperation);

      pNPPClient->hLinkOperation = W_NULL_HANDLE;
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

static void static_PNPPClientShutdownConnectionCallback( tContext* pContext, void * pCallbackParameter, W_ERROR nResult )
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);

   pNPPClient->bClientConnected = W_FALSE;

   PDebugTrace("static_PNPPClientShutdownConnectionCallback %d", nResult);

   CDebugAssert(pNPPClient->pFirstMessage == null);
   static_PNDEFMessageDestroyAutomaton(pContext, (tPNPPMessage *) pCallbackParameter);
}


/*
 * This function is called when an error occurs or when the user cancels the operation
 */
static void static_PNDEFSendNPPMessageDriverCancel(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing)
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);
   tPNPPMessage * pMessage = (tPNPPMessage *) pCancelParameter;

   /* Save the current User Instance and restore it at the end */
   tUserInstance * pUserInstance = PContextGetCurrentUserInstance(pContext);
   PContextSetCurrentUserInstance(pContext, null);

   CDebugAssert(pCancelParameter != null);

   PDebugTrace("static_PNDEFSendNPPMessageDriverCancel %d", bIsClosing);

   if ((bIsClosing == W_FALSE /* For Cancel */) || (pMessage->nError != W_SUCCESS))
   {

      /* Remove the pMessage if it was called before */
      if( pNPPClient->pFirstMessage == pMessage)
      {
         pNPPClient->pFirstMessage = pMessage->pNext;
      }
      else
      {
         tPNPPMessage * pTmp = pNPPClient->pFirstMessage;
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

      if (pNPPClient->pFirstMessage == null)
      {
         PDebugTrace("static_PNDEFSendNPPMessageDriverCancel : no more client registered");
         static_PNDEFMessageDestroyAutomaton(pContext, null);
      }
   }
   PContextSetCurrentUserInstance(pContext, pUserInstance);
}


static void static_NPPClientPostResult(tContext* pContext, tPNPPMessage * pMessage, W_ERROR nError)
{
   tNPPClientDriverInstance * pNPPClient = PContextGetNPPClientDriverInstance(pContext);
   tPNPPMessage * pCurrentMessage;
   tPNPPMessage * pLastMessage;

   PDebugTrace("static_NPPClientPostResult %d", nError);

   if (pMessage != null)
   {
      /* Remove the message from the list (if present) */

      pCurrentMessage = pNPPClient->pFirstMessage;
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
            pNPPClient->pFirstMessage = pMessage->pNext;
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
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC && defined P_INCLUDE_SNEP_NPP */

#if ((P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (! defined P_INCLUDE_SNEP_NPP)

/* Dummy functions to allow proper link of the solution even if P_INCLUDE_SNEP_NPP is not defined
   (autogen limitation workaround */

W_ERROR PNDEFRegisterNPPMessageHandlerDriver  (
      tContext * pContext,
      tPBasicGenericDataCallbackFunction* pHandler,
      void *  pHandlerParameter,
      uint8_t nPriority,
      W_HANDLE *  phRegistry )
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}


W_ERROR PNDEFRetrieveNPPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

W_HANDLE PNDEFSendNPPMessageDriver(
      tContext * pContext,
      uint8_t * pBuffer,
      uint32_t nBufferLength,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter)
{
   return W_NULL_HANDLE;
}

void PNDEFSetWorkPerformedNPPDriver(
      tContext * pContext,
      bool_t     bGiveToNextListener)
{
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC && ! defined P_INCLUDE_SNEP_NPP */

/* EOF */
