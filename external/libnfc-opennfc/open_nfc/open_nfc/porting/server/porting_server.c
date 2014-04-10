/*
 * Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
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

  Implementation of the linux server porting

*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( SERV )

#include "porting_config.h"
#include "porting_os.h"
#include "open_nfc.h"
#include "porting_driver.h"

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <errno.h>


/* function call request descriptor */
typedef struct __tDFCCLientServerMessageHeader
{
   /* input parameter size */
   uint32_t nSizeIn;

   /* output parameter size */
   uint32_t nSizeOut;

   /* function identifier */
   uint8_t nCode;

} tDFCCLientServerMessageHeader;

/* client buffer synchronization request descriptor */
typedef struct __tDFCCLientServerSyncOutputData
{
   /* address of the client buffer to be synchronized */
   void* pClientBuffer;

   /* length of the client buffer */
   uint32_t nClientBufferLength;

} tDFCCLientServerSyncOutputData;


struct __tUserIdentity {
  
  int   socket;
  uid_t uid;
  uid_t gid;
};

/* See porting guide */
W_ERROR CServerRead(
            const tUserIdentity* pUserIdentity,
            void* pBuffer,
            uint32_t nBufferLength)
{
   int socket = pUserIdentity->socket;

   int32_t nOffset = 0;
   int32_t res;

   while(nBufferLength != 0)
   {
      res = recv(socket, &((char*)pBuffer)[nOffset], nBufferLength, MSG_NOSIGNAL);
      if(res < 0)
      {
         PDebugError("CServerRead :recv() failed : errno %d", errno);
         return W_ERROR_CLIENT_SERVER_COMMUNICATION;
      }
      else if(res == 0)
      {
         /* Socket gracefully shutdown */
         PDebugError("CServerRead :recv()=0 : shutdown");
         return W_ERROR_CLIENT_SERVER_COMMUNICATION;
      }
         nOffset += res;
         nBufferLength -= res;
   }

   return W_SUCCESS;
}

/* See porting guide */
W_ERROR CServerWrite(
            const tUserIdentity* pUserIdentity,
            const void* pBuffer,
            uint32_t nBufferLength)
{
   int socket = pUserIdentity->socket;
   int32_t res;
   int32_t nOffset = 0;

   while(nBufferLength != 0)
   {
      res = send(socket, &((char*)pBuffer)[nOffset], nBufferLength, MSG_NOSIGNAL);

      if(res < 0)
      {
         PDebugError("CServerRead :send() failed : errno %d", errno);
         return W_ERROR_CLIENT_SERVER_COMMUNICATION;
      }
      nOffset += res;
      nBufferLength -= res;
   }

   return W_SUCCESS;
}

extern tDriverInstance* g_pDriverInstance;

/**
 * Processes the incoming messages sent by the client
 *
 */
