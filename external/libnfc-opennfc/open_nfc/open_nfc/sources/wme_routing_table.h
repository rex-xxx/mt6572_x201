/*
 * Copyright (c) 2012 Inside Secure, All Rights Reserved.
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
   Contains the definition of the routing table data structures
*******************************************************************************/

#ifndef __WME_ROUTING_TABLE_H
#define __WME_ROUTING_TABLE_H

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define P_ROUTING_TABLE_MAX_LENGTH 253

typedef struct __tRoutingTableInstance
{
   /** Whether a request is currently executing or not */
   uint32_t nPendingFlag;

   /** The data buffer used to read and apply the Routing Table */
   uint8_t aBuffer[P_ROUTING_TABLE_MAX_LENGTH];

   /** The callback context */
   tDFCCallbackContext sCallbackContext;

}  tRoutingTableInstance;

#endif

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

typedef struct __tRoutingTableDriverInstance
{
   /** The NAL service operation */
   tNALServiceOperation sNALServiceOperation;

   /** The NAL data buffer (just enough to process NAL_PAR_ROUTING_TABLE_CONFIG) */
   uint8_t aNALDataBuffer[2];

   /** The cached configuration parameter */
   uint32_t nConfig;

   /** The callback context */
   tDFCDriverCCReference sDriverCC;

}  tRoutingTableDriverInstance;

#endif

#endif /* __WME_ROUTING_TABLE_H */

/* EOF */
