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
   Contains the declaration of the functions for the NPP server
*******************************************************************************/

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_SNEP_NPP)

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */


#if ((P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_SNEP_NPP)

typedef struct __tPNPPMessageHandler        tPNPPMessageHandler;
typedef struct __tNPPServerNDEFMessage      tNPPServerNDEFMessage;
typedef struct __tNPPServerDriverInstance   tNPPServerDriverInstance;

struct __tNPPServerNDEFMessage
{
   /* Pointer to buffer that shall be dynamically allocated */
   uint8_t * pReceiveBuffer;

   /* The number of bytes contained within the received NDEF message */
   uint32_t nNDEFMessageLength;

   uint32_t nNDEFBufferIndex;
};

struct __tNPPServerDriverInstance
{
   W_HANDLE hServerSocket;
   uint32_t nServerSocketUsageCount;

   W_HANDLE hLinkOperation;
   W_HANDLE hLink;

   W_HANDLE hReceiveOperation;

   uint8_t aReceiveBuffer[256];

   /* The received NDEF message */
   tNPPServerNDEFMessage sReceivedNDEFMessage;

   /* The list of the NPP message handlers */
   tPNPPMessageHandler   * pFirstMessageHandler;

   /* Used to identify which Handler has been called */
   tPNPPMessageHandler   * pLastMessageHandlerCalled;

   /* NPP state */
   uint8_t nNPPReceptionState;

   bool_t  bShutdownRequested;
   bool_t  bConnectionPending;
} ;

/* Declare a SNEP Server Message Handler structure */

struct __tPNPPMessageHandler
{
   /* All objects registered using PHandleRegister must begin with a tHandleObjectHeader structure */
   tHandleObjectHeader        sObjectHeader;

   /* Indicates if this message handler has already been called */
   bool_t bIsCalled;

   /*  The SNEPServer context contain the list of all message handler registered.
       This field allows to link them together...  */
   tPNPPMessageHandler * pNext;

   /* The callback context describing the callback function to be called each time a NDEF message has been
      received */
   tDFCDriverCCReference  pDriverCC;

   /* The priority */
   uint8_t                nPriority;

   /* the callback context describing the callback function to be called once the message handler has been destroyed */
   tDFCCallbackContext    sDestroyCallbackContext;

};

/** Creates the NPP server instance */
void PNPPServerDriverInstanceCreate(
         tNPPServerDriverInstance* pNPPServer );

/** Destroyes the NPP server instance */
void PNPPServerDriverInstanceDestroy(
         tNPPServerDriverInstance* pNPPerver );



typedef struct __tNPPClientDriverInstance   tNPPClientDriverInstance;
typedef struct __tNPPMessage                tPNPPMessage;


struct __tNPPClientDriverInstance
{
   /* The client socket handle */
   W_HANDLE hClientSocket;

   /* Indicates if the socket is connected */
   bool_t   bClientConnected;

   /* The current link establishment operation */
   W_HANDLE hLinkOperation;

   /* The current link handle, if P2P is established */
   W_HANDLE hLink;

   /* The current P2P transmit operation */
   W_HANDLE hTransmitOperation;

   /* The transceive buffer */
   uint8_t aTransmitBuffer[256];
   bool_t  nTransmissionState;

   /* The maximum packet size to be sent to the peer */
   uint32_t  nSocketMIU;

   /* The unique NPP message to be sent */
   tPNPPMessage   * pFirstMessage;

   bool_t  bShutdownRequested;
} ;


struct __tNPPMessage
{
   uint8_t * pBuffer;
   uint32_t  nLength;

   uint32_t  nIndex;

   /*  The SNEP client context contain the list of all message handler registered.
       This field allows to link them together...  */
   tPNPPMessage   * pNext;

   /* The callback context describing the callback function to be called when the NDEF message has been sent */
   tDFCDriverCCReference  pDriverCC;

   /* The operation handle */
   W_HANDLE hOperation;
   W_ERROR nError;
};

/** Creates the SNEP Client instance */
void PNPPClientDriverInstanceCreate(
         tNPPClientDriverInstance* pNPPClient );

/** Destroyes the SNEP Client instance */
void PNPPClientDriverInstanceDestroy(
         tNPPClientDriverInstance* pNPPClient );


#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */


/* EOF */
