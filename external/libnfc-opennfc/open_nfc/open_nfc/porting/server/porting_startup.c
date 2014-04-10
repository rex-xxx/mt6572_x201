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

  Implementation of the server porting procedures

*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( STARTUP )

#include "open_nfc.h"
#include "porting_os.h"
#include "nfc_hal.h"
#include "porting_driver.h"

#include "porting_startup.h"


#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>

typedef struct __tOpenNFCServerInstance {

	tNALBinding  			 * pNALBinding;
	tDriverInstance		 * pDriverInstance;

	sem_t							hBootSemaphore;							/* Semaphore used to syncrhonize with the end of the NFCC boot sequence */
	/* event management */
	sem_t							hEventSemaphore;							/* semaphore used to trigger a call to PDriverProcessEvent */
	pthread_t					hProcessEventThreadId;					/* thread calling PDriverProcessEvent() */
	bool_t 							bProcessEventThreadStop;				/* flag to request the termination of the thread */

	/* client management */
	int 							nServerSocket;								/* socket waiting for client sonnections */
	pthread_t					hClientConnectionsThreadId;			/* thread processing the client connections */

	void *						pPortingConfig;

	int						   aWakeUpSockets[2];						/* socket pair use to force wake up of threads blocked in select() */

   int                     nTotalClient;                        /* total number of client created from the start of the stack */
   fd_set                  client_fds;

} tOpenNFCServerInstance;


struct __tUserIdentity {

  int   socket;
  uid_t uid;
  uid_t gid;
};

static tOpenNFCServerInstance g_Instance;

/*
 * Thread dealing with the call of PDriverProcessEvents
 */

void * static_PDriverProcessEventsThread(void * lpParameter)
{
   PDebugTrace("static_PDriverProcessEventsThread started");
	
	while (g_Instance.bProcessEventThreadStop == W_FALSE)
	{
		if (sem_wait(&g_Instance.hEventSemaphore))
		{
         PDebugError("static_PDriverProcessEventsThread : sem_wait() failed");
			break;
		}

		PDriverProcessEvents(g_Instance.pDriverInstance);
	}

   PDebugTrace("static_PDriverProcessEventsThread exiting");
   
	return NULL;
}

void StopPDriverProcessEventsThread(void * unused)
{	
	g_Instance.bProcessEventThreadStop = W_TRUE;
	
	sem_post(&g_Instance.hEventSemaphore);	
	
	pthread_join(g_Instance.hProcessEventThreadId, NULL);
	PDebugTrace("static_PDriverProcessEventsThread stopped");
}

/*
 * See Functional Specifications Document
 */
void CSyncTriggerEventPump ( void * pConfig )
{
	if (sem_post(&g_Instance.hEventSemaphore))
	{
		PDebugError("PSyncTriggerEventPump : sem_post failed %d", errno);
	}
}

/*
 * For the Linux porting, 2 different security authentications are implemented:
 *   One authentication is based on the authentication data provided by the user application.
 *   The other authentication relies on the UID-GID value for the user application.
 *
 * Data Based Authentication
 * -------------------------
 *  - The user application calls the function WSecurityAuthenticate()
 *    with a non-empty buffer containing the principal data.
 *  - The authentication data created by the HAL includes a copy of this buffer
 *  - The authentication function of the HAL is a comparison of the buffer
 *    content with the principal value.
 *
 * This authentication is used by a framwork on top of the C API, Android for example.
 * An application cannot perform the authentication directly. The framework performs
 * the authentication for the application with the hash of the certificate
 * used the sign the application code.
 *
 *
 * UID-GID Based Authentication
 * -------------------------
 *  - The user application calls the function WSecurityAuthenticate()
 *    with an empty buffer.
 *  - The authentication data created by the HAL includes the UID-GID specified
 *    in the User identity structure.
 *    The User identity structure is computed by the porting layer iin a way that cannot be
 *    conterfeated by the calling application.
 *  - The authentication function of the HAL is a comparison of the UID-GID values
 *    with the principal value.
 *
 * The format of the principal value is the following:
 * - A buffer of 20 bytes with the bytes [0 .. 11] set to 0xFF.
 * - If the bytes [12..15] are not set to 0xFF, they represent the GID value to compare.
 * - If the bytes [16..19] are not set to 0xFF, they represent the UID value to compare.
 *
 * At least one of the GID or UID value must be set in the buffer.
 */

