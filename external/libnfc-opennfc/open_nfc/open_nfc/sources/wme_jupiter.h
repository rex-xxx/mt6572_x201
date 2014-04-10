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

#ifndef __WME_JUPITER_H
#define __WME_JUPITER_H

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * @brief Process the AID list returned by the NFC Controller.
 *
 * @param[in]    pContext  The current context.
 *
 * @param[in]    nSlotIdentifier  The slot identifier.
 *
 * @param[in]    nCardProtocols  The protocol detected for the transaction.
 *
 * @param[in]    pAIDListBuffer  The buffer with the AID list.
 *
 * @param[inout] pnAIDListLength  A pointer on a variable valued with
 *               the initial length of the AID list and updated with the new
 *               value.
 *
 * @return   W_SUCCESS if the processing is done,
 *           W_ERROR_OPERATION_PENDING if PJupiterGetTransactionAID must be called.
 **/
W_ERROR PJupiterProcessAIDList(
         tContext* pContext,
         uint32_t nSlotIdentifier,
         uint32_t nCardProtocols,
         uint8_t* pAIDListBuffer,
         uint32_t* pnAIDListLength);

/**
 * @brief Get the transaction history (AIDs list) from SE and merge it with the provided AID list
 *
 * @param[in]    pContext  The current context.
 *
 * @param[in]    nSlotIdentifier  The slot identifier.
 *
 * @param[inout] pAIDListBuffer  The buffer with the AID list.
 *
 * @param[in]    nAIDListBufferMaxLength  The maximum length in bytes of the AID
 *               list buffer.
 *
 * @param[inout] pnAIDListLength  A pointer on a variable valued with
 *               the initial length of the AID list and updated with the new
 *               value.
 *
 * @param[in]    pCallback  The callback function.
 *
 * @param[in]    pCallbackParameter  The callback blind parameter.
 **/
void PJupiterGetTransactionAID(
         tContext* pContext,
         uint32_t nSlotIdentifier,
         uint8_t* pAIDListBuffer,
         uint32_t nAIDListBufferMaxLength,
         uint32_t* pnAIDListLength,
         tPBasicGenericCallbackFunction* pCallback,
         void* pCallbackParameter);


/**
 * @brief Sets the policy for a Jupiter SE.
 *
 * If the callback is called with an error code, the policy is not modified.
 *
 * The callback function returns one of the following result codes:
 *  - \ref W_SUCCESS The flag value is set.
 *  - \ref W_ERROR_BAD_PARAMETER One of the parameter is wrong.
 *  - \ref W_ERROR_EXCLUSIVE_REJECTED A set policy operation is already pending.
 *  - \ref W_ERROR_FEATURE_NOT_SUPPORTED The parameter values are valid but
 *    this configuration is not supported by the Secure Element.
 *  - others If any other error occurred.
 *
 * @param[in]  nSlotIdentifier  The slot identifier in the range [0, \ref W_NFCC_PROP_SE_NUMBER - 1].
 *
 * @param[in]  nStorageType  The storage type.
 *
 * @param[in]  nPolicyProtocols  The bit-field representing the SWP access rights.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 **/
void PJupiterSetPolicy(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            uint32_t nStorageType,
            uint32_t nProtocols,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);


/**
 * @brief Gets the policies from the embedded Jupiter SE.
 *
 * The policies is get from the embedded SE and set in the both given values (args)
 *
 * If the callback is called with an error code, the arguments values are set to 0
 *
 * The callback function returns one of the following result codes:
 *  - \ref W_SUCCESS The flag value is set.
 *  - \ref W_ERROR_BAD_PARAMETER One of the parameter is wrong.
 *  - \ref W_ERROR_EXCLUSIVE_REJECTED A set policy operation is already pending.
 *  - others If any other error occurred.
 *
 * If hConnection is null, the slot identifier is used. Otherwise the slot identifier is ignored.
 *
 * @param[in]  nSlotIdentifier  The slot identifier in the range [0, \ref W_NFCC_PROP_SE_NUMBER - 1].
 *
 * @param[in]  hConnection  The connection on the secure element.
 *
 * @param[in]  pnPersistentPolicy a pointer to an uint32_t representing the persistent policy in the SE
 *
 * @param[in]  pnVolatilePolicy a pointer to an uint32_t representing the persistent policy in the SE
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 **/
void PJupiterGetPolicy(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            uint32_t * pnPersistentPolicy,
            uint32_t * pnVolatilePolicy,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
#endif /* __WME_JUPITER_H */
