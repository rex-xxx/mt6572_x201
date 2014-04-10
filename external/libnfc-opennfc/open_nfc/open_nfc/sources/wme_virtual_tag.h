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

#ifndef __WME_VIRTUAL_TAG_H
#define __WME_VIRTUAL_TAG_H

/*******************************************************************************
  Contains the declaration of the Tag Simulation implementation
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * @brief Reads synchronously a message from a tag simulation connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[out] phMessage A pointer on a variable valued with the message handle.
 *             This value is set to null upon error.
 *
 * @param[in]  nTNF  The message TNF value.
 *
 * @param[in]  pTypeString  The type string.
 **/
W_ERROR PVirtualTagReadMessage(
            tContext* pContext,
            W_HANDLE hConnection,
            W_HANDLE* phMessage,
            uint8_t nTNF,
            const char16_t* pTypeString );

/**
 * @brief  Writes synchronously a message on the current tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  hMessage  The message handle
 *
 * @param[in]  nActionMask  The action mask
 *
 * @return  The error code.
 **/

W_ERROR PVirtualTagWriteMessage(
            tContext * pContext,
            W_HANDLE hConnection,
            W_HANDLE hMessage,
            uint32_t nActionMask );

/* See Client API Specifications */
void PVirtualTagStart(
            tContext *pContext,
            W_HANDLE hVirtualTag,
            tPBasicGenericCallbackFunction* pCompletionCallback,
            void* pCompletionCallbackParameter,
            tPBasicGenericEventHandler* pEventCallback,
            void* pEventCallbackParameter,
            bool_t bReadOnly );

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_VIRTUAL_TAG_H */