#define P_PRINCIPAL_GID_POS     12
#define P_PRINCIPAL_UID_POS     16
#define P_PRINCIPAL_LENGTH      20

/** The authentication data structure for Linux */
struct __tUserAuthenticationData
{
   uint8_t* pApplicationDataBuffer;
   uint32_t nApplicationDataBufferLength;
};

/**
 * Checks if a principal is encoding a GID-UID
 *
 * @param[in]   pBuffer  The principal buffer.
 *
 * @param[in]   nBufferLength  The principal buffer length.
 *
 * @return  1 if the principal is encodinf a GID-UID, 0 otherwise.
 **/
static uint32_t static_CSecurityIsGidUidPrincipal(
            const uint8_t* pBuffer,
            uint32_t nBufferLength)
{
   uint32_t i;

   if((nBufferLength != P_PRINCIPAL_LENGTH) || (pBuffer == null))
   {
      return 0;
   }

   for(i = 0; i < P_PRINCIPAL_GID_POS; i++)
   {
      if(pBuffer[i] != 0xFF)
      {
         return 0;
      }
   }

   return 1;
}

/* See HAL specification */
bool_t CSecurityCreateAuthenticationData(
            const tUserIdentity* pUserIdentity,
            const uint8_t* pApplicationDataBuffer,
            uint32_t nApplicationDataBufferLength,
            const uint8_t** ppAuthenticationData,
            uint32_t* pnAuthenticationDataLength)
{
   uint8_t* pAuthenticationData = null;

   *ppAuthenticationData = (uint8_t*)null;
   *pnAuthenticationDataLength = 0;

   if(((nApplicationDataBufferLength == 0) && (pApplicationDataBuffer != null))
   || ((nApplicationDataBufferLength != 0) && (pApplicationDataBuffer == null)))
   {
      return W_FALSE;
   }

   if(nApplicationDataBufferLength != 0)
   {
      uint32_t nPos;

      if((nApplicationDataBufferLength % P_PRINCIPAL_LENGTH) != 0)
      {
         return W_FALSE;
      }

      /* Check that the value given by the application is not encoding a GID-UID principal */
      for(nPos = 0; nPos < nApplicationDataBufferLength; nPos += P_PRINCIPAL_LENGTH)
      {
         if(static_CSecurityIsGidUidPrincipal(&pApplicationDataBuffer[nPos], P_PRINCIPAL_LENGTH) != 0)
         {
         return W_FALSE;
         }
      }
   }

   if(nApplicationDataBufferLength != 0)
   {
      pAuthenticationData = (uint8_t*)CMemoryAlloc(nApplicationDataBufferLength);
      if(pAuthenticationData == null)
      {
         return W_FALSE;
      }

      CMemoryCopy(pAuthenticationData,
         pApplicationDataBuffer, nApplicationDataBufferLength);
   }


   *ppAuthenticationData = pAuthenticationData;
   *pnAuthenticationDataLength = nApplicationDataBufferLength;

   return W_TRUE;
}

/* See HAL specification */
void CSecurityDestroyAuthenticationData(
            const uint8_t* pAuthenticationData,
            uint32_t nAuthenticationData)
{
   if(pAuthenticationData != null)
   {
      CMemoryFree((void*)pAuthenticationData);
   }
}

