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

#define P_MODULE  P_MODULE_DEC( HAL )

#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>

#include <arpa/inet.h>

#include "nal_porting_os.h"
#include "nal_porting_hal.h"
#include "linux_porting_hal.h"

#include "nfc_hal.h"
#include "ccclient.h"

#define TIMER_SET		'S'
#define	TIMER_CANCEL		'C'
#define EVENT_QUIT		'Q'
#define	EVENT_KICK		'K'

extern tNALBinding * const g_pNALBinding;

/* The NFC controller instance */

struct __tNALInstance
{
   /* The timer instance  */

   struct __tNALTimerInstance
   {
      uint32_t nTimerValue;       /* The current timer expiration value, 0 if no timer is pending */
      bool_t       bIsExpired;        /* The expiration flag */
      bool_t       bIsInitialized;    /* The initalization flag */

   } sTimerInstance;

   /* The com instance */

   #define P_MAX_RX_BUFFER_SIZE  300
   #define P_COM_RESET_FLAG  0x00
   #define P_COM_DATA_FLAG   0x01

   struct __tNALComInstance
   {
      void    * pNFCCConnection;      /* the connection, returned by CC Client */

      uint8_t aRXBuffer[P_MAX_RX_BUFFER_SIZE];

      uint32_t nRXDataLength;
      uint8_t* pRXData;

      bool_t       bInitialResetDone;   /* first CNALResetNFCController() call  done ? */
      uint32_t nResetPending;         /* number of CNALResetNFCController() calls waiting for confirmation */

   } sComInstance;

   int aWakeUpSockets[2];            /* sockets used to force select() exit */

   pthread_t hProcessDriverEventsThread;

} g_sNFCCInstance;


/* Retuns the pointer on the single instance */
static __inline tNALInstance* static_PNALGetInstance( void )
{
   return &g_sNFCCInstance;
}

/* Retuns the pointer on the single instance */
static __inline tNALComInstance* static_PComGetInstance( void )
{
   return &g_sNFCCInstance.sComInstance;
}

/* Retuns the pointer on the single instance */
static __inline tNALTimerInstance* static_PTimerGetInstance( void )
{
   return &g_sNFCCInstance.sTimerInstance;
}


/*******************************************************************************

  Timer Functions.

*******************************************************************************/
/* See Functional Specifications Document */
tNALTimerInstance * CNALTimerCreate( void* pPortingConfig )
{
   tNALTimerInstance * pTimer = static_PTimerGetInstance();

   if( pTimer != NULL )
   {
      pTimer->nTimerValue = 0L;
      pTimer->bIsExpired = W_FALSE;
      pTimer->bIsInitialized = W_TRUE;
   }

   return pTimer;
}


/* See Functional Specifications Document */
void CNALTimerDestroy(tNALTimerInstance * pTimer )
{
   if( pTimer != NULL )
   {
      CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );
      pTimer->bIsInitialized = W_FALSE;
   }
}

/* See Functional Specifications Document */
uint32_t CNALTimerGetCurrentTime( tNALTimerInstance* pTimer )
{
   uint32_t nCurrentTime = 0;

   CNALDebugAssert(pTimer != NULL);

   if( pTimer != NULL )
   {
      struct timespec tv;

      CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );

      if (clock_gettime(CLOCK_MONOTONIC, &tv) == 0)
      {
         nCurrentTime = tv.tv_sec * 1000 + tv.tv_nsec / 1000000;
      }
      else
      {
         PNALDebugError("CNALTimerGetCurrentTime : clock_gettime() failed %d", errno);
      }
   }

   return nCurrentTime;
}

/* See Functional Specifications Document */
void CNALTimerSet( tNALTimerInstance* pTimer, uint32_t nTimeoutMs )
{
   CNALDebugAssert(pTimer != NULL);
	uint8_t buff[1];

   if( pTimer != NULL )
   {
      CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );

      pTimer->nTimerValue = CNALTimerGetCurrentTime(pTimer) + nTimeoutMs;
      pTimer->bIsExpired = W_FALSE;

      /* write into the fd to force wakeup of the receive thread */
		buff[0] = TIMER_SET;
      write(g_sNFCCInstance.aWakeUpSockets[0], buff, 1);
   }
}

/* See Functional Specifications Document */
bool_t CNALTimerIsTimerElapsed( tNALTimerInstance* pTimer )
{
   bool_t bIsExpired = W_FALSE;

   CNALDebugAssert(pTimer != NULL);

   if( pTimer != NULL )
   {
      CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );

      bIsExpired = pTimer->bIsExpired;
      pTimer->bIsExpired = W_FALSE;
   }

   return bIsExpired;
}

