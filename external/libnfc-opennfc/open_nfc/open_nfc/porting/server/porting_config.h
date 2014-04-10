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

  This header files contains the compilation configuration of the library.

*******************************************************************************/

#ifndef __PORTING_CONFIG_H
#define __PORTING_CONFIG_H

/* Do not export the Client API */
#define OPEN_NFC_CLIENT_API

/*******************************************************************************
   Porting Type (See the Porting Guide)
*******************************************************************************/

#define P_BUILD_CONFIG   P_CONFIG_DRIVER

#define P_CONFIG_CLIENT_SERVER 1

/*******************************************************************************
   Traces and Debug (See the Porting Guide)
*******************************************************************************/
#define DEBUG
#define P_TRACE_ACTIVE
#define P_DEBUG_ACTIVE

#define P_TRACE_LEVEL_DEFAULT   P_TRACE_ERROR
#define P_TRACE_LEVEL_NFCC_B     P_TRACE_LOG
#define P_TRACE_LEVEL_SECSTACK   P_TRACE_TRACE

/*******************************************************************************
   NAL  (See the Porting Guide)
*******************************************************************************/

#define P_NFC_HAL_TRACE
/*******************************************************************************
   Reader (See the Porting Guide)
*******************************************************************************/

#define P_READER_DETECTION_TIMEOUT   1000

#define P_15693_3_MAX_ATTEMPT        1

/*******************************************************************************
   UICC Setting (See the Porting Guide)
*******************************************************************************/

#define P_UICC_SWP_BOOT_TIMEOUT  0

/*******************************************************************************
   STANDBY support Settings (See the Porting Guide)
*******************************************************************************/

#define P_CHIP_STANDBY_TIMEOUT               500

#define P_CHIP_AUTO_STANDBY_TIMEOUT          30000

/*******************************************************************************
   Random seed used to initialize random at startup
*******************************************************************************/

#define P_RANDOM_SEED            0x29D12422

//#define P_INCLUDE_JAVA_API

/*******************************************************************************
   Security Stack
*******************************************************************************/

/* Activation of the SE security. Comment this line to exclude this feature */
#define P_INCLUDE_SE_SECURITY


/* Define the behavior of the Security Stack with the UICC */
#define P_NO_UICC_ACCESS_BY_DEFAULT

/*******************************************************************************
   Inclusion of Deprecated Functions
*******************************************************************************/

/* Comment this line to include the implementation of the deprecated functions */
 #define P_EXCLUDE_DEPRECATED_FUNCTIONS

/*******************************************************************************
   Inclusion of the SNEP/NPP
*******************************************************************************/

/* Comment this line to exclude the implementation of the SNEP/NPP */
//#define P_INCLUDE_SNEP_NPP

/*******************************************************************************
   Mifare
*******************************************************************************/

/* Activation of the mifare mapping. Comment this line to exclude this feature */
#define P_INCLUDE_MIFARE_CLASSIC

/*******************************************************************************
   Picopass
*******************************************************************************/

/* Activation of the Picopass implementation. Comment this line to exclude this feature */
#define P_INCLUDE_PICOPASS

#endif /* __PORTING_CONFIG_H */
