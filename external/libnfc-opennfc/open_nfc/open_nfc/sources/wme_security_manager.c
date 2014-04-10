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
   Contains the implementation of the security manager functions
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( SECMGT )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See C API Specification */
W_ERROR PSecurityAuthenticate(
         tContext * pContext,
         const uint8_t* pApplicationDataBuffer,
         uint32_t nApplicationDataBufferLength )
{
   PDebugTrace("PSecurityAuthenticate()");

#ifdef P_INCLUDE_SE_SECURITY
   return PSecurityManagerDriverAuthenticate(
            pContext, pApplicationDataBuffer, nApplicationDataBufferLength );
#else
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
#endif /* P_INCLUDE_SE_SECURITY */
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

/** See WSecurityAuthenticate() */
W_ERROR PSecurityManagerDriverAuthenticate(
   tContext * pContext,
   const uint8_t* pApplicationDataBuffer,
   uint32_t nApplicationDataBufferLength )
{
   tUserInstance* pUserInstance;

   PDebugTrace("PSecurityManagerDriverAuthenticate()");

   pUserInstance = PContextGetCurrentUserInstance(pContext);

   if(pUserInstance->bAuthenticationDone != W_FALSE)
   {
      PDebugError("PSecurityManagerDriverAuthenticate: The authentication is already done");
      return W_ERROR_BAD_STATE;
   }

   pUserInstance->bAuthenticationDone = W_TRUE;
   pUserInstance->pUserAuthenticationData = null;
   pUserInstance->nUserAuthenticationDataLength = 0;

   if( CSecurityCreateAuthenticationData(
         pUserInstance->pUserIdentity,
         pApplicationDataBuffer,
         nApplicationDataBufferLength,
         &pUserInstance->pUserAuthenticationData,
         &pUserInstance->nUserAuthenticationDataLength) == W_FALSE)
   {
      PDebugError("PSecurityManagerDriverAuthenticate: Error returned by CSecurityCreateAuthenticationData()");
      pUserInstance->pUserAuthenticationData = null;
      pUserInstance->nUserAuthenticationDataLength = 0;
      return W_ERROR_SECURITY;
   }

   return W_SUCCESS;
}

/** See header file */
void PSecurityManagerDriverReleaseUserData(
   tContext * pContext,
   tUserInstance* pUserInstance)
{
   PDebugTrace("PSecurityManagerDriverReleaseUserData()");

   pUserInstance->bAuthenticationDone = W_FALSE;

   if(pUserInstance->pUserAuthenticationData != null)
   {
      CSecurityDestroyAuthenticationData(pUserInstance->pUserAuthenticationData, pUserInstance->nUserAuthenticationDataLength);
      pUserInstance->pUserAuthenticationData = null;
      pUserInstance->nUserAuthenticationDataLength = 0;
   }
}

/** See header file */
W_ERROR PSecurityManagerDriverCheckIdentity(
   tContext * pContext,
   tUserInstance* pUserInstance,
   uint32_t nHalSlotIdentifier,
   const uint8_t* pPrincipalBuffer,
   uint32_t nPrincipalBufferLength,
   const uint8_t* pImpersonationDataBuffer,
   uint32_t nImpersonationDataBufferLength )
{
   PDebugTrace("PSecurityManagerDriverCheckIdentity()");

   CDebugAssert(pUserInstance != null);

   if(pImpersonationDataBuffer == null)
   {
      if(pUserInstance->bAuthenticationDone == W_FALSE)
      {
         PDebugTrace("PSecurityManagerDriverCheckIdentity: Authentication not performed");
         return W_ERROR_SECURITY;
      }

      if(CSecurityCheckIdentity(
               nHalSlotIdentifier,
               pUserInstance->pUserIdentity,
               pUserInstance->pUserAuthenticationData, pUserInstance->nUserAuthenticationDataLength,
               pPrincipalBuffer, nPrincipalBufferLength) == W_FALSE)
      {
         PDebugTrace("PSecurityManagerDriverCheckIdentity: Prinipal not identified by CSecurityCheckIdentity()");
         return W_ERROR_SECURITY;
      }
   }
   else
   {
      if(pUserInstance->bAuthenticationDone == W_FALSE)
      {
         /* The authentication of the calling application may not be required for the impersonation
            Just check the consistency of the data
          */
         if((pUserInstance->pUserAuthenticationData != null) || (pUserInstance->nUserAuthenticationDataLength != 0))
         {
            PDebugTrace("PSecurityManagerDriverCheckIdentity: Inconsistent authentication data");
            return W_ERROR_SECURITY;
         }
      }

      if(CSecurityCheckImpersonatedIdentity(
               nHalSlotIdentifier,
               pUserInstance->pUserIdentity,
               pUserInstance->pUserAuthenticationData, pUserInstance->nUserAuthenticationDataLength,
               pPrincipalBuffer, nPrincipalBufferLength,
               pImpersonationDataBuffer, nImpersonationDataBufferLength) == W_FALSE)
      {
         PDebugTrace("PSecurityManagerDriverCheckIdentity: Prinipal not identified by CSecurityCheckImpersonatedIdentity()");
         return W_ERROR_SECURITY;
      }
   }

   return W_SUCCESS;
}

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */


#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (! defined P_INCLUDE_SE_SECURITY)

/* Dummy function to allow proper link of the solution even if P_INCLUDE_SE_SECURITY is not defined
   (autogen limitation workaround */

/** See WSecurityAuthenticate() */
W_ERROR PSecurityManagerDriverAuthenticate(
   tContext * pContext,
   const uint8_t* pApplicationDataBuffer,
   uint32_t nApplicationDataBufferLength )
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

#endif

