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
   Contains the implementation of the LLCP Service Manager
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( P2P_SCK )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See client API */
W_ERROR PP2PCreateSocket(
   tContext * pContext,
   uint8_t nType,
   const char16_t * pServiceURI,
   uint8_t nSAP,
   W_HANDLE * phSocket
)
{
   uint32_t nURILength = PUtilStringLength(pServiceURI);

   if ((pServiceURI != null) && (nURILength == 0))
   {
      PDebugError("PP2PCreateSocket : zero length URI are not allowed");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nURILength)
   {
      return PP2PCreateSocketDriver(pContext, nType, pServiceURI, (nURILength + 1) * sizeof(char16_t), nSAP, phSocket);
   }
   else
   {
      return PP2PCreateSocketDriver(pContext, nType, null, 0, nSAP, phSocket);
   }
}

W_ERROR PP2PGetSocketParameter(
   tContext * pContext,
   W_HANDLE   hSocket,
   uint32_t   nParameter,
   uint32_t * pnValue)
{

   return PP2PGetSocketParameterDriver(pContext, hSocket, nParameter, pnValue);
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


/**
  * Add a new socket to the P2P socket list
  */

static void static_PP2PRegisterSocket(tContext * pContext, tP2PSocket * pP2PSocket)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);

   pP2PSocket->pNext = pP2PInstance->pFirstSocket;
   pP2PInstance->pFirstSocket = pP2PSocket;
}

/**
  * Remove a socket from the P2P socket list
  */
static void static_PP2PUnregisterSocket(tContext * pContext, tP2PSocket * pP2PSocket)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PSocket * pCurrent, * pLast = null;

   pCurrent = pP2PInstance->pFirstSocket;

   while (pCurrent != null)
   {
      if (pCurrent == pP2PSocket)
      {
         if (pLast == null)
         {
            pP2PInstance->pFirstSocket = pCurrent->pNext;
         }
         else
         {
            pLast->pNext = pCurrent->pNext;
         }

         return;
      }

      pLast = pCurrent;
      pCurrent = pCurrent->pNext;
   }

   if (pCurrent == null)
   {
      PDebugError("static_PP2PUnregisterSocket : unable to find the socket");
   }
}

/**
 * Function called when the user closes the socket handle
 */

static uint32_t static_PP2PDestroySocket(tContext* pContext, void* pObject )
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PSocket * pP2PSocket = pObject;

   /* Free the resources associated to the socket */

   PP2PCleanSocketContext(pContext, pP2PSocket);

   /* update the WKS */
   if (pP2PSocket->sConfig.nLocalSap < 16)
   {
      pP2PInstance->sLLCPInstance.nLocalWKS &= ~(1<< pP2PSocket->sConfig.nLocalSap);
   }

   /* remove the socket from the socket list */
   static_PP2PUnregisterSocket(pContext, pP2PSocket);

   /* free the resources associated to the socket */
   PLLCPSAPFreeSap(&pP2PInstance->sLLCPInstance.sSAPInstance, pP2PSocket->sConfig.nLocalSap);

   if (pP2PSocket->sConfig.bServiceRegistered != W_FALSE)
   {
      PLLCPSDPUnregisterService(&pP2PInstance->sLLCPInstance.sSDPInstance, pP2PSocket->sConfig.nLocalSap);
   }

   if (pP2PSocket->sConfig.pRemoteURI != null)
   {
      CMemoryFree(pP2PSocket->sConfig.pRemoteURI);
   }

   /* remove the socket from the socket list */

   CMemoryFree(pP2PSocket);

   return P_HANDLE_DESTROY_DONE;
}

