/*
 * Copyright (c) 2011-2012 Inside Secure, All Rights Reserved.
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

  Implementation of the Secure Element HAL.

*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( SE_HAL )

#include "porting_os.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifdef P_INCLUDE_SE_SECURITY

/* See HAL Documentation */
tCSePorting* CSeCreate(
         tCSeCallback* pCallback,
         void* pCallbackParameter,
         uint8_t* pRefreshFileList,
         uint32_t nRefreshFileListLength)
{
   return NULL;
}

/* See HAL Documentation */
void CSeDestroy(
         tCSePorting* pSePorting )
{
}

/* See HAL Documentation */
bool_t CSeGetStaticInfo(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t* pnFlags,
         uint32_t* pnSwpTimeout,
         uint8_t* pNameBuffer,
         uint32_t nNameBufferLength,
         uint32_t* pnActualNameLength )
{
   return W_FALSE;
}

/* See HAL Documentation */
void CSeGetInfo(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint8_t* pAtrBuffer,
         uint32_t nAtrBufferLength )
{
}

/* See HAL Documentation */
void CSeOpenChannel(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nType,
         const uint8_t* pAidBuffer,
         uint32_t nAidLength )
{
}

/* See HAL Documentation */
void CSeExchangeApdu(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier,
         const uint8_t* pApduBuffer,
         uint32_t nApduLength,
         uint8_t* pResponseApduBuffer,
         uint32_t nResponseApduBufferLength)
{
}

/* See HAL Documentation */
void CSeGetResponseApdu(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier,
         uint8_t* pResponseApduBuffer,
         uint32_t nResponseApduBufferLength,
         uint32_t * pnResponseApduActualSize)
{
}

/* See HAL Documentation */
void CSeCloseChannel(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier,
         uint32_t nSessionIdentifier,
         uint32_t nChannelIdentifier)
{
}

void CSeTriggerStkPolling(
         tCSePorting* pSePorting,
         uint32_t nSlotIdentifier)
{
}

#endif /* #ifdef P_INCLUDE_SE_SECURITY */

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

