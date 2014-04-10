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

#ifndef __PORTING_USER_H
#define __PORTING_USER_H

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

#if (P_BUILD_CONFIG == P_CONFIG_USER)
#ifndef P_CONFIG_CLIENT_SERVER

/*******************************************************************************
  Driver Access Functions
*******************************************************************************/

void* CUserOpen(void);

void CUserClose(
            void* pUserInstance);

W_ERROR CUserIoctl(
            void* pUserInstance,
            uint8_t nCode,
            void* pBuffer,
            uint32_t nInputSize,
            uint32_t nOutputSize);

#endif /* #ifndef P_CONFIG_CLIENT_SERVER */
#endif /* P_CONFIG_USER */

#endif /* __PORTING_USER_H */