tHandleType g_sP2PSocket = { static_PP2PDestroySocket, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_P2P_SOCKET  (&g_sP2PSocket)


W_ERROR  PP2PGetSocketObject(tContext * pContext, W_HANDLE hSocket, tP2PSocket ** ppSocket)
{
   return PHandleGetConnectionObject(pContext, hSocket, P_HANDLE_TYPE_P2P_SOCKET, (void **) ppSocket);
}

/* See header */
W_ERROR PP2PCreateSocketWrapper(
   tContext    * pContext,
   uint8_t       nType,
   const char16_t * pUTF16ServiceURI,
   uint32_t      nUTF16ServiceURISize,
   uint8_t       nSAP,
   W_HANDLE   * phSocket)
{
   return PP2PCreateSocketDriver(pContext, nType, pUTF16ServiceURI, nUTF16ServiceURISize, nSAP, phSocket);
}

/* See WP2PCreateSocket */
W_ERROR PP2PCreateSocketDriver(
   tContext    * pContext,
   uint8_t       nType,
   const char16_t * pUTF16ServiceURI,
   uint32_t      nUTF16ServiceURISize,
   uint8_t       nSAP,
   W_HANDLE   * phSocket
)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tNFCControllerInfo * pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   tP2PSocket * pP2PSocket = null;
   W_ERROR  nError;

   uint32_t nUTF8ServiceURILength = 0;
   uint8_t * pUTF8ServiceURI = null;

   PDebugTrace("PP2PCreateSocketDriver");

   /* some init */

   if (phSocket != null)
   {
      * phSocket = W_NULL_HANDLE;
   }

   /* check the NFC controller is P2P capable */
   if (((pNFCControllerInfo->nProtocolCapabilities & W_NFCC_PROTOCOL_READER_P2P_INITIATOR) == 0) ||
       ((pNFCControllerInfo->nProtocolCapabilities & W_NFCC_PROTOCOL_CARD_P2P_TARGET) == 0))
   {
      PDebugError("PP2PCreateSocketDriver : NFC controller is not P2P capable");
      nError = W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
      goto error;
   }

   if (phSocket == null)
   {
      PDebugError("PP2PCreateSocketDriver phSocket must not be null");
      nError = W_ERROR_BAD_PARAMETER;
      goto error;
   }

   if ((nSAP != 0) && ((nSAP < 2) || (nSAP > 63)))
   {
      PDebugError("PP2PCreateSocketDriver: nSap value must be 0, of in [2-63]");
      nError = W_ERROR_BAD_PARAMETER;
      goto error;
   }

   if (nUTF16ServiceURISize != 0)
   {
      nUTF8ServiceURILength = PUtilConvertUTF16ToUTF8(null, pUTF16ServiceURI, nUTF16ServiceURISize / 2);

      if (nUTF8ServiceURILength > LLCP_MAX_SN_LENGTH)
      {
         /* the URI encoded in Utf-8 does not fit in the Service Name parameter */
         PDebugError("PP2PCreateSocketDriver : pServiceURI too long");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      pUTF8ServiceURI = CMemoryAlloc(nUTF8ServiceURILength);

      if (pUTF8ServiceURI == null)
      {
         PDebugError("PP2PCreateSocketDriver : no resource to convert URI in UTF8");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;
      }

      PUtilConvertUTF16ToUTF8(pUTF8ServiceURI, pUTF16ServiceURI, nUTF16ServiceURISize / 2);
   }

   switch (nType)
   {
      case W_P2P_TYPE_CLIENT :

         if ((nSAP != 0) && (pUTF8ServiceURI != null))
         {
            PDebugError("PP2PCreateSocketDriver: nSAP != 0 and pServiceURI != null invalid for client sockets");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }

         break;

      case W_P2P_TYPE_SERVER :
      case W_P2P_TYPE_CONNECTIONLESS :

         if ((nSAP == 0) && (pUTF8ServiceURI == null))
         {
            PDebugError("PP2PCreateSocketDriver: nSAP == 0 and pServiceURI == null invalid for server and connectionless sockets");
            nError = W_ERROR_BAD_PARAMETER;
            goto error;
         }
         break;

      case W_P2P_TYPE_CLIENT_SERVER:
         break;

      default:
         PDebugError("PP2PCreateSocketDriver: invalid socket type !!!");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
   }

   pP2PSocket = CMemoryAlloc(sizeof(* pP2PSocket));

   if (pP2PSocket == null)
   {
      /* unable to allocate memory for the socket */
      PDebugError("PP2PCreateSocketDriver: unable to memory for the socket !!!");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   CMemoryFill(pP2PSocket, 0, sizeof(*pP2PSocket));

   pP2PSocket->sConfig.nType = nType;

   pP2PSocket->sConfig.nLocalMIU = LLCP_DEFAULT_MIU;
   pP2PSocket->sConfig.nLocalRW =  LLCP_PARAM_RW_DEFAULT;

   if ((nType == W_P2P_TYPE_SERVER) || (nType == W_P2P_TYPE_CLIENT_SERVER) || (nType == W_P2P_TYPE_CONNECTIONLESS))
   {

      /* force URI / SAP for well-known services */

      if (PLLCPSDPCheckURIAndSAPConsistency(&pP2PInstance->sLLCPInstance.sSDPInstance, pUTF8ServiceURI, nUTF8ServiceURILength, nSAP) == W_FALSE)
      {
         PDebugError("PP2PCreateSocketDriver : inconsistency between SAP and URI value");
         nError = W_ERROR_BAD_PARAMETER;
         goto error;
      }

      if ((pUTF8ServiceURI != null) && (nSAP == 0))
      {
         uint8_t nWellKnownSAP;

         nWellKnownSAP = PLLCPSDPGetWellKnownServiceSAP(&pP2PInstance->sLLCPInstance.sSDPInstance, pUTF8ServiceURI, nUTF8ServiceURILength);

         if (nWellKnownSAP != 0)
         {
            PDebugTrace("PP2PCreateSocketDriver : force SAP value to %d", nWellKnownSAP);
            nSAP = nWellKnownSAP;
         }
      }
      else if ((nSAP != 0) && (pUTF8ServiceURI == null))
      {
         uint32_t nWellKownURILength = PLLCPSDPGetWellKnownServiceURI(&pP2PInstance->sLLCPInstance.sSDPInstance, nSAP, null);

         if (nWellKownURILength != 0)
         {
            pUTF8ServiceURI = CMemoryAlloc(nWellKownURILength);

            if (pUTF8ServiceURI == null)
            {
               PDebugError("PP2PCreateSocketDriver : out of resource");
               nError = W_ERROR_OUT_OF_RESOURCE;
               goto error;
            }

            nUTF8ServiceURILength = nWellKownURILength;

            PLLCPSDPGetWellKnownServiceURI(&pP2PInstance->sLLCPInstance.sSDPInstance, nSAP, pUTF8ServiceURI);

            PDebugTrace("PP2PCreateSocketDriver : force URI value to ");
            PDebugTraceBuffer(pUTF8ServiceURI, nWellKownURILength);
         }
      }


      if (nSAP != 0)
      {
         /* user specified a local SAP, check it is available */

         if ((pP2PSocket->sConfig.nLocalSap = PLLCPSAPAllocSap(&pP2PInstance->sLLCPInstance.sSAPInstance, SAP_FIXED_VALUE, nSAP)) == 0)
         {
            /* This SAP value is already in use */
            PDebugError("PP2PCreateSocketDriver: specified SAP is already in use!!!");
            nError = W_ERROR_EXCLUSIVE_REJECTED;
            goto error;
         }
      }
      else
      {
         /* no local SAP provided, allocate a new one */

         if ((pP2PSocket->sConfig.nLocalSap = PLLCPSAPAllocSap(&pP2PInstance->sLLCPInstance.sSAPInstance, SAP_DYNAMIC_SERVER, 0)) == 0)
         {
            /* unable to allocate a SAP for the socket !!! */
            PDebugError("PP2PCreateSocketDriver: unable to allocate local SAP !!!");
            nError = W_ERROR_OUT_OF_RESOURCE;
            goto error;
         }
      }

      if (pUTF8ServiceURI != null)
      {
         /* user specified a service name, check it is available */

         if (PLLCPSDPLookupServiceFromURI(&pP2PInstance->sLLCPInstance.sSDPInstance, pUTF8ServiceURI, nUTF8ServiceURILength) != 0)
         {
            /* This URI has already been registered ! */
            PDebugError("PP2PCreateSocketDriver: specified URI is already in use!!!");
            nError = W_ERROR_EXCLUSIVE_REJECTED;
            goto error;
         }

         nError = PLLCPSDPRegisterService(&pP2PInstance->sLLCPInstance.sSDPInstance,  pP2PSocket->sConfig.nLocalSap, pUTF8ServiceURI, nUTF8ServiceURILength);

         if (nError != W_SUCCESS)
         {
            /* unable to register the service URI */
            PDebugError("PP2PCreateSocketDriver: unable to register the service URI!!!");
            goto error;
         }

         pP2PSocket->sConfig.bServiceRegistered = W_TRUE;
      }

      /* ok, all is fine */
   }
   else
   {
      /* W_P2P_TYPE_CLIENT */

      if ((pP2PSocket->sConfig.nLocalSap = PLLCPSAPAllocSap(&pP2PInstance->sLLCPInstance.sSAPInstance, SAP_DYNAMIC_CLIENT, 0)) == 0)
      {
         /* unable to allocate a SAP for the socket !!! */
         PDebugError("PP2PCreateSocketDriver: unable to allocate local SAP !!!");
         nError = W_ERROR_OUT_OF_RESOURCE;
         goto error;

         /* ok, all is fine */
      }
   }

   if ((nType == W_P2P_TYPE_CLIENT) || (nType == W_P2P_TYPE_CLIENT_SERVER))
   {
      pP2PSocket->sConfig.nRemoteSAP = nSAP;
      pP2PSocket->sConfig.pRemoteURI = pUTF8ServiceURI;
      pP2PSocket->sConfig.nRemoteURILength = nUTF8ServiceURILength;

      /* do not free the remote URI now ! */
      pUTF8ServiceURI = null;
   }

   /* register the socket handle */

   nError = PHandleRegister(pContext, pP2PSocket, P_HANDLE_TYPE_P2P_SOCKET, phSocket);

   if (nError != W_SUCCESS)
   {
      PDebugError("PP2PCreateSocketDriver : unable to register handle");
      goto error;
   }

   if (pUTF8ServiceURI)
   {
      CMemoryFree(pUTF8ServiceURI);
   }

   pP2PSocket->sConfig.hSocket = * phSocket;

   /* update the WKS */

   if (pP2PSocket->sConfig.nLocalSap < 16)
   {
      pP2PInstance->sLLCPInstance.nLocalWKS |= (1 << pP2PSocket->sConfig.nLocalSap);
   }

   /* add the socket to the socket list */
   static_PP2PRegisterSocket(pContext, pP2PSocket);

   return W_SUCCESS;

error:

   PDebugError("PP2PCreateSocketDriver fails : %s", PUtilTraceError(nError));

   if (pP2PSocket != 0)
   {
      if (pP2PSocket->sConfig.nLocalSap != 0)
      {
         PLLCPSAPFreeSap(&pP2PInstance->sLLCPInstance.sSAPInstance, pP2PSocket->sConfig.nLocalSap);

         if (pP2PSocket->sConfig.bServiceRegistered != W_FALSE)
         {
            PLLCPSDPUnregisterService(&pP2PInstance->sLLCPInstance.sSDPInstance, pP2PSocket->sConfig.nLocalSap);
         }
      }

      CMemoryFree(pP2PSocket);
   }

   if (pUTF8ServiceURI)
   {
      CMemoryFree(pUTF8ServiceURI);
   }

   return nError;
}


/** See client API */
W_ERROR PP2PSetSocketParameter(
   tContext * pContext,
   W_HANDLE   hSocket,
   uint32_t   nParameter,
   uint32_t   nValue)
{
   tP2PSocket * pP2PSocket = null;

   W_ERROR      nError;

   /* Retreive the P2P socket object */
   nError = PP2PGetSocketObject(pContext, hSocket, & pP2PSocket);

   if (nError != W_SUCCESS)
   {
      return nError;
   }

   switch (nParameter)
   {
      case W_P2P_LOCAL_MIU:

         if (pP2PSocket->sConfig.nType != W_P2P_TYPE_CONNECTIONLESS)
         {
            if ((nValue >= 128) && (nValue <= 256))
            {
               pP2PSocket->sConfig.nLocalMIU = (uint16_t) nValue;
               nError = W_SUCCESS;
            }
            else
            {
               nError = W_ERROR_BAD_PARAMETER;
            }
         }
         else
         {
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }
         break;

      case W_P2P_LOCAL_RW:

         if (pP2PSocket->sConfig.nType != W_P2P_TYPE_CONNECTIONLESS)
         {
            if (nValue <= 15)
            {
               pP2PSocket->sConfig.nLocalRW = (uint8_t) nValue;
               nError = W_SUCCESS;
            }
            else
            {
               nError = W_ERROR_BAD_PARAMETER;
            }
         }
         else
         {
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }
         break;

      default:
         nError = W_ERROR_BAD_PARAMETER;
         break;
   }

   return nError;
}



/** See client API */
W_ERROR PP2PGetSocketParameterDriver(
   tContext * pContext,
   W_HANDLE   hSocket,
   uint32_t   nParameter,
   uint32_t * pnValue)
{
   tP2PSocket * pP2PSocket = null;
   W_ERROR      nError;

   /* Retreive the P2P socket object */
   nError = PP2PGetSocketObject(pContext, hSocket, & pP2PSocket);

   if (nError != W_SUCCESS)
   {
      return nError;
   }

   if (pnValue == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   switch (nParameter)
   {
      case W_P2P_LOCAL_MIU :

         if (pP2PSocket->sConfig.nType != W_P2P_TYPE_CONNECTIONLESS)
         {
            * pnValue = pP2PSocket->sConfig.nLocalMIU;
            nError = W_SUCCESS;
         }
         else
         {
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }
         break;

      case W_P2P_LOCAL_RW :

         if (pP2PSocket->sConfig.nType != W_P2P_TYPE_CONNECTIONLESS)
         {
            * pnValue = pP2PSocket->sConfig.nLocalRW;
            nError = W_SUCCESS;
         }
         else
         {
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }
         break;

      case W_P2P_CONNECTION_ESTABLISHED:

         if (pP2PSocket->sConfig.nType != W_P2P_TYPE_CONNECTIONLESS)
         {
            * pnValue = pP2PSocket->sConnection.bIsConnected;
            nError = W_SUCCESS;
         }
         else
         {
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }
         break;

      case W_P2P_DATA_AVAILABLE:

         if (pP2PSocket->pFirstRecvPDU != null)
         {
            * pnValue = W_TRUE;
         }
         else
         {
            * pnValue = W_FALSE;
         }
         break;


      case W_P2P_LOCAL_SAP :

         * pnValue = pP2PSocket->sConfig.nLocalSap;
         break;

      case W_P2P_REMOTE_SAP :
      case W_P2P_REMOTE_MIU :
      case W_P2P_REMOTE_RW :
      case W_P2P_IS_SERVER :

         if (pP2PSocket->sConfig.nType != W_P2P_TYPE_CONNECTIONLESS)
         {
            if (pP2PSocket->sConnection.bIsConnected != W_FALSE)
            {
               switch (nParameter)
               {
                  case W_P2P_REMOTE_SAP :

                     * pnValue = pP2PSocket->sConnection.pConnection->nRemoteSAP;
                     break;

                  case W_P2P_REMOTE_RW :

                     * pnValue = pP2PSocket->sConnection.pConnection->nRWR;
                     break;

                  case W_P2P_REMOTE_MIU :
                     * pnValue = pP2PSocket->sConnection.pConnection->nMIUR;
                     break;

                  case W_P2P_IS_SERVER :
                     * pnValue = pP2PSocket->sConnection.bIsServer;
                     break;
               }

               nError = W_SUCCESS;
            }
            else
            {
               nError = W_ERROR_BAD_STATE;
            }
         }
         else
         {
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }
         break;

      default:
         nError = W_ERROR_BAD_PARAMETER;
         break;
   }

   return nError;
}

/* See header */
void PP2PCallSocketReadAndWriteCallbacks(
   tContext     * pContext,
   tP2PSocket   * pP2PSocket,
   W_ERROR        nError)
{
   PDebugTrace("static_PP2PCallReadAndWriteCallbacks nError %d", nError);

   /* call the receive callback */
   if (pP2PSocket->bRecvInProgress)
   {
      pP2PSocket->bRecvInProgress = W_FALSE;
      pP2PSocket->pRecvBuffer = null;

      if (pP2PSocket->hRecvOperation != W_NULL_HANDLE)
      {
         PBasicSetOperationCompleted(pContext, pP2PSocket->hRecvOperation);
         pP2PSocket->hRecvOperation = W_NULL_HANDLE;
      }

      PDFCPostContext3(&pP2PSocket->sRecvCC, 0, nError);
   }

   /* call the send callback */
   if (pP2PSocket->bXmitInProgress)
   {
      pP2PSocket->bXmitInProgress = W_FALSE;
      pP2PSocket->pXmitBuffer = null;

      if (pP2PSocket->hXmitOperation != W_NULL_HANDLE)
      {
         PBasicSetOperationCompleted(pContext, pP2PSocket->hXmitOperation);
         pP2PSocket->hXmitOperation = W_NULL_HANDLE;
      }

      PDFCPostContext2(&pP2PSocket->sXmitCC, nError);
   }
}

/* See header */
void PP2PCallSocketWriteCallback(
   tContext         * pContext,
   tP2PSocket       * pP2PSocket,
   W_ERROR            nError)
{
   PDebugTrace("PP2PDataConfirmationCallback %08x - %p %s", pP2PSocket->sConfig.hSocket, pP2PSocket, PUtilTraceError(nError));

   pP2PSocket->bXmitInProgress = W_FALSE;
   pP2PSocket->pXmitBuffer = null;
   pP2PSocket->nXmitBufferLength = 0;

   if (pP2PSocket->hXmitOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, pP2PSocket->hXmitOperation);
   }

   PDFCPostContext2(&pP2PSocket->sXmitCC, nError);
}

/* See header */
void PP2PCallSocketReadCallback(
   tContext    * pContext,
   tP2PSocket  * pP2PSocket,
   uint32_t      nRecvLength,
   W_ERROR       nError,
   uint8_t       nSAP)
{
   PDebugTrace("PP2PCallSocketReadCallback %08x - %p %s", pP2PSocket->sConfig.hSocket, pP2PSocket, PUtilTraceError(nError));

   pP2PSocket->bRecvInProgress = W_FALSE;
   pP2PSocket->pRecvBuffer = null;
   pP2PSocket->nRecvBufferLength = 0;

   if (pP2PSocket->hRecvOperation != W_NULL_HANDLE)
   {
      PBasicSetOperationCompleted(pContext, pP2PSocket->hRecvOperation);
      pP2PSocket->hRecvOperation = W_NULL_HANDLE;
   }

   if (pP2PSocket->sConfig.nType == W_P2P_TYPE_CONNECTIONLESS)
   {
      PDFCPostContext4(&pP2PSocket->sRecvCC, nRecvLength, nError, nSAP);
   }
   else
   {
      PDFCPostContext3(&pP2PSocket->sRecvCC, nRecvLength, nError);
   }
}

/* see header */
void  PP2PCallAllSocketReadAndWriteCallbacks(
   tContext * pContext,
   W_ERROR nError)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PSocket * pP2PSocket;

   for (pP2PSocket = pP2PInstance->pFirstSocket; pP2PSocket != null; pP2PSocket=pP2PSocket->pNext)
   {
      PP2PCallSocketReadAndWriteCallbacks(pContext, pP2PSocket, nError);
   }
}

void PP2PCleanSocketContext(
   tContext * pContext,
   tP2PSocket  * pP2PSocket)
{
   tLLCPPDUHeader * pPDU;
   tLLCPConnection * pConnection;

   PDebugTrace("PP2PCleanSocketContext");

   pP2PSocket->bXmitInProgress = W_FALSE;
   pP2PSocket->hXmitOperation = W_NULL_HANDLE;
   pP2PSocket->pXmitBuffer = null;
   pP2PSocket->nXmitBufferLength = 0;

   pP2PSocket->bRecvInProgress = W_FALSE;
   pP2PSocket->hRecvOperation = W_NULL_HANDLE;
   pP2PSocket->pRecvBuffer = null;
   pP2PSocket->nRecvBufferLength = 0;

   while ((pP2PSocket->pFirstRecvPDU) != null)
   {
      pPDU = pP2PSocket->pFirstRecvPDU->pNext;

      PLLCPFreePDU(pContext, pP2PSocket->pFirstRecvPDU);
      pP2PSocket->pFirstRecvPDU = pPDU;
   }

   pP2PSocket->pLastRecvPDU = null;
   pP2PSocket->nRecvPDU = 0;

   /* Set pPendingConnection to null so that the checks in PLLCPConnectionFreeContext successs */
   pConnection = pP2PSocket->sConnection.pPendingConnection;
   pP2PSocket->sConnection.pPendingConnection = null;

   if ((pConnection != null) && (pConnection != pP2PSocket->sConnection.pConnection))
   {
      PLLCPConnectionFreeContext(pContext, pConnection);
   }

   /* Set pConnection to null so that the checks in PLLCPConnectionFreeContext successs */
   pConnection = pP2PSocket->sConnection.pConnection;
   pP2PSocket->sConnection.pConnection = null;

   if (pConnection != null)
   {
      PLLCPConnectionFreeContext(pContext, pConnection);
   }

   pP2PSocket->sConnection.bConnectInProgress = W_FALSE;
   pP2PSocket->sConnection.bIsConnected = W_FALSE;
   pP2PSocket->sConnection.bIsServer = W_FALSE;
   pP2PSocket->sConnection.bShutdownInProgress = W_FALSE;
}

void PP2PCleanAllSocketContexts(tContext * pContext)
{
   tP2PInstance * pP2PInstance = PContextGetP2PInstance(pContext);
   tP2PSocket * pP2PSocket;

   for (pP2PSocket = pP2PInstance->pFirstSocket; pP2PSocket != null; pP2PSocket=pP2PSocket->pNext)
   {
      PP2PCleanSocketContext(pContext, pP2PSocket);
   }


}



#endif   /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

