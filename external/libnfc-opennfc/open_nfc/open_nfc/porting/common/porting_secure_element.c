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

  Implementation of the Secure Element HAL.

*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( SE_HAL )

#include "wme_context.h"
#include "porting_os.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifdef P_INCLUDE_SE_SECURITY

#ifdef USE_CCCLIENT

#include "ccclient.h"

#include <pthread.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/poll.h>


/* The maximim length of the receive buffer */
#define CCCLIENT_RECV_BUFFER_LENGTH  4096

/** The maximum number of slots managed by this implementation */
#define P_SE_HAL_NUM_SLOTS 4
/* The maximum number of channels per slot */
#define P_SE_MAX_CHANNELS  4
/* The minimum length of a message received from PCSC Server (via CCClient) */
#define P_SE_RECEIVED_MESSAGE_MIN_LENGTH  (3 * sizeof(uint32_t) + 1)
/* The maximum number of bytes in a SE name */
#define P_SE_NAME_MAX_LENGTH  64
/* The maximum number of bytes in the response to a CREATE message */
#define P_SE_CREATE_RESPONSE_MAX_LENGTH (2 * sizeof(uint32_t) + P_SE_HAL_NUM_SLOTS * (3 * sizeof(uint32_t) + P_SE_NAME_MAX_LENGTH))

/* Operation CREATE */
#define P_SE_OPERATION_CREATE    ((uint32_t)-1)


/* used for communication interthread (socket index) */
#define P_SE_SOCKET_MAIN_THREAD      0
#define P_SE_SOCKET_RECEIVER_THREAD  1

/**
 * Data structure exchanged with the Thread used to call Callback
**/
typedef struct __tSeCallbackParameters
{
   uint32_t nSlotIdentifier;
   uint32_t nOperation;
   bool_t   bSuccess;
   uint32_t nParam1;
   uint32_t nParam2;
} tSeCallbackParameters;


/* Structure to store an APDU Response for a slot/channel */
typedef struct __tSavedAPDUResponse
{
   /* Channel identifier */
   uint32_t nChannelIdentifier;
   /* Buffer to receive APDU response */
   uint8_t* pAPDUResponseBuffer;
   /* Size of pAPDUResponseBuffer */
   uint32_t nResponseAPDUBufferLength;
} tSavedAPDUResponse;


/* The Slot data */
typedef struct __tCSePortingSlot
{
   /* The slot identifier */
   uint32_t nSlotIdentifier;
   /* The slot flags */
   uint32_t nFlags;
   /* The name of the SE slot */
   uint8_t* pName;
   /* The length of pName */
   uint32_t nNameLength;
   /* Pointer to the HAL buffer to receive APDU response */
   uint8_t* pResponseApduBuffer;
   /* Size of pResponseApduBuffer */
   uint32_t nResponseApduBufferLength;
   /* Array of structures to store the APDU response in case of
      pResponseApduBuffer is too short or in case of OpenChannel */
   tSavedAPDUResponse aSavedAPDUResponses[P_SE_MAX_CHANNELS];
}  tCSePortingSlot;

struct __tCSePorting
{
   /* CCClient connection */
   void * pConnection;

   /* Current Thread */
   pthread_t sThread;

   /* Socket descriptor used for sending/receiving events */
   int nSDCCClient;
   int nSDMessageQueue[2];
   int nSDStartStopRequest[2];

   /* Mutex to protect SE Access */
   pthread_mutex_t   sMutexSEAccess;

   /* callback used to notify operation completion and hot-plug events */
   tCSeCallback *    pCallback;
   void *            pCallbackParameter;

   /* SE Slot array */
   uint32_t          nActiveSlot;
   tCSePortingSlot * pSlots;

   /* The refresh buffer and its length */
   uint8_t* pRefreshFileList;
   uint32_t nRefreshFileListLength;
};


/* Method used to post callback */
static void static_CSeSendMessageCallback(
         tCSePorting * pSePorting,
         uint32_t      nSlotIdentifier,
         uint32_t      nOperation,
         bool_t        bSuccess,
         uint32_t      nParam1,
         uint32_t      nParam2)
{
   tSeCallbackParameters sParams = { nSlotIdentifier,
                                     nOperation,
                                     bSuccess,
                                     nParam1,
                                     nParam2 };

   (void) write(pSePorting->nSDMessageQueue[P_SE_SOCKET_MAIN_THREAD], &sParams, sizeof(sParams));
}

static void static_CSeReceiveMessageCallback(
        tCSePorting * pSePorting,
        tSeCallbackParameters * pParams)
{
   (void) read(pSePorting->nSDMessageQueue[P_SE_SOCKET_RECEIVER_THREAD], pParams, sizeof(tSeCallbackParameters));
}


/**
 *  Methods used to send/receive Start or Stop Thread
**/
static void static_CSeSendStartEvent(tCSePorting * pSePorting)
{
   uint8_t nTmp = 0;
   (void) write( pSePorting->nSDStartStopRequest[P_SE_SOCKET_RECEIVER_THREAD], &nTmp, 1);
}

static void static_CSeWaitStartEvent(tCSePorting * pSePorting)
{
   uint8_t nTmp = 0;
   (void) read( pSePorting->nSDStartStopRequest[P_SE_SOCKET_MAIN_THREAD], &nTmp, 1);
}

static void static_CSeSendStopEvent(tCSePorting * pSePorting)
{
   uint8_t nTmp = 0;
   (void) write( pSePorting->nSDStartStopRequest[P_SE_SOCKET_MAIN_THREAD], &nTmp, 1);
}

static void static_CSeWaitStopEvent(tCSePorting * pSePorting)
{
   uint8_t nTmp = 0;
   (void) read( pSePorting->nSDStartStopRequest[P_SE_SOCKET_RECEIVER_THREAD], &nTmp, 1);
}

/**
 * @brief Search and return the insance of tCSePortingSlot by its Identifier
 *
 * @param[in] pCSePorting   Structure containing all information about the SE Porting
 * @param[in] nSlotId       Slot Identifier of the tCSePortingSlot desired
 *
 * @return    Address of the tCSePortingSlot if it is found. Otherwise NULL value is returned
 **/