/* Example of default principal */
/*
static const uint8_t g_aDefaultPrincipalProprietary1_A[] = { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x10,0x11,0x12,0x13,0x14 };
static const uint8_t g_aDefaultPrincipalProprietary1_B[] = { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x10,0x11,0x12,0x13,0x14 };
static const tCSecurityDefaultPrincipal g_aDefaultPrincipalListProprietary1[] = {
   {
      g_aDefaultPrincipalProprietary1_A,
      sizeof(g_aDefaultPrincipalProprietary1_A)
   },
   {
      g_aDefaultPrincipalProprietary1_B,
      sizeof(g_aDefaultPrincipalProprietary1_B)
   }
};

static const uint8_t g_aDefaultPrincipalSwp1[] = { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x10,0x11,0x12,0x13,0x14 };
static const tCSecurityDefaultPrincipal g_aDefaultPrincipalListSwp1[] = {
   {
      g_aDefaultPrincipalSwp1,
      sizeof(g_aDefaultPrincipalSwp1)
   }
};
*/
/* See HAL specification */
bool_t CSecurityGetConfiguration(
            uint32_t nSlotIdentifier,
            const tCSecurityDefaultPrincipal** ppDefaultPrincipalList,
            uint32_t* pnDefaultPrincipalNumber)
{
   *pnDefaultPrincipalNumber = 0;
   *ppDefaultPrincipalList = null;

#if 0
   /* Se default principal */
   if(nSlotIdentifier == C_SE_SLOT_ID_PROPRIETARY_1)
   {
      /* Security stack activated for SE #1 with a the default principal */
      *pnDefaultPrincipalNumber = sizeof(g_aDefaultPrincipalListProprietary1) / sizeof(tCSecurityDefaultPrincipal*);
      *ppDefaultPrincipalList = g_aDefaultPrincipalListProprietary1;
      return W_TRUE;
   }


   else if(nSlotIdentifier == C_SE_SLOT_ID_SWP_1)
   {
      /* Security stack activated for SWP #1 with a the default principal */
      *pnDefaultPrincipalNumber = sizeof(g_aDefaultPrincipalListSwp1) / sizeof(tCSecurityDefaultPrincipal*);
      *ppDefaultPrincipalList = g_aDefaultPrincipalListSwp1;
   return W_TRUE;
}
   else if(nSlotIdentifier == C_SE_SLOT_ID_SWP_2)
   {
      /* Security stack activated for SWP #2 but the default principal is not defined */
      return W_TRUE;
   }

   /* Security stack not activated for the other SE */

   return W_FALSE;
#endif

   return W_TRUE;
}
/* See HAL specification */
bool_t CSecurityGetIdentityData(
            const tUserIdentity* pUserIdentity,
            uint8_t* pUserIdentityBuffer,
            uint32_t nUserIdentityBufferLength,
            uint32_t* pnActualLength)
{
   if(pnActualLength == null)
   {
      return W_FALSE;
   }

   if(pUserIdentity != null)
   {
      if(pUserIdentityBuffer != null)
      {
         if(nUserIdentityBufferLength < sizeof(tUserIdentity))
         {
            return W_FALSE;
         }

         CMemoryCopy(pUserIdentityBuffer, pUserIdentity, sizeof(tUserIdentity));
      }

      *pnActualLength = sizeof(tUserIdentity);
   }
   else
   {
      pnActualLength = 0;
   }

   return W_TRUE;
}

/* See HAL specification */
bool_t CSecurityCheckIdentity(
            uint32_t nSlotIdentifier,
            const tUserIdentity* pUserIdentity,
            const uint8_t* pAuthenticationData,
            uint32_t nAuthenticationDataLength,
            const uint8_t* pPrincipalBuffer,
            uint32_t nPrincipalBufferLength)
{
   if((pAuthenticationData == null)
   || (pPrincipalBuffer == null)
   || (nPrincipalBufferLength != P_PRINCIPAL_LENGTH)
   || ((nAuthenticationDataLength % P_PRINCIPAL_LENGTH) != 0))
   {
      return W_FALSE;
   }

   if(static_CSecurityIsGidUidPrincipal(pPrincipalBuffer, nPrincipalBufferLength) != 0)
   {
      /* GID-UID case */
      uid_t uid;
      gid_t gid;
      uint8_t * pos = (uint8_t *) &pPrincipalBuffer[P_PRINCIPAL_GID_POS];

      gid = (gid_t)(
                         ((*pos) & 0xFF)
                    |    (((*(pos+1)) & 0xFF) << 8)
                    |    (((*(pos+2)) & 0xFF) << 16)
                    |    (((*(pos+3)) & 0xFF) << 24)
                  );
                  
                  pos+=4;

      uid = (uid_t)(
                      ((*pos) & 0xFF)
                  |    (((*(pos+1)) & 0xFF) << 8)
                  |    (((*(pos+2)) & 0xFF) << 16)
                  |    (((*(pos+3)) & 0xFF) << 24)
              );
              
              pos+=4;

      if(((gid != (gid_t)-1) && (gid != pUserIdentity->gid))
      || ((uid != (uid_t)-1) && (uid != pUserIdentity->uid)))
      {
         return W_FALSE;
      }

   return W_TRUE;
   }
   else
   {
      while(nAuthenticationDataLength != 0)
      {
         if(CMemoryCompare(pAuthenticationData, pPrincipalBuffer, P_PRINCIPAL_LENGTH) == 0)
         {
            return W_TRUE;
         }

         nAuthenticationDataLength -= P_PRINCIPAL_LENGTH;
         pAuthenticationData += P_PRINCIPAL_LENGTH;
      }

      return W_FALSE;
   }
}