/* See Functional Specifications Document */
void CNALTimerCancel( tNALTimerInstance* pTimer )
{
   CNALDebugAssert(pTimer != NULL);
	uint8_t buff[1];

   if( pTimer != NULL )
   {
      CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );
      pTimer->nTimerValue = 0L;
      pTimer->bIsExpired = W_FALSE;

		buff[0] = TIMER_CANCEL;
      write(g_sNFCCInstance.aWakeUpSockets[0], buff, 1);
   }
}


/*
 * Marks the current timer as expired.
 */
static void static_CNALTimerSetExpired( tNALTimerInstance* pTimer)
{
   CNALDebugAssert(pTimer != NULL);

   if (pTimer != NULL)
   {
      CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );

      pTimer->nTimerValue = 0L;
      pTimer->bIsExpired = W_TRUE;
   }
}


/*******************************************************************************

  Communication Port Functions

*******************************************************************************/


static bool_t static_PComReadData(tNALComInstance* pComPort)
{
   int32_t nResult;

   if(pComPort->pNFCCConnection == NULL)
   {
      return W_FALSE;
   }

   if( pComPort->nRXDataLength == 0 )
   {
      /* No more DATA => Restart Overlapped Reception */
      nResult = CCClientReceiveData(
               pComPort->pNFCCConnection,
               pComPort->aRXBuffer, P_MAX_RX_BUFFER_SIZE,
               &pComPort->pRXData,
		W_FALSE);

      if( nResult < 0 )
      {
         PNALDebugWarning("An error occured during the read operation on the communication port");
         CNALComDestroy(pComPort);
         return W_FALSE;
      }
      else
      {
         if(nResult > 0)
         {

				pComPort->nRXDataLength = nResult;
#if 0
            if(pComPort->bInitialResetDone != W_FALSE)
            {
               if(pComPort->pRXData[0] == 0) /* Answer to reset? */
               {
                  PNALDebugTrace("Reset confirmation received");

                  if(pComPort->nResetPending > 0)
                  {
                     pComPort->nResetPending--;
                  }
                  else
                  {
                     PNALDebugWarning("An error occured in the protocol with the NFCC");
                  }
               }
               else
               {
                  if(pComPort->nResetPending == 0)
                  {
                     pComPort->nRXDataLength = (uint32_t)(nResult - 1);
                     pComPort->pRXData++;
                  }
                  else
                  {
                     PNALDebugWarning("discarding data received prior reset acknowledge");
                  }
               }
            }
            else
            {
            PNALDebugWarning("discarding data received prior initial reset");
            }
#endif
         }
      }

   }

   return W_TRUE;
}


const char16_t uri[] = { 'c','c',':','n','f','c','c','_','h','a', 'l', 0 };
#define CC_I2C_PROTOCOL_VERSION_10     0x10
static uint8_t g_nCCI2cProtocolVersions[] = { CC_I2C_PROTOCOL_VERSION_10 };
static char address[16];

/* See Functional Specifications Document */
tNALComInstance* CNALComCreate(
               void* pPortingConfig,
               uint32_t* pnType )
{
   tNALComInstance* pComPort = static_PComGetInstance();
   uint32_t nError;
	uint32_t addr;
uint8_t  nAgreedVersion;

	addr = inet_addr((char *) pPortingConfig);
  sprintf(address, "%d", addr);

  CCClientSetAddress((char *) pPortingConfig);
    
    nError = CCClientOpen(uri, W_FALSE, g_nCCI2cProtocolVersions, sizeof(g_nCCI2cProtocolVersions), &nAgreedVersion, &pComPort->pNFCCConnection);
	
   if ((pComPort->pNFCCConnection == NULL) && (nError == CC_ERROR_NO_PROVIDER))
   {
      PNALDebugTrace("Caution: The NFC Controller Service is not started\n");
      PNALDebugTrace("Waiting for Service Provider connection ... Press Ctrl-C to abort\n");

      nError = CCClientOpen(uri, W_TRUE, g_nCCI2cProtocolVersions, sizeof(g_nCCI2cProtocolVersions), &nAgreedVersion, &pComPort->pNFCCConnection);
   }

   if(pComPort->pNFCCConnection == NULL)
   {
      char16_t aServiceName[51];

      /* Check the connection type */
      if(CCClientGetProtocol(uri, aServiceName, 51) == W_FALSE)
      {
         PNALDebugWarning("Error: Invalid configuration string\n");
         return W_FALSE;
      }

      PNALDebugWarning( "=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+\n" );
      PNALDebugWarning( "CANNOT CONNECT TO THE CONNECTION CENTER.\n" );

      switch( nError )
      {
         case CC_ERROR_URI_SYNTAX:
            PNALDebugWarning( "SYMPTOM: Syntax error in URI parameter\n" );
            break;

         case CC_ERROR_CONNECTION_FAILURE:
            PNALDebugWarning( "SYMPTOM: Impossible to connect to the Connection Center\n" );
            PNALDebugWarning( "DIAGNOSTIC: Connection Center not started, firewall, ...\n"
                     "           Check Connection Center and your network configuration.\n");
            break;

         case CC_ERROR_PROVIDER_BUSY:
            PNALDebugWarning ("SYMPTOM: Service Provider is busy\n");
            PNALDebugWarning( "DIAGNOSTIC: another client uses the service provider;\n"
                    "            check the Service List in the Connection Center\n" );
            break;

         default:

            PNALDebugWarning( "SYMPTOM: Internal error while operating on TCP/IP Socket %d", nError  );
            break;
      }

      PNALDebugTrace( "=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+\n" );

      goto return_error;
   }

   pComPort->nRXDataLength = 0;
#ifndef HCI_SWP
   pComPort->bInitialResetDone = W_FALSE;
#else
   pComPort->bInitialResetDone = W_TRUE;
#endif /*HCI_SWP*/
   pComPort->nResetPending = 0;


   *pnType = P_COM_TYPE_NFC_HAL_TCPIP;

   return pComPort;

return_error:

   *pnType = 0;

   return NULL;
}