static tCSePortingSlot * static_CSeGetSlot( tCSePorting * pCSePorting, uint32_t nSlotId)
{
   uint32_t nIndex = 0;

   CDebugAssert(pCSePorting != NULL);

   for(/* nIndex already initialized */ ; nIndex < pCSePorting->nActiveSlot; ++ nIndex)
      if( pCSePorting->pSlots[ nIndex ].nSlotIdentifier == nSlotId )
         return &pCSePorting->pSlots[nIndex];

   return NULL;
}

/* Method used for treatment of CCClientMessage */
static uint32_t static_CSeReceiveMessageCCClient(
        tCSePorting * pSePorting,
        tSeCallbackParameters * pParams)
{
   uint8_t   aBuffer[CCCLIENT_RECV_BUFFER_LENGTH];
   uint8_t * pCommand;
   uint32_t  nResult = CC_SUCCESS;
   uint32_t  nLength = 0;
   uint32_t  nOffset = 0;

   tCSePortingSlot * pSlot;

   PDebugTrace("static_CSeReceiveMessageCCClient\n");

   memset(pParams, 0x00, sizeof(tSeCallbackParameters));

   pthread_mutex_lock(&pSePorting->sMutexSEAccess);

   nLength = CCClientReceiveData(
                 pSePorting->pConnection,
                 aBuffer,
                 sizeof(aBuffer),
                 &pCommand,
                 W_TRUE);

   if(nLength >= 0)
   {
      nResult = CC_SUCCESS;

      /* Parse the received message */
      if(nLength < P_SE_RECEIVED_MESSAGE_MIN_LENGTH)
      {
        PDebugError("static_CSeParseMessage: wrong message length (%d). This message is ignored.", nLength);
        nResult = (uint32_t) (-1);
        goto end;
      }

      pParams->nOperation = PUtilReadUint32FromLittleEndianBuffer(&pCommand[nOffset]);
      nOffset += sizeof(uint32_t);

      pParams->nSlotIdentifier = PUtilReadUint32FromLittleEndianBuffer(&pCommand[nOffset]);
      nOffset += sizeof(uint32_t);

      pSlot = static_CSeGetSlot(pSePorting, pParams->nSlotIdentifier);
      if(pSlot == NULL)
      {
         PDebugError("static_CSeParseMessage: unknown slot identifier (%d)", pParams->nSlotIdentifier);
         goto end;
      }

      pParams->bSuccess = PUtilReadBoolFromLittleEndianBuffer(&pCommand[nOffset]);
      nOffset += sizeof(uint8_t);

      pParams->nParam1 = PUtilReadUint32FromLittleEndianBuffer(&pCommand[nOffset]);
      nOffset += sizeof(uint32_t);

      PDebugTrace("static_CSeParseMessage: slot (%d), operation (%d), success (%d), channel (%d)",
                    pParams->nSlotIdentifier,
                    pParams->nOperation,
                    pParams->bSuccess,
                    pParams->nParam1);

      switch(pParams->nOperation)
      {
         case C_SE_OPERATION_GET_INFO:
            if( nLength < nOffset + sizeof(uint32_t))
            {
               PDebugError("static_CSeParseMessage: C_SE_OPERATION_GET_INFO. Wrong message length (%d)", nLength);
               pParams->bSuccess = W_FALSE;
            }
            else
            {
               /* Retrieve ATR Length */
               pParams->nParam2 = PUtilReadUint32FromLittleEndianBuffer(&pCommand[nOffset]);
               nOffset += sizeof(uint32_t);

               PDebugTrace("static_CSeParseMessage: C_SE_OPERATION_GET_INFO (halId=%d sessionId=0x%X ATRlength=%d)",
                               pParams->nSlotIdentifier,
                               pParams->nParam1,
                               pParams->nParam2);

               if (nLength < nOffset + pParams->nParam2)
               {
                  PDebugError("static_CSeParseMessage: C_SE_OPERATION_GET_INFO. Wrong message length (%d)", nLength);
                  pParams->bSuccess = W_FALSE;
                  pParams->nParam2 = 0;
               }
               else if (pParams->nParam2 > 0)
               {
                  if (pSlot->nResponseApduBufferLength < pParams->nParam2)
                  {
                     /* Buffer too short */
                     PDebugWarning("static_CSeParseMessage: C_SE_OPERATION_GET_INFO. Buffer too short. ATR is truncated.");
                     pParams->nParam2 = pSlot->nResponseApduBufferLength;
                  }
                  CDebugAssert(pSlot->pResponseApduBuffer != NULL);
                  /* Copy ATR */
                  CMemoryCopy(pSlot->pResponseApduBuffer, &pCommand[nOffset], pParams->nParam2);
               }
            }
            break;

         case C_SE_OPERATION_OPEN:
            if (nLength < nOffset + sizeof(uint32_t))
            {
               PDebugError("static_CSeParseMessage: C_SE_OPERATION_OPEN. Wrong message length (%d)", nLength);
               pParams->bSuccess = W_FALSE;
            }
            else
            {
               /* Retrieve FCI data length */
               pParams->nParam2 = PUtilReadUint32FromLittleEndianBuffer(&pCommand[nOffset]);
               nOffset += sizeof(uint32_t);

               if (nLength < nOffset + pParams->nParam2)
               {
                  PDebugError("static_CSeParseMessage: C_SE_OPERATION_OPEN. Wrong message length (%d)", nLength);
                  pParams->bSuccess = W_FALSE;
                  pParams->nParam2 = 0;
               }
               else
               {
                  uint32_t nChannelIndex;

                  for (nChannelIndex = 0; nChannelIndex < P_SE_MAX_CHANNELS; nChannelIndex++)
                  {
                     if (pSlot->aSavedAPDUResponses[nChannelIndex].nChannelIdentifier == 0)
                        break;
                  }

                  if (nChannelIndex >= P_SE_MAX_CHANNELS)
                  {
                     PDebugError("static_CSeParseMessage: C_SE_OPERATION_OPEN. Out of resource (channels)");
                     pParams->bSuccess = W_FALSE;
                     pParams->nParam2 = 0;
                  }
                  else
                  {
                     /* The FCI data (answer to select) must be saved in order to be retrieved with CSeGetResponseApdu */
                     CDebugAssert(pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer == NULL);
                     CDebugAssert(pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength == 0);

                     /* Save channel identifier */
                     pSlot->aSavedAPDUResponses[nChannelIndex].nChannelIdentifier = pParams->nParam1;

                     if (pParams->nParam2 > 0)
                     {
                        /* Allocate the buffer */
                        pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer = (uint8_t*) CMemoryAlloc(pParams->nParam2);
                        if (pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer == NULL)
                        {
                           PDebugError("static_CSeParseMessage: C_SE_OPERATION_OPEN. Out of memory");
                           pParams->bSuccess = W_FALSE;
                           pParams->nParam2 = 0;
                           /* Clear channel identifier */
                           pSlot->aSavedAPDUResponses[nChannelIndex].nChannelIdentifier = 0;
                        }
                        else
                        {
                           /* Save FCI data (answer to select) */
                           CMemoryCopy(pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer, &pCommand[nOffset], pParams->nParam2);
                           pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength = pParams->nParam2;
                        }
                     }
                  }
               }
            }
            break;

         case C_SE_OPERATION_CLOSE:
            /* Parameter bSuccess must be W_TRUE even in case of failure */
            pParams->bSuccess = W_TRUE;
            break;

         case C_SE_OPERATION_EXCHANGE:
            if (nLength < nOffset + sizeof(uint32_t))
            {
               PDebugError("static_CSeParseMessage: C_SE_OPERATION_EXCHANGE. Wrong message length (%d)", nLength);
               pParams->bSuccess = W_FALSE;
            }
            else
            {
               pParams->nParam2 = PUtilReadUint32FromLittleEndianBuffer(&pCommand[nOffset]);
               nOffset += sizeof(uint32_t);

               if (nLength < nOffset + pParams->nParam2)
               {
                  PDebugError("static_CSeParseMessage: C_SE_OPERATION_EXCHANGE. Wrong message length (%d)", nLength);
                  pParams->bSuccess = W_FALSE;
                  pParams->nParam2 = 0;
               }
               else if (pSlot->nResponseApduBufferLength < pParams->nParam2)
               {
                  uint32_t nChannelIndex;

                  /* Buffer too short */
                  PDebugError("static_CSeParseMessage: C_SE_OPERATION_EXCHANGE. Buffer too short");

                  for (nChannelIndex = 0; nChannelIndex < P_SE_MAX_CHANNELS; nChannelIndex++)
                  {
                     if (pSlot->aSavedAPDUResponses[nChannelIndex].nChannelIdentifier == pParams->nParam1)
                        break;
                  }

                  if (nChannelIndex >= P_SE_MAX_CHANNELS)
                  {
                     PDebugError("static_CSeParseMessage: C_SE_OPERATION_EXCHANGE. Unknown channel identifier");
                     pParams->nParam2 = 0;
                  }
                  else
                  {
                     /* The APDU response must be saved in order to be retrieved with CSeGetResponseApdu */
                     CDebugAssert(pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer == NULL);
                     CDebugAssert(pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength == 0);

                     /* Allocate the buffer */
                     pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer = (uint8_t*) CMemoryAlloc(pParams->nParam2);
                     if (pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer == NULL)
                     {
                        PDebugError("static_CSeParseMessage: C_SE_OPERATION_EXCHANGE. Out of memory");
                        pParams->nParam2 = 0;
                     }
                     else
                     {
                        /* Save APDU response */
                        CMemoryCopy(pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer, &aBuffer[nOffset], pParams->nParam2);
                        pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength = pParams->nParam2;
                     }
                  }

                  /* Report this error */
                  pParams->bSuccess = W_FALSE;
               }
               else if (pParams->bSuccess == W_TRUE)
               {
                  CDebugAssert(pSlot->pResponseApduBuffer != NULL);

                  CMemoryCopy(pSlot->pResponseApduBuffer, &pCommand[nOffset], pParams->nParam2);
                  nOffset += pParams->nParam2;
               }
            }

            /* Clear reference on response buffer */
            pSlot->pResponseApduBuffer = (uint8_t*)NULL;
            pSlot->nResponseApduBufferLength = 0;

            break;
         case C_SE_NOTIFY_HOT_PLUG:
            /* Noting to do */
            break;
         case C_SE_NOTIFY_STK_ACTIVATE_SWP:
            /* Noting to do */
            break;
         case C_SE_NOTIFY_STK_REFRESH:
            if(pParams->nParam2 > pSePorting->nRefreshFileListLength)
            {
               PDebugError("static_CSeParseMessage: Refresh buffer too large, truncating the data");
               pParams->nParam2 = pSePorting->nRefreshFileListLength;
            }
            CMemoryCopy(pSePorting->pRefreshFileList, &pCommand[nOffset], pParams->nParam2);
            break;
         default:
            PDebugError("static_CSeParseMessage: Unknown operation %d", pParams->nOperation);
            nResult = (uint32_t) (-1);
            break;
         }
      }
      else
      {
         nResult = (uint32_t) (-1);
      }
end :
   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);

   return nResult;
}


