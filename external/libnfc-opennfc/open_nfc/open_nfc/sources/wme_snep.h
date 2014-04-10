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
   Contains the declaration of the functions for the SNEP server
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */


#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

typedef struct __tPSNEPMessageHandler        tPSNEPMessageHandler;
typedef struct __tSNEPServerNDEFMessage      tSNEPServerNDEFMessage;
typedef struct __tSNEPServerDriverInstance   tSNEPServerDriverInstance;

struct __tSNEPServerNDEFMessage
{
   /* Pointer to buffer that shall be dynamically allocated */
   uint8_t * pReceiveBuffer;

   /* The number of bytes contained within the received NDEF message */
   uint32_t nNDEFMessageLength;

   /* The index with the buffer where newly received data should be placed */
   uint32_t nNDEFBufferIndex;
};

struct __tSNEPServerDriverInstance
{
   W_HANDLE hServerSocket;
   uint32_t nServerSocketUsageCount;

   W_HANDLE hLinkOperation;
   W_HANDLE hLink;

   W_HANDLE hReceiveOperation;
   W_HANDLE hTransmitOperation;

   uint8_t aReceiveBuffer[256];
   uint8_t aTransmitBuffer[256];

   /* The received NDEF message */
   tSNEPServerNDEFMessage sReceivedNDEFMessage;

   /* The list of the SNEP message handlers */
   tPSNEPMessageHandler   * pFirstMessageHandler;
   tPSNEPMessageHandler   * pLastMessageHandlerCalled;

   /* The state of the SNEP message reception */
   uint8_t nSNEPReceptionState;

   bool_t  bShutdownRequested;
   bool_t  bConnectionPending;
} ;

/* Declare a SNEP Server Message Handler structure */

struct __tPSNEPMessageHandler
{
   /* All objects registered using PHandleRegister must begin with a tHandleObjectHeader structure */
   tHandleObjectHeader        sObjectHeader;

   /* Handler priority */
   uint8_t nPriority;

   /* Indicates if this message handler has already been called */
   bool_t bIsCalled;

   /*  The SNEPServer context contain the list of all message handler registered.
       This field allows to link them together...  */
   tPSNEPMessageHandler * pNext;

   /* The callback context describing the callback function to be called each time a NDEF message has been
      received */
   tDFCDriverCCReference  pDriverCC;

   /* the callback context describing the callback function to be called once the message handler has been destroyed */
   tDFCCallbackContext    sDestroyCallbackContext;

};


/** Creates the SNEP Server instance */
void PSNEPServerDriverInstanceCreate(
         tSNEPServerDriverInstance* pSNEPServer );

/** Destroyes the SNEP Server instance */
void PSNEPServerDriverInstanceDestroy(
         tSNEPServerDriverInstance* pSNEPServer );



typedef struct __tSNEPClientDriverInstance   tSNEPClientDriverInstance;
typedef struct __tSNEPMessage                tPSNEPMessage;


struct __tSNEPClientDriverInstance
{
   /* The client socket handle */
   W_HANDLE hClientSocket;

   /* Indicates if the socket is connected */
   bool_t   bClientConnected;

   /* The current link establishment operation */
   W_HANDLE hLinkOperation;

   /* The current link handle, if P2P is established */
   W_HANDLE hLink;

   /* The current P2P receive operation */
   W_HANDLE hReceiveOperation;

   /* The current P2P transmit operation */
   W_HANDLE hTransmitOperation;

   /* The receive buffer */
   uint8_t aReceiveBuffer[256];

   /* The transceive buffer */
   uint8_t aTransmitBuffer[256];

   /* The maximum packet size to be sent to the peer */
   uint32_t  nSocketMIU;

   /* The list of the SNEP message to be sent */
   tPSNEPMessage   * pFirstMessage;

   /* The state of the SNEP message transmission */
   uint8_t nSNEPTransmissionState;

   bool_t  bShutdownRequested;

   bool_t  bConnectionPending; /* Used to avoid multiple connection */
} ;


struct __tSNEPMessage
{
   uint8_t * pBuffer;
   uint32_t  nLength;

   uint32_t  nIndex;

   /*  The SNEP client context contain the list of all message handler registered.
       This field allows to link them together...  */
   tPSNEPMessage   * pNext;

   /* The callback context describing the callback function to be called when the NDEF message has been sent */
   tDFCDriverCCReference  pDriverCC;

   /* The operation handle */
   W_HANDLE hOperation;

   W_ERROR nError;
};

/** Creates the SNEP Client instance */
void PSNEPClientDriverInstanceCreate(
         tSNEPClientDriverInstance* pSNEPServer );

/** Destroyes the SNEP Client instance */
void PSNEPClientDriverInstanceDestroy(
         tSNEPClientDriverInstance* pSNEPServer );


#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* EOF */