/* See HAL specification */
bool_t CSecurityCheckImpersonatedIdentity(
            uint32_t nSlotIdentifier,
            const tUserIdentity* pUserIdentity,
            const uint8_t* pAuthenticationData,
            uint32_t nAuthenticationDataLength,
            const uint8_t* pPrincipalBuffer,
            uint32_t nPrincipalBufferLength,
            const uint8_t* pImpersonationDataBuffer,
            uint32_t nImpersonationDataBufferLength)
{
   if((pAuthenticationData == null)
   || (pPrincipalBuffer == null)
   || (nPrincipalBufferLength == 0))
   {
      return W_FALSE;
   }

   if((nImpersonationDataBufferLength % P_PRINCIPAL_LENGTH) != 0)
   {
      return W_FALSE;
   }

   /* User data case */
   if(nPrincipalBufferLength != P_PRINCIPAL_LENGTH)
   {
      return W_FALSE;
   }

   while(nImpersonationDataBufferLength != 0)
   {
      if(CMemoryCompare(pImpersonationDataBuffer, pPrincipalBuffer, P_PRINCIPAL_LENGTH) == 0)
      {
         return W_TRUE;
      }

      nImpersonationDataBufferLength -= P_PRINCIPAL_LENGTH;
      pImpersonationDataBuffer += P_PRINCIPAL_LENGTH;
   }

   return W_FALSE;
}
/*
 *  PDriverCreate() callback function
 */

static void static_PDriverCreateCompletionCallback( void* pPortingConfig, uint32_t nMode)
{
	if (sem_post(&g_Instance.hBootSemaphore))
	{
		PDebugError("static_PDriverCreateCompletionCallback : sem_post failed %d", errno);
	}
}

/*
 * Initializes the NFCC controller
 */

void * StartNFCC(tNALBinding * pBinding, void * pPortingConfig)
{

	bool_t bSuccess = W_FALSE;

   /* Reset the g_Instance */
   
   memset(& g_Instance, 0 , sizeof(g_Instance));

	/* initializes the semaphore used for the boot sequence */

	if (sem_init(&g_Instance.hBootSemaphore, 0, 0))
	{
		PDebugError(" BootNFCC : sem_init(hBootSemaphore) failed : errno %d", errno);
		goto return_function;
	}

	/* initializes the semaphore used for the event processing */

	if (sem_init(&g_Instance.hEventSemaphore, 0, 0))
	{
		PDebugError(" BootNFCC : sem_init(hEventSemaphore) failed : errno %d", errno);
		goto return_function;
	}

	/* initialize the socket pair used for thread wake-up */

	if (socketpair(AF_UNIX, SOCK_STREAM, 0, g_Instance.aWakeUpSockets))
	{
		PDebugError(" BootNFCC : socketpair() failed : errno %d", errno);
		goto return_function;
	}

	g_Instance.pNALBinding = pBinding;

	/* create the driver instance */

	g_Instance.pDriverInstance = PDriverCreate(static_PDriverCreateCompletionCallback , pPortingConfig, W_FALSE, (void *) g_Instance.pNALBinding, sizeof(* g_Instance.pNALBinding));

	if( g_Instance.pDriverInstance == NULL )
	{
		PDebugError(" BootNFCC : PDriverCreate() failed");
		goto return_function;
	}

	/* starts the event processing thread */

	g_Instance.bProcessEventThreadStop = W_FALSE;

   if (pthread_create(&g_Instance.hProcessEventThreadId, NULL, static_PDriverProcessEventsThread, NULL) != 0)
	{
		PDebugError(" BootNFCC : : pthread_create(PDriverProcessEventsThread) failed");
		goto return_function;
	}
   g_Instance.pPortingConfig = pPortingConfig;

	/* all is fine... */

	bSuccess = W_TRUE;

return_function:

	if (bSuccess == W_FALSE)
	{

		StopNFCC(&g_Instance);

		return NULL;
	}

	else
	{
		return &g_Instance;
	}
}


/*
 * Waits until the NFCC boot has been completed
 */

int WaitForNFCCBoot(void * unused)
{
	if (sem_wait(&g_Instance.hBootSemaphore))
	{
		PDebugError("WaitForNFCCBoot : : sem_wait(hBootSemaphore) failed %d", errno);

		return -1;
	}

	return 0;
}


