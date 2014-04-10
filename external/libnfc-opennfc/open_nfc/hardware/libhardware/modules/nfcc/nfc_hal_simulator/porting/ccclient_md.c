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

Implementation of the standard CCClient trace client.

*******************************************************************************/

#include <pthread.h>
#include <string.h>
#include <sys/time.h>
#include <sys/socket.h>
#include <sys/errno.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <stdarg.h>

#include <android/log.h>


static const char TAG[] = "ccclienr_md";

#define LogDebug(format, ...)              __android_log_print(ANDROID_LOG_DEBUG,  TAG, format, ##__VA_ARGS__)
#define LogWarning(format, ...)                        __android_log_print(ANDROID_LOG_WARN,    TAG, format, ##__VA_ARGS__)
#define LogInformation(format, ...)       __android_log_print(ANDROID_LOG_INFO,      TAG, format, ##__VA_ARGS__)
#define LogError(format, ...)                 __android_log_print(ANDROID_LOG_ERROR,  TAG, format, ##__VA_ARGS__)

#include "ccclient.h"
#include "ccclient_md.h"

static const char * tag = "CC_MD";
static long long g_InitialTime = 0;

static void static_CCTracePrintf(const char* pTag, uint32_t nTraceLevel, const char* pMessage, ...)
{
  va_list list;

  va_start(list,pMessage);

  static_CCTracePrintf(pTag, nTraceLevel, pMessage, list);

   va_end(list);
}

static bool_t          bUseColor = W_FALSE;
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

bool_t CCInterlockedEnter()
{
   static bool_t nLastResult = W_FALSE;
   bool_t nResult;

   if (pthread_mutex_lock(&mutex) != 0)
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR, "pthread_mutex_lock failed");
   }

   nResult = nLastResult;
   nLastResult = W_TRUE;

   if (isatty(STDOUT_FILENO))
   {
      bUseColor = W_TRUE;
   }

   if (pthread_mutex_unlock(&mutex) != 0)
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR, "pthread_mutex_unlock failed");
   }

   return nResult;
}

typedef struct __OSCriticalSection
{
   pthread_mutex_t mutex;

} OSCriticalSection;

void CCCreateCriticalSection(
      CCCriticalSection* pCS)
{
   if (pthread_mutex_init( & ((OSCriticalSection*)pCS)->mutex, null) != 0)
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR, "pthread_mutex_init failed");
   }
}

void CCDestroyCriticalSection(
      CCCriticalSection* pCS)
{
   if (pthread_mutex_destroy(& ((OSCriticalSection*)pCS)->mutex) != 0)
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR, "pthread_mutex_destroy failed");
   }

}

void CCEnterCriticalSection(
      CCCriticalSection* pCS)
{
   if (pthread_mutex_lock(& ((OSCriticalSection*)pCS)->mutex) != 0)
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR,"pthread_mutex_lock failed\n");
   }
}

void CCLeaveCriticalSection(
      CCCriticalSection* pCS)
{
   if (pthread_mutex_unlock(& ((OSCriticalSection*)pCS)->mutex) != 0)
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR,"pthread_mutex_unlock failed\n");
   }
}

void CCInitializeTime(void)
{
   struct timeval currentTime;
   if (gettimeofday(&currentTime, NULL) == 0)
   {
      g_InitialTime = currentTime.tv_sec * 1000000 + currentTime.tv_usec;
   }
   else
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR,"gettimeofday failed\n");
   }
}

void CCGetAbsoluteRawTime(
               char* pBuffer,
               uint32_t nMaxLength)
{
   struct timeval currentTime;
   long long usec;

   if (gettimeofday(&currentTime, NULL) == 0)
   {
      usec = currentTime.tv_sec * 1000000 + currentTime.tv_usec;
   }
   else
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR,"gettimeofday failed\n");
      usec = 0;
   }

   snprintf(pBuffer, nMaxLength, "%lld", usec);
}

void CCGetRelativeTime(
               char* pBuffer,
               uint32_t nMaxLength)
{
   struct timeval currentTime;
   long long usec;

   if (gettimeofday(&currentTime, NULL) == 0)
   {
      usec = currentTime.tv_sec * 1000000 + currentTime.tv_usec;
      usec -= g_InitialTime;
   }
   else
   {
      static_CCTracePrintf(tag, P_TRACE_ERROR,"gettimeofday failed\n");
      usec = 0;
   }

   snprintf(pBuffer, nMaxLength, "[t=%8lld]", usec / 1000);
}

void CCPrintError(
            const char* pString)
{
   CCDefaultPrintf(P_TRACE_ERROR, pString);
   CCDefaultPrintf(P_TRACE_ERROR, "\n");
}

void CCDefaultPrintf(
               uint32_t nTraceLevel,
               const char* pString)
{
 
	switch (nTraceLevel)
	{
		case P_TRACE_TRACE:
			__android_log_write(ANDROID_LOG_DEBUG, tag, pString);
			break;

		case P_TRACE_LOG :
			__android_log_write(ANDROID_LOG_INFO, tag, pString);
			break;

		default:
		case P_TRACE_ERROR :
			__android_log_write(ANDROID_LOG_ERROR, tag, pString);
			break;
	}


}

uint32_t CCStrLen(
               const char* pString,
               uint32_t nMaxLength)
{
   return (uint32_t)strnlen(pString, nMaxLength);
}


void CCStrCat(
               char* pString,
               uint32_t nMaxLength,
               const char* pAppend)
{
   strncat(pString, pAppend, nMaxLength);
}

