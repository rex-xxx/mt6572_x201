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

  This header files contains the compilation configuration of the library.

*******************************************************************************/

#ifndef __PORTING_CONFIG_H
#define __PORTING_CONFIG_H

/* Export the Client API */
#define OPEN_NFC_CLIENT_API /* empty: see library header of llibopen_nfc.so */

/*******************************************************************************
   Porting Type (See the Porting Guide)
*******************************************************************************/

#define P_BUILD_CONFIG   P_CONFIG_USER
#define P_CONFIG_CLIENT_SERVER

/*******************************************************************************
   Traces and Debug (See the Porting Guide)
*******************************************************************************/

/* Define the Debug active flag and the trace active flag */

#define DEBUG
#define P_TRACE_ACTIVE
#define P_DEBUG_ACTIVE


#define P_TRACE_LEVEL_DEFAULT   P_TRACE_ERROR
#define P_TRACE_LEVEL_BASIC   P_TRACE_TRACE
#define P_TRACE_LEVEL_SECSTACK   P_TRACE_TRACE

/*******************************************************************************
   Test Engine Configuration (See the Porting Guide)
*******************************************************************************/

/* List of processor types used for the test bundles
 *    - 0x01 for ix86 processor family
 *    - 0x02 for any processor compatible with ARM7 Thumb little-endian
 *    - 0x03 for any processor compatible with ARM7 ARM little-endian
 *    - 0x04 for any processor compatible with MIPS32 little-endian
 *    - 0x05 for x86 64-bit processor family
 */
#ifdef i386
#  define P_TEST_PROCESSOR       0x01
#endif
#ifdef __arm__
#  ifdef __thumb__
#     define P_TEST_PROCESSOR       0x02
#  else
#     define P_TEST_PROCESSOR       0x03
#  endif
#endif
#ifdef __mips__
#  define P_TEST_PROCESSOR       0x04
#endif

#define P_TEST_PRODUCT_NAME            "Linux Porting"

//#define P_INCLUDE_JAVA_API

/*******************************************************************************
   Synchronous function misuse detection
*******************************************************************************/

/* Comment this line to exclude this feature */
#define P_SYNCHRONOUS_FUNCTION_DEBUG

/*******************************************************************************
   Inclusion of Deprecated Functions
*******************************************************************************/

/* Comment this line to include the implementation of the deprecated functions */
 #define P_EXCLUDE_DEPRECATED_FUNCTIONS

/*******************************************************************************
   Inclusion of SNEP/NPP
*******************************************************************************/

/* Comment this line to exclude the implementation of the SNEP/NPP */
#define P_INCLUDE_SNEP_NPP

/*******************************************************************************
   Security Stack
*******************************************************************************/

/* Activation of the SE security. Comment this line to exclude this feature */
#define P_INCLUDE_SE_SECURITY

/*******************************************************************************
   BT & Wi-Fi Pairing
*******************************************************************************/

/* Activation of the pairing. Comment this line to exclude this feature */
#define P_INCLUDE_PAIRING

/*******************************************************************************
   Test Engine
*******************************************************************************/

/* Activation of the Test Engine. Comment this line to exclude this feature */
#define P_INCLUDE_TEST_ENGINE


/*******************************************************************************
   Picopass
*******************************************************************************/

/* Activation of the Picopass implementation. Comment this line to exclude this feature */
#define P_INCLUDE_PICOPASS

#endif /* __PORTING_CONFIG_H */
