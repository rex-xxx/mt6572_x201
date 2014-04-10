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

  This header file contains the functions to implement to port the user part
  of Open NFC on the device hardware. For the porting, refer the documentation on the
  function detailed description and the porting process.

*******************************************************************************/

#ifndef __PORTING_DRIVER_H
#define __PORTING_DRIVER_H

#ifndef  P_CONFIG_DRIVER
/* Define the build configuration for the monolithic porting */
#  define P_CONFIG_MONOLITHIC  1
/* Define the build configuration for the driver porting */
#  define P_CONFIG_DRIVER  2
/* Define the build configuration for the user porting */
#  define P_CONFIG_USER    3
/* Define the build configuration for the client porting */
#  define P_CONFIG_CLIENT    4
/* Define the build configuration for the server porting */
#  define P_CONFIG_SERVER    5
#endif /* #ifndef  P_CONFIG_DRIVER */

#include "porting_config.h"
#include "porting_types.h"
#include "porting_inline.h"

/* Special value, not used with the function CSyncMapUserBuffer() */
#define P_SYNC_BUFFER_FLAG_I  0x01
#define P_SYNC_BUFFER_FLAG_O  0x02
#define P_SYNC_BUFFER_FLAG_A  0x04
#define P_SYNC_BUFFER_FLAG_W  0x08
#define P_SYNC_BUFFER_FLAG_L  0x10
#define P_SYNC_BUFFER_FLAG_2  0x20

#define P_SYNC_BUFFER_I    P_SYNC_BUFFER_FLAG_I
#define P_SYNC_BUFFER_O    P_SYNC_BUFFER_FLAG_O
#define P_SYNC_BUFFER_IO   (P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_O)
#define P_SYNC_BUFFER_IA   (P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_A)
#define P_SYNC_BUFFER_OA   (P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A)
#define P_SYNC_BUFFER_IOA  (P_SYNC_BUFFER_FLAG_I | P_SYNC_BUFFER_FLAG_O | P_SYNC_BUFFER_FLAG_A)

#ifndef __OPEN_NFC_H
typedef uint32_t W_ERROR;
#endif /* __OPEN_NFC_H */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*******************************************************************************
  Functions called by the Driver porting and the Monolithic porting
*******************************************************************************/

typedef struct __tDriverInstance tDriverInstance;

typedef struct __tUserInstance tUserInstance;

typedef void tPDriverCreateCompleted(
         void* pPortingConfig,
         uint32_t nMode);

tDriverInstance* PDriverCreate(
         tPDriverCreateCompleted* pCompletionCallback,
         void* pPortingConfig,
         bool_t bForceReset,
         void* pNALBinding,
         uint32_t nNALBindingSize );

void PDriverDestroy(
         tDriverInstance* pDriverInstance );

bool_t PDriverIsStarted(
         tDriverInstance* pDriverInstance );

void PDriverProcessEvents(
         tDriverInstance* pDriverInstance );

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */


/*******************************************************************************
  Functions called by the Driver porting
*******************************************************************************/

#if P_BUILD_CONFIG == P_CONFIG_DRIVER

tUserInstance* PDriverOpen(
          tDriverInstance* pDriverInstance,
          const tUserIdentity* pUserIdentity );

const tUserIdentity* PDriverGetUserIdentity(
          tUserInstance* pUserInstance );

void PDriverPreClose(
          tUserInstance* pUserInstance );

void PDriverClose(
          tUserInstance* pUserInstance );

W_ERROR PDriverIoctl(
          tUserInstance* pUserInstance,
          uint8_t nCode,
          void* pBuffer);

P_SYNC_SEMAPHORE* PDriverGetUserSemaphoreHandle(
         tUserInstance* pUserInstance );

#endif /* P_CONFIG_DRIVER */

/*******************************************************************************
  Functions to implement for the Server Porting porting
*******************************************************************************/

#if P_BUILD_CONFIG == P_CONFIG_DRIVER
#ifdef P_CONFIG_CLIENT_SERVER

/* See porting guide */
W_ERROR CServerRead(
            const tUserIdentity* pUserIdentity,
            void* pBuffer,
            uint32_t nLength);

/* See porting guide */
W_ERROR CServerWrite(
            const tUserIdentity* pUserIdentity,
            const void* pBuffer,
            uint32_t nLength);

/* See porting guide */
W_ERROR CServerCopyToClientBuffer(
            const tUserIdentity* pUserIdentity,
            const void * pUserBufferAddress,
            const void * pKernelBuffer,
            uint32_t nBufferLength);

#define P_Identifier_PSyncOutputData     (-1)

#endif /* #ifdef P_CONFIG_CLIENT_SERVER */
#endif /* P_CONFIG_DRIVER */

#endif /* __PORTING_DRIVER_H */