uint32_t CCVSPrintf(
               char* pString,
               uint32_t nMaxLength,
               const char* pFormat,
               va_list list)
{
   return (uint32_t)vsnprintf( pString, nMaxLength, pFormat, list);
}

typedef struct __OSSocket
{
   int socket;

} OSSocket;


uint32_t CCSocketCreate(
               const char* pAddress,
               CCSocket* pSocket)
{
	LogInformation("Sok: Addr1:[%s]", pAddress);
   int sConnectionSocket;
   struct sockaddr_in clientService;
   int bTCPNoDelay = 1;
   int nSendBufSize=1*1024*1024;
   uint32_t nError = CC_SUCCESS;

   sConnectionSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

   if (sConnectionSocket == -1)
   {
	   LogInformation("Sok: CC_ERROR_SOCKET_OPERATION0");
      nError = CC_ERROR_SOCKET_OPERATION;
      goto return_function;
   }

   if (setsockopt(sConnectionSocket, IPPROTO_TCP, TCP_NODELAY, (char*)&bTCPNoDelay, sizeof(bTCPNoDelay)) == -1)
   {
      nError = CC_ERROR_SOCKET_OPERATION;
      LogInformation("Sok: CC_ERROR_SOCKET_OPERATION1");
      goto return_function;
   }
   if (setsockopt(sConnectionSocket, SOL_SOCKET, SO_SNDBUF, (char*)&nSendBufSize, sizeof(nSendBufSize)) == -1)
   {
      nError = CC_ERROR_SOCKET_OPERATION;
      LogInformation("Sok: CC_ERROR_SOCKET_OPERATION2");
      goto return_function;
   }

   clientService.sin_family = AF_INET;
   clientService.sin_addr.s_addr = inet_addr(pAddress);
   clientService.sin_port = htons( 14443 );
   LogInformation("Sok: Addr2:[%s]", pAddress);
   if (connect( sConnectionSocket, (struct sockaddr*) &clientService, sizeof(clientService) ) == -1)
   {
      nError = CC_ERROR_CONNECTION_FAILURE;
      LogInformation("Sok: CC_ERROR_CONNECTION_FAILURE");
      goto return_function;
   }

   nError = CC_SUCCESS;

return_function:

   if(nError != CC_SUCCESS)
   {
      if(sConnectionSocket != -1)
      {
         close(sConnectionSocket);
         sConnectionSocket = -1;
      }
   }

   ((OSSocket*)pSocket)->socket = sConnectionSocket;

   return nError;
}

void CCSocketShutdownClose(
               CCSocket* pSocket)
{
   if(((OSSocket*)pSocket)->socket != -1)
   {
      shutdown(((OSSocket*)pSocket)->socket, SHUT_RDWR);
      close(((OSSocket*)pSocket)->socket);
      ((OSSocket*)pSocket)->socket = -1;
   }
}

int32_t CCSocketReceive(
            CCSocket* pSocket,
            uint8_t* pBuffer,
            uint32_t nLength,
            bool_t bWait)
{
   int32_t nOffset = 0;
   int32_t res;
   int socket = ((OSSocket*)pSocket)->socket;
   int nFlags = 0;


   if (bWait == W_FALSE)
   {
      nFlags = MSG_DONTWAIT;
   }

   while (nLength != 0)
   {
      res = recv(socket, &pBuffer[nOffset], nLength, nFlags);

      if(res < 0)
      {
         if ((errno == EAGAIN)  || (errno == EWOULDBLOCK))
         {
            res = 0;
         }
         else
         {
            return res;
         }
      }
      else if(res == 0)
      {
         /* Socket gracefully shutdown */
         return -1;
      }

      if(res == 0)
      {
         if((nOffset == 0) && (bWait == W_FALSE))
         {
            return 0;
         }

         nFlags = 0;
      }
      else
      {
         nOffset += res;
         nLength -= res;
      }
   }

   return 1;
}

bool_t CCSocketSend(
            CCSocket* pSocket,
            const uint8_t* pBuffer,
            uint32_t nLength)
{
   int32_t res;
   int32_t nOffset = 0;
   int socket = ((OSSocket*)pSocket)->socket;

   while(nLength != 0)
   {
      res = send(socket, &pBuffer[nOffset], nLength, 0);

      if(res < 0)
      {
        return W_FALSE;
      }

      nOffset += res;
      nLength -= res;
   }

   return W_TRUE;
}

void* CCSocketGetReceptionEvent(
            CCSocket* pSocket)
{
   return (void *) (uintptr_t) ((OSSocket*)pSocket)->socket;
}

void CCSocketSignalReceptionEvent(
            CCSocket* pSocket)
{
   /* FIXME */
}

void CCGetApplicationName(
            char16_t* pBuffer)
{
   char16_t   aName[] = { 'U','n', 'k','n','o','w','n', 0 };

   memcpy(pBuffer, aName, sizeof(aName));
}

void CCGetApplicationIdentifier(
            char16_t* pBuffer)
{
   char aBuffer[9];
   int i;

   sprintf(aBuffer, "%08x", getpid());

   for (i=0; i < sizeof(aBuffer); i++)
   {
      pBuffer[i] = aBuffer[i];
   }
}

void * CCMalloc(uint32_t nSize)
{
   return malloc(nSize);
}

void  CCFree(void * pMemory)
{
   free(pMemory);
}

void CCMemcpy(void * pDest, const void * pSrc, uint32_t nLength)
{
   memcpy(pDest, pSrc, nLength);
}