W_ERROR CServerLoop(const tUserIdentity* pUserIdentity, tDriverInstance* pDriverInstance);

/*
 * Thread dealing with the reception of data from the client
 */

static void * static_ServerLoopThread( void * lpParameter )
{
   const tUserIdentity* pUserIdentity = (const tUserIdentity*)lpParameter;


   W_ERROR nError;

   PDebugTrace("static_ServerLoopThread (user:%d group:%d) started",
      pUserIdentity->uid, pUserIdentity->gid);

   nError = CServerLoop(pUserIdentity, g_Instance.pDriverInstance);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_ServerLoopThread: PServerLoop(user:%d group:%d) returned %d",
         pUserIdentity->uid, pUserIdentity->gid, nError);
   }
   else
   {
      PDebugTrace("static_ServerLoopThread: Returning from PServerLoop(user:%d group:%d)",
         pUserIdentity->uid, pUserIdentity->gid);
   }

   close(pUserIdentity->socket);

   FD_CLR(pUserIdentity->socket, &g_Instance.client_fds);
   free((void*)pUserIdentity);

   PDebugTrace("static_ServerLoopThread exiting");
   sem_post(& g_Instance.hBootSemaphore);
   
   return (void *) nError;
}

/*
 * Thread dealing with processing of client connections
 */

#define min(a, b)       ((a) < (b) ? (a) : (b))
#define max(a, b)       ((a) < (b) ? (b) : (a))

static void * static_AcceptClientConnectionsThread(void * lpParameter)
{
	PDebugTrace("static_AcceptClientConnectionsThread started");

	int nSocket;

	for(;;)
	{
		pthread_t thread_id;
		fd_set readfds;
      int nfds;
      tUserIdentity* pUserIdentity;

		FD_ZERO(&readfds);
		FD_SET(g_Instance.nServerSocket, &readfds);
		FD_SET(g_Instance.aWakeUpSockets[0], &readfds);

      nfds = max(g_Instance.nServerSocket,g_Instance.aWakeUpSockets[0]) +1;

      if (select(nfds, &readfds, NULL, NULL, NULL) < 0)
		{
			PDebugError("static_AcceptClientConnectionsThread : select() failed");
			goto end;
		}

		if (FD_ISSET(g_Instance.aWakeUpSockets[0], &readfds))
		{
			goto end;
		}

		if ((nSocket = accept( g_Instance.nServerSocket, NULL, NULL )) == -1)
		{
			/* accept failed, this typically occurs during server shutdown (listen socket has been closed) */
			PDebugError("static_AcceptClientConnectionsThread : accept() failed");
			goto end;
		}

      {
        int on = 1;

        if (setsockopt(nSocket, SOL_SOCKET, SO_PASSCRED, &on, sizeof(on)) == -1)
        {
            PDebugError("Error: Cannot request credential passing");
            goto end;
        }

      }

		PDebugTrace("Processing client connection");

		/* create the tread dealing with communication with the client */
      pUserIdentity = (tUserIdentity*)malloc(sizeof(tUserIdentity));

      memset(pUserIdentity, 0, sizeof(tUserIdentity));

      if(pUserIdentity == NULL)
      {
          PDebugError("Error: Cannot allocate client structure\n");
          close(nSocket);
      }
      else
      {
          char data[32], control[32];
          struct msghdr   msg;
          struct cmsghdr  *cmsg;
          struct iovec    iov;

          pUserIdentity->socket = nSocket;

          /* get the credential sent by the client */

          memset(&msg, 0, sizeof(msg));
          iov.iov_base   = data;
          iov.iov_len    = sizeof(data)-1;
          msg.msg_iov    = &iov;
          msg.msg_iovlen = 1;
          msg.msg_control = control;
          msg.msg_controllen = sizeof(control);

          if (recvmsg(pUserIdentity->socket, &msg, 0) < 0)
          {
              PDebugError("Error: Cannot receive credential message");
              close(nSocket);
              free(pUserIdentity);
              continue;
          }

          if (strcmp(data, "OPENNFC"))
          {
              PDebugError("Error : unexpected message format %s", data);
              close(nSocket);
              free(pUserIdentity);
              continue;
          }

          /* Loop over all control messages */
          cmsg = CMSG_FIRSTHDR(&msg);

          while (cmsg != NULL)
          {
              if ((cmsg->cmsg_level == SOL_SOCKET)  && (cmsg->cmsg_type  == SCM_CREDENTIALS))
              {
                struct ucred * credptr = (struct ucred *) CMSG_DATA(cmsg);
                pUserIdentity->uid = credptr->uid;
                pUserIdentity->gid = credptr->gid;

                PDebugError("Client credentials UID %08X, GID %08X\n", pUserIdentity->uid, pUserIdentity->gid);
                break;
              }
          }

          if (pthread_create(&thread_id, NULL, static_ServerLoopThread, pUserIdentity) != 0)
          {
              PDebugError("Error: Cannot create client thread! \n");
              close(nSocket);
              free(pUserIdentity);
              continue;
          }
          pthread_detach(thread_id);          
                    
          /* all is fine, add the new client to the list */
          FD_SET(nSocket, &g_Instance.client_fds);
          g_Instance.nTotalClient++;
      }
   }

end:
   PDebugTrace("static_AcceptClientConnectionsThread exiting");

	return NULL;
}

