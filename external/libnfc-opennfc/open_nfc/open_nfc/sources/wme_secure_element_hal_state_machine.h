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

#ifndef __WME_SE_HAL_STATE_MACHINE_H
#define __WME_SE_HAL_STATE_MACHINE_H

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

void PSeHalSmNotifyOperationCompletion(
                  tContext* pContext,
                  uint32_t nSlotIdentifier,
                  uint32_t nOperation,
                  bool_t bSuccess,
                  uint32_t nParam1,
                  uint32_t nParam2);

/* ----------------------------------------------------------------------------

      Logical Channel Interface

   ---------------------------------------------------------------------------- */

/**
 * @brief Opaque data type representing an instance of the ISO-7816 State Machine.
 **/
typedef struct __tPSeHalSmInstance tPSeHalSmInstance;

/**
 * @brief The default instance of the State Machine interface.
 *
 * All pointer-to-function data fields are guaranteed to be non-null pointers.
 **/
extern tP7816SmInterface g_sPSeHalSmInterface;

/**
 * @brief Creates a new instance of the ISO-7816 State Machine.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSlot  The slot instance.
 *
 * @param[out] ppInstance  The pointer that is used to receive the newly created
 *                 ISO-7816 State Machine instance.
 *
 * @result     An error code, among which:
 *             - W_SUCCESS, to indicate success.
 *             - W_ERROR_BAD_PARAMETER, to indicate that a passed parameter is incorrect.
 *             - W_ERROR_OUT_OF_RESOURCE, in case of an out-of-memory condition.
 **/
W_ERROR PSeHalSmCreateInstance(
                  tContext* pContext,
                  tSESlot* pSlot,
                  tPSeHalSmInstance** ppInstance);

/**
 * @brief Destroys an ISO-7816 State Machine.
 *
 * Associated memory is freed and the passed instance must not be used any longer.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 **/
void PSeHalSmDestroyInstance(
                  tContext* pContext,
                  tPSeHalSmInstance* pInstance);

#endif /* (P_CONFIG_DRIVER ||P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */

#endif /* __WME_SE_HAL_STATE_MACHINE_H */
