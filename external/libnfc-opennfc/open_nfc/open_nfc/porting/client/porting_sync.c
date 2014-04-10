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


#include <stdio.h>
#include <stdlib.h>
#include <semaphore.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include "porting_os.h"


bool_t CSyncCreateWaitObject(
           P_SYNC_WAIT_OBJECT* phWaitObject)
{
   int retCode;

   /* Initialize event semaphore */
   retCode = sem_init(phWaitObject,    /* handle to the event semaphore */
                      0,               /* not shared */
                      0);              /* initially set to non signaled state */

   if (retCode != 0)
   {
      perror ("CSyncCreateWaitObject");
   }

   return (retCode != 0) ? W_FALSE : W_TRUE;
}

void CSyncWaitForObject(
           P_SYNC_WAIT_OBJECT* phWaitObject )
{
   int retCode;

   if(phWaitObject != NULL)
   {
      retCode = sem_wait(phWaitObject);

      if (retCode != 0)
      {
         perror ("CSyncWaitForObject");
      }
   }
}

void CSyncSignalWaitObject(
           P_SYNC_WAIT_OBJECT* phWaitObject )
{
   int retCode;

   if(phWaitObject != NULL)
   {
      retCode = sem_post(phWaitObject);

      if (retCode != 0)
      {
         perror ("CSyncSignalWaitObject");
      }
   }
}

void CSyncDestroyWaitObject(
           P_SYNC_WAIT_OBJECT* phWaitObject )
{
   int retCode;

   if(phWaitObject != NULL)
   {
      retCode = sem_destroy(phWaitObject);

      if (retCode != 0)
      {
         perror ("CSyncDestroyWaitObject");
      }
   }
}

uintptr_t CSyncGetCurrentTaskIdentifier(void)
{
   return pthread_self();
}
   
/* EOF */
