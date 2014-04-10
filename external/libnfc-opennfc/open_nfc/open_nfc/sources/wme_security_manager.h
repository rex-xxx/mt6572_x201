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

#ifndef __WME_SECURITY_MANAGER_H
#define __WME_SECURITY_MANAGER_H
#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

/**
 * @brief Releases the security data linked to a user session.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pUserInstance  The user instance.
 **/
void PSecurityManagerDriverReleaseUserData(
   tContext * pContext,
   tUserInstance* pUserInstance);

/**
 * @brief  Checks the identity of user based on a principal value.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pUserInstance  The current user instance.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier where is read the principal value.
 *
 * @param[in]  pPrincipalBuffer  A pointer on the value of the principal.
 *             This value is never null.
 *
 * @param[in]  nPrincipalBufferLength  The length in bytes of the \a pPrincipalBuffer buffer.
 *             This value is always positive.
 *
 * @param[in]  pImpersonationDataBuffer  The buffer containing the impersonation data
 *             for the authentication. Null if impersonation is not requested.
 *
 * @param[in]  nImpersonationDataBufferLength  The length in bytes of the impersonation data.
 *             Zero if impersonation is not requested.
 *
 * @return  W_SUCCESS if the application matches the principal value,
 *          W_ERROR_SECURITY otherwise.
 **/
W_ERROR PSecurityManagerDriverCheckIdentity(
   tContext * pContext,
   tUserInstance* pUserInstance,
   uint32_t nHalSlotIdentifier,
   const uint8_t* pPrincipalBuffer,
   uint32_t nPrincipalBufferLength,
   const uint8_t* pImpersonationDataBuffer,
   uint32_t nImpersonationDataBufferLength );

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY*/
#endif /* __WME_SECURITY_MANAGER_H */