/**
 * @brief Entry point for the thread that processes the messages from the PCSC Server.
 *        It processes messages from others threads also.
 *
 * @param[in] pThreadParam Parameter given to thread ( here pCSePorting )
 **/
void * static_thread_CSeMessagesReceiverProc( void * pThreadParam)
{
   tCSePorting * pSePorting = (tCSePorting *) pThreadParam;

   uint32_t nResult = CC_SUCCESS;

   tSeCallbackParameters sParam;
   struct pollfd ufds[3];

   ufds[0].fd = pSePorting->nSDCCClient;
   ufds[0].events = POLLIN;

   ufds[1].fd = pSePorting->nSDMessageQueue[P_SE_SOCKET_RECEIVER_THREAD];
   ufds[1].events = POLLIN;

   ufds[2].fd = pSePorting->nSDStartStopRequest[P_SE_SOCKET_RECEIVER_THREAD];
   ufds[2].events = POLLIN;

   /* Set Event Thread Start */
   static_CSeSendStartEvent(pSePorting);

   while(nResult == CC_SUCCESS)
   {
      PDebugTrace("Wait in Thread");
      if( poll(ufds, 3 /* sizeof (ufds) / sizeof(ufds[0]) */, -1 /* wait infinit */ ) < 0)
      {
         PDebugError("Error during the poll");
         break;
      }

      PDebugTrace("Something Arrive");

      /* If message is received from the other thread to post a callback */
      if( ufds[1].revents & POLLIN)
      {
        (void) static_CSeReceiveMessageCallback( pSePorting, &sParam);
      }
      /* If something must be read from the CCServer */
      else if( ufds[0].revents & POLLIN)
      {
        nResult = static_CSeReceiveMessageCCClient( pSePorting, &sParam);
      }
      /* If the Stop is requested */
      else if( ufds[2].revents & POLLIN)
      {
         static_CSeWaitStopEvent(pSePorting);
         PDebugTrace("static_thread_CSeMessagesReceiverProc: Exiting this thread has been requested\n");
         break;
      }
      /* Error */
      else
      {
         /* An error occurred. Stop this thread */
         PDebugError("static_thread_CSeMessagesReceiverProc: CCClientGetReceptionEvent error: %d\n", nResult);
         break;
      }


      pSePorting->pCallback( pSePorting->pCallbackParameter,
                             sParam.nSlotIdentifier,
                             sParam.nOperation,
                             sParam.bSuccess,
                             sParam.nParam1,
                             sParam.nParam2);
   }/* while */

   pthread_exit(NULL);
   return NULL;
}


