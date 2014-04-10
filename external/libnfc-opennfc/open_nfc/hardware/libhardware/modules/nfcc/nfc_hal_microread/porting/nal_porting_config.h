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

#ifndef __NAL_PORTING_CONFIG_H
#define __NAL_PORTING_CONFIG_H


/*******************************************************************************
   Traces and Debug (See the Porting Guide)
*******************************************************************************/

#define P_NAL_TRACE_ACTIVE
#define P_NAL_DEBUG_ACTIVE

#define P_TRACE_LEVEL_DEFAULT    P_TRACE_ERROR

/*******************************************************************************
   SHDLC  (See the Porting Guide)
*******************************************************************************/

#define P_SHDLC_FRAME_READ_BUFFER_SIZE  16

#define P_SHDLC_TRACE 1

/*******************************************************************************
   RF Setting (See the Porting Guide)
*******************************************************************************/

/*
 *  Warning: use only the parameters provided by the NFC Controller manufacturer
 */

#define P_MGT_RF_CARD_DETECT                 { { 0x10, 0x04 },  \
                                               { 0x10, 0x05 },  \
                                               { 0x10, 0x06 },  \
                                               { 0x10, 0x07 } }

#endif /* __NAL_PORTING_CONFIG_H */
