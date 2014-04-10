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

  Syncrhonization Functions.

*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( SYNC )

#include "porting_os.h"
#include <errno.h>
#include <sys/socket.h>

void CSyncCreateSemaphore(
                          P_SYNC_SEMAPHORE* phSemaphore )
{
   PDebugTrace("CSyncCreateSemaphore");

   /* we use a socket pair to wake up the client.
      one of the socket pair endpoint is provided to client using SCM_RIGHTS  */

   if (socketpair(AF_UNIX, SOCK_STREAM, 0, phSemaphore->sockets) < 0)
   {
      PDebugError("CSyncCreateSemaphore : socketpair() failed");

      phSemaphore->sockets[0] = phSemaphore->sockets[1] = -1;
   }
}

void CSyncDestroySemaphore(
                           P_SYNC_SEMAPHORE* phSemaphore )
{
   PDebugTrace("CSyncDestroySemaphore");

   close(phSemaphore->sockets[0]);
   close(phSemaphore->sockets[1]);
}

void CSyncIncrementSemaphore(
                             P_SYNC_SEMAPHORE* phSemaphore )
{
   PDebugTrace("CSyncIncrementSemaphore");

   if (send(phSemaphore->sockets[0], "K", 1, 0) != 1)
   {
      PDebugError("CSyncIncrementSemaphore : send failed");
   }
}

bool_t CSyncWaitSemaphore(
                        P_SYNC_SEMAPHORE* phSemaphore )
{

   PDebugTrace("CSyncWaitSemaphore");
   /* in client / server porting, the client waits for the semaphore prior retreiving server DFC */
   return W_TRUE;
}


/* EOF */