/* iso7816_4 Protocol version 1.0 */
#define CC_ISO7816_4_PROTOCOL_VERSION_1_0  0x10
static const uint8_t g_nCCIso7816ProtocolVersions[] = { CC_ISO7816_4_PROTOCOL_VERSION_1_0 };
static const char16_t g_sCCIso7816Uri[] = {'c', 'c', ':', 'i', 's', 'o', '7', '8', '1', '6', '_', '4', '?', 'n', 'a', 'm', 'e', '=', 'S', 'E', '%', '2', '0', 'H', 'A', 'L', '\0'};

/* See HAL Documentation */
tCSePorting* CSeCreate(
         tCSeCallback* pCallback,
         void* pCallbackParameter,
         uint8_t* pRefreshFileList,
         uint32_t nRefreshFileListLength)
{
   tCSePorting * pSePorting = NULL;
   uint8_t nNegociatedVersion;
   uint32_t nError;
   bool_t bCreateReceived = W_FALSE;
   uint8_t aMessageBuffer[sizeof(uint32_t)];
   uint32_t nOffset = 0;

   PDebugTrace("CSeCreate()");

   CDebugAssert(pCallback != NULL);

   pSePorting = (tCSePorting*)CMemoryAlloc(sizeof(tCSePorting));
   if (pSePorting == NULL)
   {
      PDebugError("CSeCreate: Cannot allocate the structure");
      goto error;
   }

   /* Reset the internal state  */
   CMemoryFill(pSePorting, 0x00, sizeof(tCSePorting));

   /* Initialize the critical section used to protect to the state of the SE */
   if( pthread_mutex_init(&pSePorting->sMutexSEAccess, NULL) < 0)
   {
      PDebugError("CSeCreate: Cannot Initialize the structure critical section");
      goto error;
   }

   /* create socketpair to communicate with the receiver thread */
   if( socketpair(AF_UNIX, SOCK_STREAM, 0, pSePorting->nSDStartStopRequest) < 0)
   {
      PDebugError("Error during the creation of the socket pair used to receive or send start/stop event");
      goto error_destroy_mutex;
   }


   /* create socketpair to communicate with the receiver thread */
   if( socketpair(AF_UNIX, SOCK_STREAM, 0, pSePorting->nSDMessageQueue) < 0)
   {
      PDebugError("Error during the creation of the socket pair used to receive or send start/stop event");
      goto error_close_sd_start_stop;
   }

   /* Connect to the iso7816_4 service of Connection Center */
   nError = CCClientOpen(g_sCCIso7816Uri, W_FALSE,
                         g_nCCIso7816ProtocolVersions, sizeof(g_nCCIso7816ProtocolVersions),
                         &nNegociatedVersion, &pSePorting->pConnection);

   if (nError == CC_ERROR_NO_PROVIDER)
   {
      PDebugWarning("The PC/SC server is not started. The Secure Element's porting is not used.\n");
      goto error_close_sd_message_queue;
   }

   if(nError != CC_SUCCESS)
   {
      char16_t aServiceName[51];

      /* Check the connection type */
      CCClientGetProtocol(g_sCCIso7816Uri, aServiceName, 51);

      PDebugError( "=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+\n" );
      PDebugError( "CANNOT CONNECT TO THE CONNECTION CENTER.\n" );

      switch( nError )
      {
      case CC_ERROR_CC_VERSION:
         PDebugError( "SYMPTOM: Version of the Connection Center not supported\n" );
         PDebugError( "DIAGNOSTIC: Update your version of the Connection Center\n");
         break;

      case CC_ERROR_SERVICE_VERSION:
         PDebugError( "SYMPTOM: Version of the [%s] service protocol not supported\n", (uint8_t *) aServiceName);
         PDebugError( "DIAGNOSTIC: Update your version of the PC/SC server\n");
         break;

      case CC_ERROR_URI_SYNTAX:
         PDebugError( "SYMPTOM: Syntax error in URI parameter\n" );
         break;

      case CC_ERROR_CONNECTION_FAILURE:
         PDebugError( "SYMPTOM: Impossible to connect to the Connection Center\n" );
         PDebugError( "DIAGNOSTIC: Connection Center not started, firewall, ...\n"
            "           Check Connection Center and your network configuration.\n");
         break;

      case CC_ERROR_PROVIDER_BUSY:
         PDebugError ("SYMPTOM: Service Provider for [%s] is busy\n", (uint8_t *) aServiceName);
         PDebugError( "DIAGNOSTIC: another client uses the service provider;\n"
            "            check the Service List in the Connection Center\n" );
         break;

      default:
         PDebugError( "SYMPTOM: Internal Connection Center error %d while operating on TCP/IP Socket\n", nError );
         break;
      }

      PDebugError( "=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+\n" );

      goto error_close_sd_message_queue;
   }

   /* Build the CREATE message to send to the PCSC server */
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(P_SE_OPERATION_CREATE, &aMessageBuffer[nOffset]);

   /* Send the CREATE message to the PCSC server */
   if (CCClientSendData(pSePorting->pConnection, aMessageBuffer, nOffset) == 0)
   {
      PDebugError("CSeCreate: CCClientSendData error\n");
      goto error_close_cc_connection;;
   }

   while (bCreateReceived == W_FALSE)
   {
      /* Wait for CREATE response message */
      /* Get received data */
      uint32_t nOperation;
      uint8_t aBuffer[P_SE_CREATE_RESPONSE_MAX_LENGTH];
      uint8_t* pData;
      int32_t nLength = CCClientReceiveData(
                           pSePorting->pConnection,
                           aBuffer,
                           sizeof(aBuffer),
                           &pData,
                           W_TRUE);

      if (nLength < 2 * sizeof(uint32_t))
      {
         PDebugWarning("CSeCreate: CCClientReceiveData error %d", nLength);
         continue;
      }

      /* Parse received data */
      nOffset = 0;
      /* Retrieve operation and check it is CREATE */
      nOperation = PUtilReadUint32FromLittleEndianBuffer(&pData[nOffset]);
      if (nOperation != P_SE_OPERATION_CREATE)
      {
         PDebugWarning("CSeCreate: Wrong received message. Operation is %d", nOperation);
         continue;
      }
      nOffset += sizeof(uint32_t);

      /* Retrieve the number of slots */
      pSePorting->nActiveSlot = PUtilReadUint32FromLittleEndianBuffer(&pData[nOffset]);
      nOffset += sizeof(uint32_t);

      if (pSePorting->nActiveSlot > 0)
      {
         uint32_t nSlot;

         /* Allocate slots array */
         pSePorting->pSlots = (tCSePortingSlot*) CMemoryAlloc(sizeof(tCSePortingSlot) * pSePorting->nActiveSlot);
         if (pSePorting->pSlots == NULL)
         {
            PDebugError("CSeCreate: Out of memory");
            pSePorting->nActiveSlot = 0;
            CMemoryFree(pSePorting->pSlots);
            pSePorting->pSlots = NULL;
         }

         for (nSlot = 0; nSlot < pSePorting->nActiveSlot; nSlot++)
         {
            CMemoryFill(&pSePorting->pSlots[nSlot], 0, sizeof(tCSePortingSlot));

            if ((uint32_t)nLength < nOffset + 2 * sizeof(uint32_t))
            {
               PDebugError("CSeCreate: Wrong received length %d", nLength);
               pSePorting->nActiveSlot = 0;
               CMemoryFree(pSePorting->pSlots);
               pSePorting->pSlots = NULL;
               break;
            }

            /* Retrieve slot data (identifier, flags, name length, name) */
            pSePorting->pSlots[nSlot].nSlotIdentifier = PUtilReadUint32FromLittleEndianBuffer(&pData[nOffset]);
            nOffset += sizeof(uint32_t);
            pSePorting->pSlots[nSlot].nFlags = PUtilReadUint32FromLittleEndianBuffer(&pData[nOffset]);
            nOffset += sizeof(uint32_t);
            pSePorting->pSlots[nSlot].nNameLength = PUtilReadUint32FromLittleEndianBuffer(&pData[nOffset]);
            nOffset += sizeof(uint32_t);
            if (pSePorting->pSlots[nSlot].nNameLength > 0)
            {
               if ((uint32_t)nLength < nOffset + pSePorting->pSlots[nSlot].nNameLength)
               {
                  PDebugError("CSeCreate: Wrong received length %d", nLength);
                  pSePorting->nActiveSlot = 0;
                  CMemoryFree(pSePorting->pSlots);
                  pSePorting->pSlots = NULL;
                  break;
               }

               pSePorting->pSlots[nSlot].pName = (uint8_t*) CMemoryAlloc(pSePorting->pSlots[nSlot].nNameLength);
               if (pSePorting->pSlots[nSlot].pName != NULL)
               {
                  /* Copy slot's name */
                  CMemoryCopy(pSePorting->pSlots[nSlot].pName, &pData[nOffset], pSePorting->pSlots[nSlot].nNameLength);
                  nOffset += pSePorting->pSlots[nSlot].nNameLength;
               }
               else
               {
                  PDebugError("CSeCreate: Out of memory (pName == NULL)");
                  /* skip name */
                  nOffset += pSePorting->pSlots[nSlot].nNameLength;
                  pSePorting->pSlots[nSlot].nNameLength = 0;
               }
            }

            PDebugTrace("CSeCreate: Slot(%d) = %.*s (halId=%d flags=0x%X)", nSlot, pSePorting->pSlots[nSlot].nNameLength, pSePorting->pSlots[nSlot].pName, pSePorting->pSlots[nSlot].nSlotIdentifier, pSePorting->pSlots[nSlot].nFlags);
         }
      }
      else
      {
         PDebugWarning("CSeCreate: nActiveSlot = 0");
      }

      /* CREATE message has been received */
      bCreateReceived = W_TRUE;
   }

   /* Initialize the pSePorting context */
   pSePorting->pCallback = pCallback;
   pSePorting->pCallbackParameter = pCallbackParameter;
   pSePorting->pRefreshFileList = pRefreshFileList;
   pSePorting->nRefreshFileListLength = nRefreshFileListLength;

   /* save the Socket/File descriptor of the CCClient to be notified */
   pSePorting->nSDCCClient = PUtilConvertPointerToUint( CCClientGetReceptionEvent(pSePorting->pConnection));

   /* Create the thread that processes messages from PCSC server */
   (void)pthread_create(&pSePorting->sThread, NULL, static_thread_CSeMessagesReceiverProc, pSePorting );

   /* Wait for the thread to start */
   static_CSeWaitStartEvent(pSePorting);

   return pSePorting;

error_close_cc_connection:
   CCClientClose(pSePorting->pConnection);

error_close_sd_message_queue:
   close(pSePorting->nSDMessageQueue[0]);
   close(pSePorting->nSDMessageQueue[1]);

error_close_sd_start_stop:
   close(pSePorting->nSDStartStopRequest[0]);
   close(pSePorting->nSDStartStopRequest[1]);

error_destroy_mutex:
   pthread_mutex_destroy(&pSePorting->sMutexSEAccess);

error:
   if(pSePorting != NULL)
   {
      CMemoryFree(pSePorting);
   }
   return NULL;
}