/*
 * creates a server socket waiting for client connection and starts
 * the thread dealing with client connections
 */

int StartAcceptingClientConnections(void * unused)
{
	int nRetVal = -1;

	struct sockaddr_un sSockAddr;

	g_Instance.nServerSocket = socket(AF_UNIX, SOCK_STREAM, 0);

	if (g_Instance.nServerSocket == -1)
	{
		PDebugError("Error: Cannot create the listen socket");
		goto return_function;
	}

	CMemoryFill(&sSockAddr, 0, sizeof(sSockAddr));

	sSockAddr.sun_family = AF_UNIX;
	strcpy(&sSockAddr.sun_path[1], "opennfc");

	if (bind( g_Instance.nServerSocket, (struct sockaddr*) &sSockAddr, sizeof(sSockAddr)) == -1)
	{
		PDebugError("Error: Cannot bind the listen socket");
		goto return_function;
	}

	if (listen( g_Instance.nServerSocket, 5 ) == -1)
	{
		PDebugError("Error: Error returned by listen()");
		goto return_function;
	}

	g_Instance.hClientConnectionsThreadId = 0;

	if (pthread_create(&g_Instance.hClientConnectionsThreadId, NULL, static_AcceptClientConnectionsThread, NULL) != 0)
	{
      PDebugError("pthread_create(static_AcceptClientConnectionsThread) failed");
		goto return_function;
	}

	nRetVal = 0;

return_function:

	if (nRetVal)
	{
		if (g_Instance.nServerSocket != -1)
		{
			close(g_Instance.nServerSocket);
			g_Instance.nServerSocket = -1;
		}
	}

	return nRetVal;
}


int StopAcceptingClientConnections(void * unused)
{
   ssize_t nResult;

	if (g_Instance.nServerSocket != -1)
	{
		close(g_Instance.nServerSocket);
		g_Instance.nServerSocket = -1;
	}

	/* write into the aWakeUpSockets to force select() exit */

   nResult = write(g_Instance.aWakeUpSockets[1], "K", 1);
	if (nResult <= 0)
	{
		PDebugError("%s: write error %d", __FUNCTION__, (int) nResult);
	}

	pthread_join(g_Instance.hClientConnectionsThreadId, NULL);
   PDebugTrace("static_AcceptClientConnectionsThread stopped");

	return 0;
}


int CloseClientConnections(void * unused)
{
   int i;
         
   for (i=0; i<1024; i++)
   {
      if (FD_ISSET(i, & g_Instance.client_fds))
   {
         shutdown(i, SHUT_RDWR);
         close(i);
      }
   }
	   
   for (i=0; i<g_Instance.nTotalClient; i++)
   {
	   sem_wait(& g_Instance.hBootSemaphore);
   }
    
	return 0;
}


int StopNFCC(void * unused)
{
   /* Stop processing driver events */
   StopPDriverProcessEventsThread(null);       
   
   /* No longer accept new clients */
   StopAcceptingClientConnections(null); 
   
   /* close all client connections */
   CloseClientConnections(null);   
   /* Terminate the NFC HAL event loop */
   StopNALEventLoop();

   if (g_Instance.pDriverInstance != NULL)
   {
      PDriverDestroy(g_Instance.pDriverInstance);
      g_Instance.pDriverInstance = NULL;
   }

   close(g_Instance.aWakeUpSockets[0]);
   close(g_Instance.aWakeUpSockets[1]);

	return 0;
}