W_ERROR CServerLoop(
               const tUserIdentity* pUserIdentity,
               tDriverInstance* pDriverInstance)
{
   W_ERROR nError;
   tUserInstance* pUserInstance;
   tDFCCLientServerMessageHeader sHeader;
   P_SYNC_SEMAPHORE * phSemaphore;

   struct msghdr  msg;
   struct cmsghdr * cmsg;
   char   control[CMSG_SPACE(sizeof(int))];
   int * fdptr;
   struct iovec iov;

   uint8_t aParameterBuffer[256]; /* 256 bytes allocated in the stack for each client thread */

   /* create the driver instance */
   pUserInstance = PDriverOpen( pDriverInstance, pUserIdentity );

   if(pUserInstance == null)
   {
      PDebugError("CServerLoop: Error returned by PDriverOpen()");
      return W_ERROR_BAD_PARAMETER;
   }

   /* retrieve the socket pair used to trigger WBasicPumpEvent() in client */

   phSemaphore = PDriverGetUserSemaphoreHandle(pUserInstance);

   if ((phSemaphore->sockets[0] == -1) || (phSemaphore->sockets[1] == -1))
   {
      PDebugError("CServerLoop : the socket pair used for semaphore failed");
      return W_ERROR_OUT_OF_RESOURCE;
   }

   /* use SCM_RIGTHS to give access of the socket to client */
   memset(&msg, 0, sizeof(msg));

   iov.iov_base = "OPENNFC";
   iov.iov_len  = 8;
   msg.msg_iov = &iov;
   msg.msg_iovlen = 1;
   msg.msg_control = control;
   msg.msg_controllen = sizeof(control);

   cmsg = CMSG_FIRSTHDR(&msg);
   cmsg->cmsg_level = SOL_SOCKET;
   cmsg->cmsg_type = SCM_RIGHTS;
   cmsg->cmsg_len = CMSG_LEN(sizeof(int));
   fdptr =( int *) CMSG_DATA(cmsg);
   memcpy(fdptr, &phSemaphore->sockets[1], sizeof(int));

   msg.msg_controllen = cmsg->cmsg_len;

   if (sendmsg(pUserIdentity->socket, &msg, 0) < 0)
   {

      PDebugError("CServerLoop: sendmsg() failed");
      return W_ERROR_CLIENT_SERVER_COMMUNICATION;
   }

   for(;;)
   {
      nError = CServerRead(pUserIdentity,
         &sHeader, sizeof(tDFCCLientServerMessageHeader));
      if(nError != W_SUCCESS)
      {
         PDebugError("CServerLoop: Error %d returned by CServerRead()", nError);
         break;
      }
      if((sHeader.nSizeIn > 256) || (sHeader.nSizeOut > 256))
      {
         PDebugError("CServerLoop: Invalid function code or invalid parameter size sent by the client");
         nError = W_ERROR_CLIENT_SERVER_PROTOCOL;
         break;
      }
      if(sHeader.nSizeIn > 0)
      {
         nError = CServerRead(pUserIdentity,
            &aParameterBuffer, sHeader.nSizeIn);
         if(nError != W_SUCCESS)
         {
            PDebugError("CServerLoop: Error %d returned by CServerRead()", nError);
            break;
         }
      }

      nError = PDriverIoctl(pUserInstance, sHeader.nCode, aParameterBuffer);
      if(nError != W_SUCCESS)
      {
         PDebugError("CServerLoop: Error %d returned by PDriverIoctl()", nError);
         break;
      }

      nError = CServerWrite(pUserIdentity, &sHeader.nCode, 1);
      if(nError != W_SUCCESS)
      {
        PDebugError("CServerLoop: Error %d returned by CServerWrite()", nError);
        break;
      }

      if(sHeader.nSizeOut > 0)
      {
         nError = CServerWrite(pUserIdentity,
            &aParameterBuffer, sHeader.nSizeOut);
         if(nError != W_SUCCESS)
         {
            PDebugError("CServerLoop: Error %d returned by CServerWrite()", nError);
            break;
         }
      }
   }

   PDriverPreClose( pUserInstance );

   PDriverClose( pUserInstance );

   return nError;
}

/* See porting guide */
W_ERROR CServerCopyToClientBuffer(
            const tUserIdentity* pUserIdentity,
            const void * pUserBufferAddress,
            const void * pKernelBuffer,
            uint32_t nBufferLength)

{
   tDFCCLientServerSyncOutputData sSyncOutpuData;
   uint8_t nCode = (uint8_t) P_Identifier_PSyncOutputData;
   W_ERROR nError = CServerWrite(pUserIdentity, &nCode, 1);
   if(nError != W_SUCCESS)
   {
      PDebugError("static_DFCDriverCopyToUserBuffer: Error %d returned by CServerWrite", nError);
      return nError;
   }

   sSyncOutpuData.pClientBuffer = (void *) pUserBufferAddress;
   sSyncOutpuData.nClientBufferLength = nBufferLength;

   nError = CServerWrite(pUserIdentity, &sSyncOutpuData, sizeof(tDFCCLientServerSyncOutputData));

   if(nError != W_SUCCESS)
   {
      PDebugError("static_DFCDriverCopyToUserBuffer: Error %d returned by CServerWrite", nError);
      return nError;
   }

   nError = CServerWrite(pUserIdentity, pKernelBuffer, nBufferLength);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_DFCDriverCopyToUserBuffer: Error %d returned by CServerWrite", nError);
      return nError;
   }

   return W_SUCCESS;
}

/* EOF */