/**********************************************************************************************************************/
/***                                                   ^                                                            ***/
/***                                                   |                                                            ***/
/***                                                   |                                                            ***/
/**********************************************************************************************************************/

/* See HAL Documentation */
void CSeDestroy(
         tCSePorting* pSePorting )
{
   uint32_t nSlot;
   uint32_t nChannelIndex;

   PDebugTrace("CSeDestroy()");

   if (pSePorting != NULL)
   {
      /* Stop the thread that processes messages from PCSC server */
      static_CSeSendStopEvent(pSePorting);
      pthread_join(pSePorting->sThread, NULL);

      close(pSePorting->nSDMessageQueue[0]);
      close(pSePorting->nSDMessageQueue[1]);
      close(pSePorting->nSDStartStopRequest[0]);
      close(pSePorting->nSDStartStopRequest[1]);

      if (pSePorting->pConnection != NULL)
      {
         /* Disconnect from connection center */
         CCClientClose(pSePorting->pConnection);
         pSePorting->pConnection = NULL;
      }

      /* Free data */
      for (nSlot = 0; nSlot < pSePorting->nActiveSlot; nSlot++)
      {
         /* Free saved APDU responses */
         for (nChannelIndex = 0; nChannelIndex < P_SE_MAX_CHANNELS; nChannelIndex++)
         {
            if (pSePorting->pSlots[nSlot].aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer != NULL)
            {
               CMemoryFree(pSePorting->pSlots[nSlot].aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer);
               pSePorting->pSlots[nSlot].aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer = (uint8_t*)NULL;
               pSePorting->pSlots[nSlot].aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength = 0;
            }
         }

         /* Free slot names */
         if (pSePorting->pSlots[nSlot].pName != NULL)
         {
            CMemoryFree(pSePorting->pSlots[nSlot].pName);
            pSePorting->pSlots[nSlot].pName = NULL;
         }
      }

      /* Free slots array */
      if (pSePorting->pSlots != NULL)
      {
         CMemoryFree(pSePorting->pSlots);
         pSePorting->pSlots = NULL;
      }

      pthread_mutex_destroy(&pSePorting->sMutexSEAccess);

      CMemoryFree(pSePorting);

   }
}