/* See Functional Specifications Document */
void CNALComDestroy(
         tNALComInstance* pComPort )
{
   if(pComPort != NULL)
   {
      CCClientClose(pComPort->pNFCCConnection);
      pComPort->pNFCCConnection = NULL;
   }
}


/* See Functional Specifications Document */
uint32_t CNALComReadBytes(
         tNALComInstance* pComPort,
         uint8_t* pReadBuffer,
         uint32_t nBufferLength)
{
   if(pComPort->nRXDataLength != 0)
   {
      if(nBufferLength <= pComPort->nRXDataLength)
      {
         CNALMemoryCopy(pReadBuffer, pComPort->pRXData, nBufferLength);
         pComPort->nRXDataLength -= nBufferLength;
         pComPort->pRXData += nBufferLength;
      }
      else
      {
         CNALMemoryCopy(pReadBuffer, pComPort->pRXData, pComPort->nRXDataLength);
         nBufferLength = pComPort->nRXDataLength;
         pComPort->nRXDataLength = 0;
      }
   }
   else
   {
      nBufferLength = 0;
   }

   return nBufferLength;
}

/* See Functional Specifications Document */
uint32_t CNALComWriteBytes(
         tNALComInstance* pComPort,
         uint8_t* pBuffer,
         uint32_t nBufferLength )
{
   if(pComPort->pNFCCConnection != 0)
   {
      if(CCClientSendDataEx(pComPort->pNFCCConnection,
         P_COM_DATA_FLAG, pBuffer, nBufferLength) != 0)
      {
         return nBufferLength;
      }
   }

   return 0;
}

/*******************************************************************************

  Reset Functions

*******************************************************************************/

/* See Functional Specifications Document */
void CNALResetNFCController(
         void* pPortingConfig,
         uint32_t nResetType )
{
   tNALComInstance* pComPort = static_PComGetInstance();
   uint8_t nValue = (uint8_t)nResetType;

	PNALDebugLog("*****************  Reseting NFC Controller *********************");

	pComPort->bInitialResetDone = W_TRUE;
	pComPort->nResetPending++;

   if(CCClientSendDataEx(pComPort->pNFCCConnection, P_COM_RESET_FLAG, &nValue, 1) == 0)
   {
      PNALDebugWarning("Error returned by CCClientSendData()\n");

		pComPort->bInitialResetDone = W_FALSE;
      pComPort->nResetPending--;
   }
}

/*
 * Thread dealing with the call of poll function of the NAL binding
 */