/* See HAL Documentation */
bool_t CSeGetStaticInfo(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t* pnFlags,
         uint32_t* pnSwpTimeout,
         uint8_t* pNameBuffer,
         uint32_t nNameBufferLength,
         uint32_t* pnActualNameLength )
{
   tCSePortingSlot * pSlot = NULL;

   PDebugTrace("CSeGetStaticInfo(nSlotIdentifier=%d)", nSlotIdentifier);

   if (pSePorting == NULL)
   {
      PDebugError("CSeGetStaticInfo: Parameter pSePorting == NULL");
      return W_FALSE;
   }

   CDebugAssert(pnFlags != NULL);
   CDebugAssert(pnSwpTimeout != NULL);
   CDebugAssert(pNameBuffer != NULL);
   CDebugAssert(pnActualNameLength != NULL);

   *pnFlags = 0;
   *pnActualNameLength = 0;
   *pnSwpTimeout = 100;

   /* Retrieve the slot index */
   pSlot = static_CSeGetSlot(pSePorting, nSlotIdentifier);

   if(pSlot != NULL)
   {
      /* Copy flags */
      *pnFlags = pSlot->nFlags;

      /* Copy name */
      if (nNameBufferLength > pSlot->nNameLength)
      {
         *pnActualNameLength = pSlot->nNameLength;
      }
      else
      {
         *pnActualNameLength = nNameBufferLength;
      }

      if ((*pnActualNameLength > 0) && (pSlot->pName != NULL))
      {
         CMemoryCopy(pNameBuffer, pSlot->pName, *pnActualNameLength);
      }

      return W_TRUE;
   }
   else
   {
      PDebugTrace("CSeGetStaticInfo: Parameter nSlotIDentifier (%d) is not supported", nSlotIdentifier);
      return W_FALSE;
   }
}

/* See HAL Documentation */
void CSeGetInfo(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint8_t* pAtrBuffer,
         uint32_t nAtrBufferLength )
{
   tCSePortingSlot * pSlot = NULL;
   uint8_t pMessageBuffer[2 * sizeof(uint32_t)];
   uint32_t nOffset = 0;

   PDebugTrace("CSeGetInfo(nSlotIdentifier=%d)", nSlotIdentifier);

   if (pSePorting == NULL)
   {
      PDebugError("CSeGetInfo: Parameter pSePorting == NULL");
      goto return_failure;
   }

   /* Retrieve the slot index */
   pSlot = static_CSeGetSlot(pSePorting, nSlotIdentifier);
   if (pSlot == NULL)
   {
      PDebugError("CSeGetInfo: Parameter nSlotIdentifier unknown");
      goto return_failure;
   }

   /* Build the message to the PCSC server */
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(C_SE_OPERATION_GET_INFO, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nSlotIdentifier, &pMessageBuffer[nOffset]);

   pthread_mutex_lock(&pSePorting->sMutexSEAccess);

   /* Save ATR buffer */
   pSlot->pResponseApduBuffer = pAtrBuffer;
   pSlot->nResponseApduBufferLength = nAtrBufferLength;

   /* Send the message to the PCSC server */
   if (CCClientSendData(pSePorting->pConnection, pMessageBuffer, nOffset) == 0)
   {
      PDebugError("CSeGetInfo: error during CCClientSendData");
      goto return_failure_critical_section;
   }

   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);
   return;

return_failure_critical_section:

   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);

return_failure:
   static_CSeSendMessageCallback(pSePorting, nSlotIdentifier, C_SE_OPERATION_GET_INFO, W_FALSE, 0, 0);
}

/* See HAL Documentation */
void CSeOpenChannel(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nType,
         const uint8_t* pAidBuffer,
         uint32_t nAidLength )
{
   tCSePortingSlot * pSlot = NULL;
   uint8_t pMessageBuffer[5 * sizeof(uint32_t) + 16];
   uint32_t nOffset = 0;

   PDebugTrace("CSeOpenChannel(nSlotIdentifier=%d)", nSlotIdentifier);

   if (pSePorting == NULL)
   {
      PDebugError("CSeOpenChannel: Parameter pSePorting == NULL");
      goto return_failure;
   }

   /* Check AID parameters */
   if ((nAidLength > 0) && (pAidBuffer == NULL))
   {
      PDebugError("CSeOpenChannel: Parameter pAidBuffer == NULL");
      goto return_failure;
   }

   if (nAidLength > 16)
   {
      PDebugError("CSeOpenChannel: Parameter nAidLength > 16");
      goto return_failure;
   }

   /* Retrieve the slot index */
   pSlot = static_CSeGetSlot(pSePorting, nSlotIdentifier);
   if (pSlot == NULL)
   {
      PDebugError("CSeGetInfo: Parameter nSlotIdentifier unknown");
      goto return_failure;
   }

   /* Build the message to the PCSC server */
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(C_SE_OPERATION_OPEN, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nSlotIdentifier, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nSessionIdentifier, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nType, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nAidLength, &pMessageBuffer[nOffset]);
   if (pAidBuffer != NULL)
   {
      CMemoryCopy(&pMessageBuffer[nOffset], pAidBuffer, nAidLength);
      nOffset += nAidLength;
   }

   pthread_mutex_lock(&pSePorting->sMutexSEAccess);

   /* Send the message to the PCSC server */
   if (CCClientSendData(pSePorting->pConnection, pMessageBuffer, nOffset) == 0)
   {
      PDebugError("CSeOpenChannel: error during CCClientSendData");
      goto return_failure_critical_section;
   }

   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);
   return;

return_failure_critical_section:
   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);

return_failure:
   static_CSeSendMessageCallback(pSePorting, nSlotIdentifier, C_SE_OPERATION_OPEN, W_FALSE, P_7816SM_NULL_CHANNEL, 0);
}

/* See HAL Documentation */
void CSeExchangeApdu(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier,
         const uint8_t* pApduBuffer,
         uint32_t nApduLength,
         uint8_t* pResponseApduBuffer,
         uint32_t nResponseApduBufferLength)
{
   tCSePortingSlot * pSlot = NULL;
   uint32_t nChannelIndex;
   uint8_t* pMessageBuffer = (uint8_t*) NULL;
   uint32_t nOffset = 0;

   PDebugTrace("CSeExchangeApdu(nSlotIdentifier=%d)", nSlotIdentifier);

   if (pSePorting == NULL)
   {
      PDebugError("CSeExchangeApdu: Parameter pSePorting == NULL");
      goto return_failure;
   }

   /* Retrieve the slot index */
   pSlot = static_CSeGetSlot(pSePorting, nSlotIdentifier);
   if (pSlot == NULL)
   {
      PDebugError("CSeGetInfo: Parameter nSlotIdentifier unknown");
      goto return_failure;
   }

   /* Allocate a buffer to store the message to send */
   pMessageBuffer = (uint8_t*) CMemoryAlloc(5 * sizeof(uint32_t) + nApduLength);
   if (pMessageBuffer == NULL)
   {
      PDebugError("CSeExchangeApdu: Out of memory");
      goto return_failure;
   }

   /* Build the message to the PCSC server */
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(C_SE_OPERATION_EXCHANGE, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nSlotIdentifier, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nSessionIdentifier, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nChannelIdentifier, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nApduLength, &pMessageBuffer[nOffset]);
   CMemoryCopy(&pMessageBuffer[nOffset], pApduBuffer, nApduLength);
   nOffset += nApduLength;

   pthread_mutex_lock(&pSePorting->sMutexSEAccess);

   /* Retrieve the channel index */
   for (nChannelIndex = 0; nChannelIndex < P_SE_MAX_CHANNELS; nChannelIndex++)
   {
      if (pSlot->aSavedAPDUResponses[nChannelIndex].nChannelIdentifier == nChannelIdentifier)
         break;
   }
   if (nChannelIndex >= P_SE_MAX_CHANNELS)
   {
      PDebugError("CSeExchangeApdu: Parameter nChannelIdentifier unknown");
      goto return_failure_critical_section;
   }

   /* If a previous response has been saved, clear it */
   if (pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer != NULL)
   {
      PDebugWarning("CSeExchangeApdu: Clear the saved response APDU buffer before it was retrieved with CSeGetResponseApdu");
      CMemoryFree(pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer);
      pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer = (uint8_t*)NULL;
      pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength = 0;
   }

   /* Save APDU response buffer */
   pSlot->pResponseApduBuffer = pResponseApduBuffer;
   pSlot->nResponseApduBufferLength = nResponseApduBufferLength;

   /* Send the message to the PCSC server */
   if (CCClientSendData(pSePorting->pConnection, pMessageBuffer, nOffset) == 0)
   {
      PDebugError("CSeExchangeApdu: error during CCClientSendData");
      goto return_failure_critical_section;
   }

   CMemoryFree(pMessageBuffer);
   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);
   return;

return_failure_critical_section:
   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);

return_failure:

   if (pMessageBuffer != NULL)
   {
      CMemoryFree(pMessageBuffer);
   }

   static_CSeSendMessageCallback(pSePorting, nSlotIdentifier, C_SE_OPERATION_EXCHANGE, W_FALSE, nChannelIdentifier, 0);
}