void * PDriverReceiveThread(void * arg)
{
   tNALInstance   * pNALInstance = static_PNALGetInstance();
   tNALComInstance    * pComPort = static_PComGetInstance();
   tNALTimerInstance  * pTimer = static_PTimerGetInstance();

   fd_set             readfds;
   int               data_fd, ctrl_fd, max_fd;

   uint32_t nTimeoutMs;
   struct timeval   tv;
   int res;
   uint8_t buffer[256];

   data_fd = (int)CCClientGetReceptionEvent(pComPort->pNFCCConnection);
   ctrl_fd = pNALInstance->aWakeUpSockets[1];


   if (data_fd > ctrl_fd)
   {
      max_fd = data_fd + 1;
   }
   else
   {
      max_fd = ctrl_fd + 1;
   }

   for (;;)
   {
		bool_t bCallPoll = W_FALSE;

      FD_ZERO(&readfds);
      FD_SET(data_fd, &readfds);
      FD_SET(ctrl_fd, &readfds);

      if (pTimer->nTimerValue != 0)
      {
         uint32_t nCurrentTime = CNALTimerGetCurrentTime(pTimer);

         if (pTimer->nTimerValue > nCurrentTime)
         {
            nTimeoutMs = pTimer->nTimerValue - nCurrentTime;

            tv.tv_sec = nTimeoutMs / 1000;
            tv.tv_usec = (nTimeoutMs - tv.tv_sec * 1000) * 1000;
            res = select(max_fd, &readfds, NULL, NULL, &tv);

         }
         else
         {
            res = 0;
         }
      }
      else
      {
         res = select(max_fd, &readfds, NULL, NULL, NULL);
      }

      if (res < 0)
      {
         if (errno != EINTR)
         {
            PNALDebugError("PDriverReceiveThread : select failed : errno %d\n", errno);
            pthread_exit((void *) -1);
         }
      }
      else if (res > 0)
      {
			if (FD_ISSET(data_fd, &readfds))
         {
            /* some data has been received */
            static_PComReadData(pComPort);
				bCallPoll = W_TRUE;
         }
			else
			{
				/* process data received on the control socket */
				int ret, i;

				ret = recv(ctrl_fd, buffer, sizeof(buffer), 0);

            if (((ret < 0) && (errno != EINTR)) || (ret == 0))
            {
               /* something goes wrong with the control socket, this typically occurs when exitin the service */

               pthread_exit((void *) -1);
            }


				for (i=0; i<ret; i++)
				{
					switch (buffer[i])
					{
						case EVENT_QUIT :
							pthread_exit((void *) -1);
							break;

						case EVENT_KICK :
							bCallPoll= W_TRUE;
							break;
					}
				}
			}
      }
      else
      {
         /* select returned due to a timeout */
         static_CNALTimerSetExpired(pTimer);
			bCallPoll = W_TRUE;
      }

      if (bCallPoll != W_FALSE)
		{
			do
			{

				g_pNALBinding->pPollFunction(arg);

			} while(pComPort->nRXDataLength != 0);
		}
   }
}



void CNALSyncTriggerEventPump(
            void* pPortingConfig )
{
	tNALInstance * pNALInstance = static_PNALGetInstance();
	uint8_t buff[1];

	buff[0] = EVENT_KICK;
	write(pNALInstance->aWakeUpSockets[0], buff, 1);
}

/* See Functional Specification Document */
tNALInstance * CNALPreCreate(void * pPortingConfig)
{
   tNALInstance * pNALInstance = static_PNALGetInstance();

	CNALMemoryFill(pNALInstance, 0, sizeof(* pNALInstance));

   if (socketpair(AF_UNIX, SOCK_STREAM, 0, pNALInstance->aWakeUpSockets))
   {
      PNALDebugError("CNALPreCreate : socketpair() failed %d", errno);

		return null;
   }

	return pNALInstance;
}


/* See Functional Specification Document */
bool_t CNALPostCreate(tNALInstance * pNALInstance, void * pNALVoidInstance)
{
	if (pthread_create(&pNALInstance->hProcessDriverEventsThread, NULL, PDriverReceiveThread, pNALVoidInstance))
	{
		PNALDebugError("Can not create the event processing thread");
		return W_FALSE;
	}

   return W_TRUE;
}

/* See Functional Specification Document */
void CNALPreDestroy(tNALInstance * pNALInstance)
{
}


/* See Functional Specification Document */
void CNALPostDestroy(tNALInstance * pNALInstance)
{
   if (pNALInstance)
   {
		close(pNALInstance->aWakeUpSockets[0]);
		close(pNALInstance->aWakeUpSockets[1]);
   }
}

void StopNALEventLoop(void)
{
   tNALInstance * pNALInstance = static_PNALGetInstance();
   if (pNALInstance->hProcessDriverEventsThread)
   {
      uint8_t buff[1];

      buff[0] = EVENT_QUIT;
      write(pNALInstance->aWakeUpSockets[0], buff, 1);
      pthread_join(pNALInstance->hProcessDriverEventsThread, NULL);
   }
}
tNALBinding  * GetNALBinding(void)
{

	PNALDebugError("GetNALBinding");

   return g_pNALBinding;
}

/* EOF */