/* See HAL Documentation */
void CSeGetResponseApdu(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier,
         uint8_t* pResponseApduBuffer,
         uint32_t nResponseApduBufferLength,
         uint32_t * pnResponseApduActualSize)
{
   tCSePortingSlot * pSlot = NULL;
   uint32_t nChannelIndex;

   PDebugTrace("CSeGetResponseApdu(nSlotIdentifier=%d)", nSlotIdentifier);

   *pnResponseApduActualSize = 0;

   if (pSePorting == NULL)
   {
      PDebugError("CSeGetResponseApdu: Parameter pSePorting == NULL");
      return;
   }

   /* Retrieve the slot index */
   pSlot = static_CSeGetSlot(pSePorting, nSlotIdentifier);
   if (pSlot == NULL)
   {
      PDebugError("CSeGetInfo: Parameter nSlotIdentifier unknown");
      return;
   }

   pthread_mutex_lock(&pSePorting->sMutexSEAccess);

   /* Retrieve the channel index */
   for (nChannelIndex = 0; nChannelIndex < P_SE_MAX_CHANNELS; nChannelIndex++)
   {
      if (pSlot->aSavedAPDUResponses[nChannelIndex].nChannelIdentifier == nChannelIdentifier)
         break;
   }
   if (nChannelIndex >= P_SE_MAX_CHANNELS)
   {
      PDebugError("CSeGetResponseApdu: Parameter nChannelIdentifier unknown (%d)", nChannelIdentifier);
      goto return_critical_section;
   }

   /* Set the response length */
   *pnResponseApduActualSize = pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength;

   /* Check if data is available */
   if (pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer != NULL)
   {
      CDebugAssert(pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength > 0);

      if (nResponseApduBufferLength < pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength)
      {
         PDebugError("CSeGetResponseApdu: Buffer too short");
         goto return_critical_section;
      }
      else
      {
         /* Copy data */
         CMemoryCopy(pResponseApduBuffer,
                     pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer,
                     pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength);

         /* Release buffer */
         CMemoryFree(pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer);
         pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer = (uint8_t*)NULL;
         pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength = 0;
      }
   }

return_critical_section:
   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);
}

/* See HAL Documentation */
void CSeCloseChannel(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier)
{
   tCSePortingSlot * pSlot = NULL;
   uint32_t nChannelIndex;
   uint8_t pMessageBuffer[4 * sizeof(uint32_t)];
   uint32_t nOffset = 0;

   PDebugTrace("CSeCloseChannel(nSlotIdentifier=%d)", nSlotIdentifier);

   if (pSePorting == NULL)
   {
      PDebugError("CSeCloseChannel: Parameter pSePorting == NULL");
      goto return_failure;
   }

   /* Retrieve the slot index */
   pSlot = static_CSeGetSlot(pSePorting, nSlotIdentifier);
   if (pSlot == NULL)
   {
      PDebugError("CSeGetInfo: Parameter nSlotIdentifier unknown");
      goto return_failure;
   }

   /* Build the message to the PCSC server */
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(C_SE_OPERATION_CLOSE, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nSlotIdentifier, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nSessionIdentifier, &pMessageBuffer[nOffset]);
   nOffset += PUtilWriteUint32ToLittleEndianBuffer(nChannelIdentifier, &pMessageBuffer[nOffset]);

   pthread_mutex_lock(&pSePorting->sMutexSEAccess);

   /* Retrieve the channel index */
   for (nChannelIndex = 0; nChannelIndex < P_SE_MAX_CHANNELS; nChannelIndex++)
   {
      if (pSlot->aSavedAPDUResponses[nChannelIndex].nChannelIdentifier == nChannelIdentifier)
         break;
   }
   if (nChannelIndex >= P_SE_MAX_CHANNELS)
   {
      PDebugError("CSeCloseChannel: Parameter nChannelIdentifier unknown");
      goto return_failure_critical_section;
   }

   /* Clear saved APDU response if needed */
   if (pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer != NULL)
   {
      PDebugWarning("CSeCloseChannel: Clear the saved response APDU buffer before it was retrieved with CSeGetResponseApdu");
      CMemoryFree(pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer);
      pSlot->aSavedAPDUResponses[nChannelIndex].pAPDUResponseBuffer = (uint8_t*)NULL;
      pSlot->aSavedAPDUResponses[nChannelIndex].nResponseAPDUBufferLength = 0;
   }
   /* Clear channel identifier (free this channel) */
   pSlot->aSavedAPDUResponses[nChannelIndex].nChannelIdentifier = 0;

   /* Send the message to the PCSC server */
   if (CCClientSendData(pSePorting->pConnection, pMessageBuffer, nOffset) == 0)
   {
      PDebugError("CSeCloseChannel: error during CCClientSendData");
      goto return_failure_critical_section;
   }

   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);
   return;

return_failure_critical_section:
   pthread_mutex_unlock(&pSePorting->sMutexSEAccess);

return_failure:

   /* Parameter bSuccess must be W_TRUE even in case of failure */
   static_CSeSendMessageCallback(pSePorting, nSlotIdentifier, C_SE_OPERATION_CLOSE, W_TRUE, nChannelIdentifier, 0);
}

void CSeTriggerStkPolling(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier)
{
   /* CSeTriggerStkPolling() cannot be implemented on Windows PC above PC/SC */
   PDebugWarning("CSeTriggerStkPolling(): not implemented on Linux above PC/SC");
}

#else

/* See HAL Documentation */
tCSePorting* CSeCreate(
         tCSeCallback* pCallback,
         void* pCallbackParameter,
         uint8_t* pRefreshFileList,
         uint32_t nRefreshFileListLength)
{
   return NULL;
}

/* See HAL Documentation */
void CSeDestroy(
         tCSePorting* pSePorting )
{
}



/* See HAL Documentation */
bool_t CSeGetStaticInfo(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t* pnFlags,
         uint32_t* pnSwpTimeout,
         uint8_t* pNameBuffer,
         uint32_t nNameBufferLength,
         uint32_t* pnActualNameLength )
{
   return W_FALSE;
}

/* See HAL Documentation */
void CSeGetInfo(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint8_t* pAtrBuffer,
         uint32_t nAtrBufferLength )
{
}

/* See HAL Documentation */
void CSeOpenChannel(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nType,
         const uint8_t* pAidBuffer,
         uint32_t nAidLength )
{
}

/* See HAL Documentation */
void CSeExchangeApdu(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier,
         const uint8_t* pApduBuffer,
         uint32_t nApduLength,
         uint8_t* pResponseApduBuffer,
         uint32_t nResponseApduBufferLength)
{
}

/* See HAL Documentation */
void CSeGetResponseApdu(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier,
         uint8_t* pResponseApduBuffer,
         uint32_t nResponseApduBufferLength,
         uint32_t * pnResponseApduActualSize)
{
}

/* See HAL Documentation */
void CSeCloseChannel(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier)
{
}

void CSeTriggerStkPolling(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier)
{
}

#endif /* #ifdef P_USE_CONNECTION_CENTER */

#endif /* #ifdef P_INCLUDE_SE_SECURITY */

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

