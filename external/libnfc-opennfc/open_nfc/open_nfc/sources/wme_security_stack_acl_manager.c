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
   Contains the implementation of the security stack ACL manager functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( SECSTACK )

#include "wme_context.h"

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

#include "wme_security_stack_acl_manager.h"

/** AID type: All AID */
#define P_SECSTACK_AID_TYPE_ALL 0
/** AID type: Default AID */
#define P_SECSTACK_AID_TYPE_DEFAULT 1
/** AID type: Specific AID */
#define P_SECSTACK_AID_TYPE_SPECIFIC 2

/*
 * The following data types are used to build an internal representation of the ACL.
 */

/** Structure for an ACIE (Access Control Index Entry) */
typedef struct __tACIE
{
   /** The internal index of this ACIE */
   uint32_t nAcieIndex;
   /** The pointer to the next ACIE */
   struct __tACIE* pNextAcie;
   /** The type of the AID (one of the P_SECSTACK_AID_TYPE_* constants) */
   uint32_t nAidType;
   /** The length of the AID (0 indicates that the AID is not present) */
   uint32_t nAidLength;
   /** The AID value */
   uint8_t aAid[P_SECSTACK_AID_MAX_LENGTH];
   /** The pointer to the ACE associated with this ACIE */
   struct __tACE* pAce;
}  tACIE;

/** Structure for an ACE (Access Control Entry) */
struct __tACE
{
   /** The internal index of this ACE */
   uint32_t nAceIndex;
   /** The pointer to the next ACE */
   struct __tACE* pNextAce;
   /** The path of the EF file */
   uint32_t nFid;
   /** The index (offset) of the ACE with the EF file */
   uint32_t nIndex;
   /** The length of the ACE with the EF file */
   uint32_t nLength;
   /** The type of Principals */
   uint32_t nPrincipalType;
   /** The number of Principals */
   uint32_t nPrincipals;
   /** The array of Principals (dynamically allocated) */
   tPrincipal* pPrincipals;
   /** The type of Permissions */
   uint32_t nPermissionType;
   /** The number of Permissions */
   uint32_t nPermissions;
   /** The array of Permissions (dynamically allocated) */
   tPermission* pPermissions;
};

/** Structure for an ACL (Access Control List) */
struct __tSecurityStackACL
{
   /** Whether the Security Stack manages rules with default AID */
   bool_t bManageDefaultSelectFci;
   /** The next ACIE internal index */
   uint32_t nNextAcieIndex;
   /** Pointer to the first ACIE */
   tACIE* pFirstAcie;
   /** The next ACE internal index */
   uint32_t nNextAceIndex;
   /** Pointer to the first ACE */
   tACE* pFirstAce;
};


/** The maximum number of checked Principals that are are cached */
#define P_SECSTACK_MAX_CACHED_PRINCIPALS 16

/** Structure used to cache PSecurityManagerDriverCheckIdentity results */
typedef struct __tSecurityStackCheckIdentityResultCache
{
   /** The number of checked Principals (with failure). The value -1 indicates the current Principal (stored in slot 0) has been found */
   int32_t nFailedPrincipals;
   /** The array of checked Principals */
   tPrincipal sPrincipals[P_SECSTACK_MAX_CACHED_PRINCIPALS];
}  tSecurityStackCheckIdentityResultCache;


/** See header file */
W_ERROR PSecurityStackAclCreateInstance(
   bool_t bManageDefaultSelectFci,
   tSecurityStackACL** ppAcl)
{
   tSecurityStackACL* pAcl;

   if (ppAcl == null)
   {
      PDebugError("PSecurityStackAclCreateInstance: ppAcl == null");
      return W_ERROR_BAD_PARAMETER;
   }

   pAcl = (tSecurityStackACL*)CMemoryAlloc(sizeof(*pAcl));
   if (pAcl == null)
   {
      PDebugError("PSecurityStackAclCreateInstance: Unable to allocate memory");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pAcl, 0, sizeof(*pAcl));

   pAcl->bManageDefaultSelectFci = bManageDefaultSelectFci;
   pAcl->nNextAcieIndex = 1;
   pAcl->pFirstAce = (tACE*)null;
   pAcl->nNextAceIndex = 1;
   pAcl->pFirstAcie = (tACIE*)null;

   *ppAcl = pAcl;

   return W_SUCCESS;
}

/** See header file */
W_ERROR PSecurityStackIsDefaultAidSupported(
   tSecurityStackACL* pAcl)
{
   if (pAcl == null)
   {
      PDebugError("PSecurityStackIsDefaultAidSupported: pAcl == null");
      return W_ERROR_BAD_PARAMETER;
   }

   return (pAcl->bManageDefaultSelectFci != W_FALSE) ? W_SUCCESS : W_ERROR_BAD_STATE;
}


/** See header file */
W_ERROR PSecurityStackAclCreateAcie(
   tSecurityStackACL* pAcl,
   const uint8_t* pAid,
   uint32_t nAidLength,
   tACE* pAce)
{
   tACIE* pAcie;
   uint32_t nAidType;

   if (pAcl == null)
   {
      PDebugError("PSecurityStackAclCreateAcie: pAcl == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pAid == P_SECSTACK_BUFFER_AID_ALL) && (nAidLength == P_SECSTACK_LENGTH_AID_ALL))
   {
      nAidType = P_SECSTACK_AID_TYPE_ALL;

      pAid = (const uint8_t*)null;
      nAidLength = 0;
   }
   else if ((pAid == null) && (nAidLength == 0))
   {
      if (pAcl->bManageDefaultSelectFci == W_FALSE)
      {
         PDebugError("PSecurityStackAclCreateAcie: Default AID is incompatible with bManageDefaultSelectFci option");
         return W_ERROR_BAD_PARAMETER;
      }

      nAidType = P_SECSTACK_AID_TYPE_DEFAULT;
   }
   else if ((pAid != null) && ((nAidLength == P_SECSTACK_MFID_LENGTH) || ((nAidLength >= P_SECSTACK_AID_MIN_LENGTH) && (nAidLength <= P_SECSTACK_AID_MAX_LENGTH))))
   {
      nAidType = P_SECSTACK_AID_TYPE_SPECIFIC;
   }
   else
   {
      PDebugError("PSecurityStackAclCreateAcie: Invalid pAid/nAidLength parameters");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAce == null)
   {
      PDebugError("PSecurityStackAclCreateAcie: pAce == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Allocate ACIE */
   pAcie = (tACIE*)CMemoryAlloc(sizeof(*pAcie));
   if (pAcie == null)
   {
      PDebugError("PSecurityStackAclCreateAcie: Unable to allocate memory");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pAcie, 0, sizeof(*pAcie));

   pAcie->nAcieIndex = pAcl->nNextAcieIndex++;
   pAcie->pNextAcie = (tACIE*)null;
   pAcie->nAidType = nAidType;
   if (pAid != null)
   {
      pAcie->nAidLength = nAidLength;
      CMemoryCopy(pAcie->aAid, pAid, nAidLength);
   }
   pAcie->pAce = pAce;

   /* Append ACIE to the ACL data structure */
   if (pAcl->pFirstAcie == null)
   {
      pAcl->pFirstAcie = pAcie;
   }
   else
   {
      tACIE* pLastAcie;
      for(pLastAcie = pAcl->pFirstAcie; pLastAcie->pNextAcie != null; pLastAcie = pLastAcie->pNextAcie);
      pLastAcie->pNextAcie = pAcie;
   }

   return W_SUCCESS;
}

/** See header file */
W_ERROR PSecurityStackAclCreateAce(
   tSecurityStackACL* pAcl,
   uint32_t nAceFid,
   uint32_t nAceIndex,
   uint32_t nAceLength,
   tACE** ppAce)
{
   tACE* pAce;

   if (pAcl == null)
   {
      PDebugError("PSecurityStackAclLookupAce: pAcl == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (ppAce == null)
   {
      PDebugError("PSecurityStackAclLookupAce: ppAce == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* First check whether this ACE has already been processed */
   for (pAce = pAcl->pFirstAce; pAce != null; pAce = pAce->pNextAce)
   {
      if ((pAce->nFid == nAceFid) && (pAce->nIndex == nAceIndex) && (pAce->nLength == nAceLength))
      {
         *ppAce = pAce;
         return W_SUCCESS;
      }
   }

   /* Allocate the ACE */
   pAce = (tACE*)CMemoryAlloc(sizeof(*pAce));
   if (pAce == null)
   {
      PDebugError("PSecurityStackAclCreateAce: Unable to allocate memory");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pAce, 0, sizeof(*pAce));

   /* Initialize the ACE */
   pAce->nAceIndex = pAcl->nNextAceIndex++;
   pAce->pNextAce = (tACE*)null;
   pAce->nFid = nAceFid;
   pAce->nIndex = nAceIndex;
   pAce->nLength = nAceLength;
   pAce->nPrincipalType = P_SECSTACK_PRINCIPAL_TYPE_NEVER;
   pAce->nPrincipals = 0;
   pAce->pPrincipals = (tPrincipal*)null;
   pAce->nPermissionType = P_SECSTACK_PERMISSION_TYPE_NEVER;
   pAce->nPermissions = 0;
   pAce->pPermissions = (tPermission*)null;

   /* Append the ACE to the ACL data structure */
   if (pAcl->pFirstAce == null)
   {
      pAcl->pFirstAce = pAce;
   }
   else
   {
      tACE* pLastAce;
      for(pLastAce = pAcl->pFirstAce; pLastAce->pNextAce != null; pLastAce = pLastAce->pNextAce);
      pLastAce->pNextAce = pAce;
   }

   *ppAce = pAce;

   /* Return this error code to indicate that the ACE has been created */
   return W_ERROR_ITEM_NOT_FOUND;
}

/** See header file */
W_ERROR PSecurityStackAclCreateAcePrincipals(
   tSecurityStackACL* pAcl,
   tACE* pAce,
   uint32_t nPrincipals,
   tPrincipal** ppPrincipals)
{
   if (pAcl == null)
   {
      PDebugError("PSecurityStackAclCreateAcePrincipals: pAcl == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAce == null)
   {
      PDebugError("PSecurityStackAclCreateAcePrincipals: pAce == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nPrincipals == 0) && (ppPrincipals != null))
   {
      PDebugError("PSecurityStackAclCreateAcePrincipals: nPrincipals == 0 && ppPrincipals != null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nPrincipals != 0) && (ppPrincipals == null))
   {
      PDebugError("PSecurityStackAclCreateAcePrincipals: nPrincipals != 0 && ppPrincipals == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Reset current principals */
   pAce->nPrincipalType = P_SECSTACK_PRINCIPAL_TYPE_NEVER;
   pAce->nPrincipals = 0;
   if (pAce->pPrincipals != null)
   {
      CMemoryFree(pAce->pPrincipals);
      pAce->pPrincipals = (tPrincipal*)null;
   }

   if (nPrincipals == 0)
   {
      pAce->nPrincipalType = P_SECSTACK_PRINCIPAL_TYPE_ALWAYS;
   }
   else
   {
      pAce->pPrincipals = (tPrincipal*)CMemoryAlloc(nPrincipals*sizeof(tPrincipal));
      if (pAce->pPrincipals == null)
      {
         PDebugError("PSecurityStackAclCreateAcePrincipals: Unable to allocate memory");
         return W_ERROR_OUT_OF_RESOURCE;
      }
      CMemoryFill(pAce->pPrincipals, 0, nPrincipals*sizeof(tPrincipal));

      pAce->nPrincipalType = P_SECSTACK_PRINCIPAL_TYPE_SPECIFIC;
      pAce->nPrincipals = nPrincipals;

      *ppPrincipals = pAce->pPrincipals;
   }

   return W_SUCCESS;
}

/** See header file */
W_ERROR PSecurityStackAclCreateAcePermissions(
   tSecurityStackACL* pAcl,
   tACE* pAce,
   uint32_t nPermissions,
   tPermission** ppPermissions)
{
   if (pAcl == null)
   {
      PDebugError("PSecurityStackAclCreateAcePermissions: pAcl == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAce == null)
   {
      PDebugError("PSecurityStackAclCreateAcePermissions: pAce == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nPermissions == 0) && (ppPermissions != null))
   {
      PDebugError("PSecurityStackAclCreateAcePermissions: nPermissions == 0 && ppPermissions != null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nPermissions != 0) && (ppPermissions == null))
   {
      PDebugError("PSecurityStackAclCreateAcePermissions: nPermissions != 0 && ppPermissions == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Reset current permissions */
   pAce->nPermissionType = P_SECSTACK_PERMISSION_TYPE_NEVER;
   pAce->nPermissions = 0;
   if (pAce->pPermissions != null)
   {
      CMemoryFree(pAce->pPermissions);
      pAce->pPermissions = (tPermission*)null;
   }

   if (nPermissions == 0)
   {
      pAce->nPermissionType = P_SECSTACK_PERMISSION_TYPE_ALWAYS;
   }
   else
   {
      pAce->pPermissions = (tPermission*)CMemoryAlloc(nPermissions*sizeof(tPermission));
      if (pAce->pPermissions == null)
      {
         PDebugError("PSecurityStackAclCreateAcePermissions: Unable to allocate memory");
         return W_ERROR_OUT_OF_RESOURCE;
      }
      CMemoryFill(pAce->pPermissions, 0, nPermissions*sizeof(tPermission));

      pAce->nPermissionType = P_SECSTACK_PERMISSION_TYPE_SPECIFIC;
      pAce->nPermissions = nPermissions;

      *ppPermissions = pAce->pPermissions;
   }

   return W_SUCCESS;
}

/* See header file */
W_ERROR PSecurityStackAclDestroyInstance
(
   tSecurityStackACL* pAcl
)
{
   tACE* pAce;
   tACIE* pAcie;

   if (pAcl == null)
   {
      PDebugError("PSecurityStackAclDestroyInstance: pAcl == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Free the (linked) list of ACIE */
   pAcie = pAcl->pFirstAcie;
   while(pAcie != null)
   {
      tACIE* pNextAcie = pAcie->pNextAcie;
      CMemoryFree(pAcie);
      pAcie = pNextAcie;
   }

   /* Free the (linked) list of ACE */
   pAce = pAcl->pFirstAce;
   while(pAce != null)
   {
      tACE* pNextAce = pAce->pNextAce;

      if (pAce->pPrincipals != null)
      {
         CMemoryFree(pAce->pPrincipals);
         pAce->pPrincipals = (tPrincipal*)null;
      }

      if (pAce->pPermissions != null)
      {
         CMemoryFree(pAce->pPermissions);
         pAce->pPermissions = (tPermission*)null;
      }

      CMemoryFree(pAce);
      pAce = pNextAce;
   }

   /* Free the ACL itself */
   CMemoryFree(pAcl);

   return W_SUCCESS;
}

/**
 * @brief   Checks an ACL Principal against the current Principal.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pUserInstance  The current user instance.
 *
 * @param[in]  nHalSlotIdentifier  The identifier of the slot where is read the principal value
 *
 * @param[in]  pPrincipalBuffer  A pointer to the principal.
 *
 * @param[in]  nPrincipalBufferLength  The length in bytes of the \a pPrincipalBuffer buffer.
 *
 * @param[in]  pCache  A pointer to the cache holding previous principal identity check results.
 *
 * @return One of the following error code:
 *          - W_SUCCESS  The passed Principal matches the current Principal.
 *          - W_ERROR_SECURITY  The passed Principal does not match the current Principal.
 **/
static W_ERROR static_PSecurityStackCheckPrincipalIdentity
(
   tContext* pContext,
   tUserInstance* pUserInstance,
   uint32_t nHalSlotIdentifier,
   const uint8_t* pPrincipalBuffer,
   uint32_t nPrincipalBufferLength,
   tSecurityStackCheckIdentityResultCache* pCache
)
{
   CDebugAssert(pCache != null);
   CDebugAssert(pPrincipalBuffer != null);
   CDebugAssert(nPrincipalBufferLength == P_SECSTACK_PRINCIPALID_LENGTH);

   if (pCache->nFailedPrincipals == -1) /* Current Principal already found ? */
   {
      if (CMemoryCompare(pCache->sPrincipals[0].id, pPrincipalBuffer, P_SECSTACK_PRINCIPALID_LENGTH) == 0)
      {
         return W_SUCCESS;
      }
   }
   else
   {
      int32_t i;

      CDebugAssert((pCache->nFailedPrincipals >= 0) && (pCache->nFailedPrincipals < P_SECSTACK_MAX_CACHED_PRINCIPALS));

      for(i = 0; i < pCache->nFailedPrincipals; i++)
      {
         if (CMemoryCompare(pCache->sPrincipals[i].id, pPrincipalBuffer, P_SECSTACK_PRINCIPALID_LENGTH) == 0)
         {
            return W_ERROR_SECURITY;
         }
      }

      if (PSecurityManagerDriverCheckIdentity(pContext, pUserInstance, nHalSlotIdentifier, pPrincipalBuffer, nPrincipalBufferLength, null, 0) == W_SUCCESS)
      {
         CMemoryCopy(pCache->sPrincipals[0].id, pPrincipalBuffer, P_SECSTACK_PRINCIPALID_LENGTH);
         pCache->nFailedPrincipals = -1; /* Found */

         return W_SUCCESS;
      }

      if (pCache->nFailedPrincipals < P_SECSTACK_MAX_CACHED_PRINCIPALS)
      {
         CMemoryCopy(pCache->sPrincipals[pCache->nFailedPrincipals++].id, pPrincipalBuffer, P_SECSTACK_PRINCIPALID_LENGTH);
      }
   }

   return W_ERROR_SECURITY;
}

/* See header file */
W_ERROR PSecurityStackCheckAcl(
   tContext* pContext,
   uint32_t nHalSlotIdentifier,
   tUserInstance* pUserInstance,
   tSecurityStackACL* pAcl,
   const uint8_t* pAid,
   uint32_t nAidLength,
   const uint8_t* pApduHeader,
   bool_t bIsAnswerApdu)
{
   tSecurityStackCheckIdentityResultCache cache;
   bool_t bProcessGeneralRule = W_FALSE;
   bool_t bFoundGeneralRule;

   PDebugTrace("PSecurityStackCheckAcl()");

   if (pAcl == null)
   {
      PDebugError("PSecurityStackCheckAcl: Invalid ACL parameter");
      return W_ERROR_SECURITY;
   }

   if ((pAid == null) && (nAidLength != 0))
   {
      PDebugError("PSecurityStackCheckAcl: pAid == null && nAidLength != 0");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pAid != null) && (nAidLength == 0))
   {
      /* This is an unknown AID. Directly process general rules */
      bProcessGeneralRule = W_TRUE;
   }
   else if ((pAid != null) && ! ((nAidLength == P_SECSTACK_MFID_LENGTH) || ((nAidLength >= P_SECSTACK_AID_MIN_LENGTH) && (nAidLength <= P_SECSTACK_AID_MAX_LENGTH))))
   {
      PDebugError("PSecurityStackCheckAcl: pAid == null && nAidLength not 2 and not in range 5..16");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pAid == null) && (pApduHeader != null))
   {
      PDebugError("PSecurityStackCheckAcl: pAid == null && pApduHeader != null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((bIsAnswerApdu != W_FALSE) && (pApduHeader != null))
   {
      PDebugError("PSecurityStackCheckAcl: bIsAnswerApdu != W_FALSE && pApduHeader != null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAid == null)
   {
      PDebugTrace("PSecurityStackCheckAcl: AID is null (Default application AID)");
   }
   else
   {
      PDebugTrace("PSecurityStackCheckAcl: AID {");
      PDebugTraceBuffer(pAid, nAidLength);
   }

   if (pApduHeader == null)
   {
      PDebugTrace("PSecurityStackCheckAcl: APDU Header is the SELECT[AID] command");
   }
   else
   {
      PDebugTrace("PSecurityStackCheckAcl: APDU Header {");
      PDebugTraceBuffer(pApduHeader, 4);
   }

   /*
    * Clear Principal cache
    */
   CMemoryFill(&cache, 0, sizeof(cache));

   CDebugAssert((W_FALSE == 0) && (W_TRUE == (W_FALSE + 1)));
   for(bFoundGeneralRule = W_FALSE; bProcessGeneralRule == bFoundGeneralRule; bProcessGeneralRule++)
   {
      tACIE* pAcie = (tACIE*)null;
      bool_t bFoundSpecificRule = W_FALSE;
      bool_t bGrantAccess = W_FALSE;

      /*
       * Scan all ACIE
       * - First loop is for the default AID or a specific AID.
       * - Second loop is for all AID
       */
      for(pAcie = pAcl->pFirstAcie; pAcie != null; pAcie = pAcie->pNextAcie)
      {
         if (bProcessGeneralRule == W_FALSE)
         {
            /* Process the default/specific AID */
            PDebugTrace("PSecurityStackCheckAcl: Processing ACIE#%d (Default/specific AID rules)", pAcie->nAcieIndex);

            if (pAcie->nAidType == P_SECSTACK_AID_TYPE_ALL)
            {
               bFoundGeneralRule = W_TRUE;
            }
            else if ((pAcie->nAidType == P_SECSTACK_AID_TYPE_DEFAULT) && (pAid == null))
            {
               /* Default AID matches */
               bFoundSpecificRule = W_TRUE;
               goto found_aid;
            }
            else if ((pAcie->nAidType == P_SECSTACK_AID_TYPE_SPECIFIC) && (pAid != null)
                  && (pAcie->nAidLength == nAidLength) && (CMemoryCompare(pAcie->aAid, pAid, nAidLength) == 0))
            {
               /* Specific AID matches */
               bFoundSpecificRule = W_TRUE;
               goto found_aid;
            }
         }
         else
         {
            /* Process all AID */
            PDebugTrace("PSecurityStackCheckAcl: Processing ACIE#%d (General AID rules)", pAcie->nAcieIndex);

            if (pAcie->nAidType == P_SECSTACK_AID_TYPE_ALL)
            {
               goto found_aid;
            }
         }

         continue; /* Skip to next ACIE */

      found_aid:

         switch(pAcie->pAce->nPrincipalType)
         {
            case P_SECSTACK_PRINCIPAL_TYPE_ALWAYS:
            {
               /* Access is granted */
               PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (All Principals matches)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto found_principal;
            }

            case P_SECSTACK_PRINCIPAL_TYPE_NEVER:
            {
               /* Access is denied */
               PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (All Principals) denies access", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto return_access_denied;
            }

            case P_SECSTACK_PRINCIPAL_TYPE_SPECIFIC:
            {
               /* Look for a matching principal ID */
               uint32_t i;
               for(i = 0; i < pAcie->pAce->nPrincipals; i++)
               {
                  if (static_PSecurityStackCheckPrincipalIdentity(
                     pContext,
                     pUserInstance,
                     nHalSlotIdentifier,
                     pAcie->pAce->pPrincipals[i].id,
                     P_SECSTACK_PRINCIPALID_LENGTH,
                     &cache) == W_SUCCESS)
                  {
                     /* Principal ID matches */
                     PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (Principal#%d matches)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex, i);
                     goto found_principal;
                  }
               }

               /* Principal ID does not match */
               PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (Principal not found)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               break;
            }

            default:
            {
               CDebugAssert(W_FALSE);
               goto return_access_denied;
            }
         }

         continue; /* Skip to next ACIE */

      found_principal:

         switch(pAcie->pAce->nPermissionType)
         {
            case P_SECSTACK_PERMISSION_TYPE_ALWAYS:
            {
               /* Access is allowed */
               PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (All APDU Permissions matches)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto found_permission;
            }

            case P_SECSTACK_PERMISSION_TYPE_NEVER:
            {
               /* Access is forbidden */
               PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (All APDU Permissions) denies access", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto return_access_denied;
            }

            case P_SECSTACK_PERMISSION_TYPE_SPECIFIC:
            {
               if (pApduHeader == null)
               {
                  /* This is the SELECT[AID] APDU command. */
                  PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (SELECT) matches", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
                  goto found_permission;
               }
               /* No need to check APDU permissions if access has already been granted */
               else if (bGrantAccess == W_FALSE)
               {
                  /* Look for a matching permission */
                  uint32_t i;
                  for(i = 0; i < pAcie->pAce->nPermissions; i++)
                  {
                     tPermission* pPermission = &pAcie->pAce->pPermissions[i];

                     uint8_t aMaskedApduHeader[P_SECSTACK_APDU_PERMISSION_LENGTH]; /* Length is assumed to be 4 though !!! */
                     aMaskedApduHeader[0] = (uint8_t)(pApduHeader[0] & pPermission->apduMask[0]);
                     aMaskedApduHeader[1] = (uint8_t)(pApduHeader[1] & pPermission->apduMask[1]);
                     aMaskedApduHeader[2] = (uint8_t)(pApduHeader[2] & pPermission->apduMask[2]);
                     aMaskedApduHeader[3] = (uint8_t)(pApduHeader[3] & pPermission->apduMask[3]);

                     if (CMemoryCompare(aMaskedApduHeader, pPermission->apduHeader, P_SECSTACK_APDU_PERMISSION_LENGTH) == 0)
                     {
                        /* APDU permission matches */
                        PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (APDU Permission#%d) matches", pAcie->nAcieIndex, pAcie->pAce->nAceIndex, i);
                        goto found_permission;
                     }
                  }
               }

               PDebugTrace("PSecurityStackCheckAcl: ACIE#%d, ACE#%d (APDU Permission not found)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               break;
            }

            default:
            {
               CDebugAssert(W_FALSE);
               goto return_access_denied;
            }
         }

         continue; /* Skip to next ACIE */

      found_permission:

         bGrantAccess = W_TRUE;
         continue; /* Skip to next ACIE */
      }

      if (bGrantAccess != W_FALSE)
      {
         if ((pApduHeader == null) && (bIsAnswerApdu == W_FALSE))
         {
            /* This is a SELECT[AID] command APDU */

            if (pAid == null)
            {
               /* This is the SELECT[AID=empty] command (not the answer). Do not install hook. */
               PDebugTrace("PSecurityStackCheckAcl: Access is granted (AID in returned FCI shall not be checked)");
               return W_SUCCESS;
            }
            else
            {
               /* This is a SELECT[AID!=empty] command (not the answer). Inform caller to install hook. */
               PDebugTrace("PSecurityStackCheckAcl: Access is granted (AID in returned FCI must be checked)");
               return W_ERROR_MISSING_INFO;
            }
         }

         PDebugTrace("PSecurityStackCheckAcl: Access is granted");
         return W_SUCCESS;
      }

      if (bFoundSpecificRule != W_FALSE)
      {
         /* Do not scan general rules if a specific rule was found */
         break;
      }
   }

   /* At this point, the access has neither been explictly granted nor denied */

#if 0 /* @todo */
   if ((pAid == null) && (bIsAnswerApdu == W_FALSE))
   {
      /* This is the SELECT[AID] command with an empty AID (P3='00'). Inform caller to install hook. */
      PDebugTrace("PSecurityStackCheckAcl: Access is granted (AID in returned FCI must be checked)");
      return W_ERROR_MISSING_INFO;
   }
#else/* 0 */
   if ((pApduHeader == null) && (bIsAnswerApdu == W_FALSE))
   {
      /* This is a SELECT[AID] command APDU */

      if ((pAid == null) && (pAcl->bManageDefaultSelectFci == W_FALSE))
      {
         /*
          * This is the SELECT[AID] command with an empty AID (P3='00') and the ACL does not manage Default AID.
          * Inform caller to install hook.
          */
         PDebugTrace("PSecurityStackCheckAcl: Access is granted (AID in returned FCI must be checked)");
         return W_ERROR_MISSING_INFO;
      }
   }
#endif /* 0*/

return_access_denied:

   PDebugTrace("PSecurityStackCheckAcl: Access is denied");
   return W_ERROR_SECURITY;
}

/* See header file */
W_ERROR PSecurityStackCheckAidAccessInternal(
   tContext* pContext,
   uint32_t nHalSlotIdentifier,
   tUserInstance* pUserInstance,
   tSecurityStackACL* pAcl,
   const uint8_t* pAid,
   uint32_t nAidLength,
   const uint8_t* pImpersonationDataBuffer,
   uint32_t nImpersonationDataBufferLength)
{
   bool_t bProcessGeneralRule;
   bool_t bFoundGeneralRule;

   PDebugTrace("PSecurityStackCheckAidAccessInternal()");

   if ((pAid == null) || ((nAidLength != P_SECSTACK_MFID_LENGTH) && ((nAidLength < P_SECSTACK_AID_MIN_LENGTH) || (nAidLength > P_SECSTACK_AID_MAX_LENGTH))))
   {
      PDebugError("PSecurityStackCheckAidAccessInternal: Invalid AID parameter");
      return W_ERROR_BAD_PARAMETER;
   }

   PDebugTrace("PSecurityStackCheckAidAccessInternal: AID {");
   PDebugTraceBuffer(pAid, nAidLength);

   if (pAcl == null)
   {
      PDebugError("PSecurityStackCheckAidAccessInternal: Invalid ACL parameter");
      return W_ERROR_SECURITY;
   }

   CDebugAssert((W_FALSE == 0) && (W_TRUE == (W_FALSE + 1)));
   for(bProcessGeneralRule = bFoundGeneralRule = W_FALSE; bProcessGeneralRule == bFoundGeneralRule; bProcessGeneralRule++)
   {
      tACIE* pAcie = (tACIE*)null;
      bool_t bFoundSpecificRule = W_FALSE;
      bool_t bGrantAccess = W_FALSE;

      /*
       * Scan all ACIE
       * - First loop is for the default AID or a specific AID.
       * - Second loop is for all AID
       */
      for(pAcie = pAcl->pFirstAcie; pAcie != null; pAcie = pAcie->pNextAcie)
      {
         if (bProcessGeneralRule == W_FALSE)
         {
            /* Process the default/specific AID */
            PDebugTrace("PSecurityStackCheckAidAccessInternal: Processing ACIE#%d (Default/specific AID rules)", pAcie->nAcieIndex);

            if (pAcie->nAidType == P_SECSTACK_AID_TYPE_ALL)
            {
               bFoundGeneralRule = W_TRUE;
            }
            else if ((pAcie->nAidType == P_SECSTACK_AID_TYPE_DEFAULT) && (pAid == null))
            {
               /* Default AID matches */
               bFoundSpecificRule = W_TRUE;
               goto found_aid;
            }
            else if ((pAcie->nAidType == P_SECSTACK_AID_TYPE_SPECIFIC) && (pAid != null)
                  && (pAcie->nAidLength == nAidLength) && (CMemoryCompare(pAcie->aAid, pAid, nAidLength) == 0))
            {
               /* Specific AID matches */
               bFoundSpecificRule = W_TRUE;
               goto found_aid;
            }
         }
         else
         {
            /* Process all AID */
            PDebugTrace("PSecurityStackCheckAidAccessInternal: Processing ACIE#%d (General AID rules)", pAcie->nAcieIndex);

            if (pAcie->nAidType == P_SECSTACK_AID_TYPE_ALL)
            {
               goto found_aid;
            }
         }

         continue; /* Skip to next ACIE */

      found_aid:

         switch(pAcie->pAce->nPrincipalType)
         {
            case P_SECSTACK_PRINCIPAL_TYPE_ALWAYS:
            {
               /* Access is granted */
               PDebugTrace("PSecurityStackCheckAidAccessInternal: ACIE#%d, ACE#%d (All Principals matches)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto found_principal;
            }

            case P_SECSTACK_PRINCIPAL_TYPE_NEVER:
            {
               /* Access is denied */
               PDebugTrace("PSecurityStackCheckAidAccessInternal: ACIE#%d, ACE#%d (All Principals) denies access", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto return_access_denied;
            }

            case P_SECSTACK_PRINCIPAL_TYPE_SPECIFIC:
            {
               /* Look for a matching principal ID */
               uint32_t i;
               for(i = 0; i < pAcie->pAce->nPrincipals; i++)
               {
                  if (PSecurityManagerDriverCheckIdentity(
                     pContext,
                     pUserInstance,
                     nHalSlotIdentifier,
                     pAcie->pAce->pPrincipals[i].id,
                     P_SECSTACK_PRINCIPALID_LENGTH,
                     pImpersonationDataBuffer, nImpersonationDataBufferLength) == W_SUCCESS)
                  {
                     /* Principal ID matches */
                     PDebugTrace("PSecurityStackCheckAidAccessInternal: ACIE#%d, ACE#%d (Principal#%d matches)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex, i);
                     goto found_principal;
                  }
               }

               /* Principal ID does not match */
               PDebugTrace("PSecurityStackCheckAidAccessInternal: ACIE#%d, ACE#%d (Principal not found) denies access", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               break;
            }

            default:
            {
               CDebugAssert(W_FALSE);
               goto return_access_denied;
            }
         }

         continue; /* Skip to next ACIE */

      found_principal:

         switch(pAcie->pAce->nPermissionType)
         {
            case P_SECSTACK_PERMISSION_TYPE_ALWAYS:
            {
               /* Access is allowed */
               PDebugTrace("PSecurityStackCheckAidAccessInternal: ACIE#%d, ACE#%d (All APDU Permissions matches)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto found_permission;
            }

            case P_SECSTACK_PERMISSION_TYPE_NEVER:
            {
               /* Access is forbidden */
               PDebugTrace("PSecurityStackCheckAidAccessInternal: ACIE#%d, ACE#%d (All APDU Permissions) denies access", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto return_access_denied;
            }

            case P_SECSTACK_PERMISSION_TYPE_SPECIFIC:
            {
               /* Access is allowed */
               PDebugTrace("PSecurityStackCheckAidAccessInternal: ACIE#%d, ACE#%d (Specific APDU Permissions matches)", pAcie->nAcieIndex, pAcie->pAce->nAceIndex);
               goto found_permission;
            }

            default:
            {
               CDebugAssert(W_FALSE);
               goto return_access_denied;
            }
         }

         continue; /* Skip to next ACIE */

      found_permission:

         bGrantAccess = W_TRUE;
         continue; /* Skip to next ACIE */
      }

      if (bGrantAccess != W_FALSE)
      {
         PDebugTrace("PSecurityStackCheckAidAccessInternal: Access is granted");
         return W_SUCCESS;
      }

      if (bFoundSpecificRule != W_FALSE)
      {
         /* Do not scan general rules if a specific rule was found */
         break;
      }
   }

return_access_denied:

   PDebugTrace("PSecurityStackCheckAidAccessInternal: Access is denied");
   return W_ERROR_SECURITY;
}

static void static_PSecurityStackAclFormatBytes(const uint8_t* pData, uint32_t nData, char* psOutput)
{
   static const char sHex[]="0123456789ABCDEF";
   while(nData-->0)
   {
      *psOutput++ = sHex[ (*pData >> 4) & 0x0F ];
      *psOutput++ = sHex[ (*pData     ) & 0x0F ];
      *psOutput++ = ' ';
      pData++;
   }
   psOutput--;
   *psOutput = '\0';
}


/** See header file */
void PSecurityStackAclDumpContents(
   tSecurityStackACL* pAcl)
{
   tACIE* pAcie;

   PDebugTrace("PSecurityStackAclDumpContents");

   if (pAcl == null)
   {
      return;
   }

   PDebugTrace("PSecurityStackAclDumpContents: Starting dump...");

   if (pAcl->bManageDefaultSelectFci != W_FALSE)
   {
      PDebugTrace("ACL: Default AID rules are active");
   }

   for(pAcie = pAcl->pFirstAcie; pAcie != null; pAcie = pAcie->pNextAcie)
   {
      tACE* pAce = pAcie->pAce;

      switch(pAcie->nAidType)
      {
         case P_SECSTACK_AID_TYPE_ALL:
         {
            PDebugTrace("ACIE #%d: All AID", pAcie->nAcieIndex);
            break;
         }

         case P_SECSTACK_AID_TYPE_DEFAULT:
         {
            PDebugTrace("ACIE #%d: Default AID", pAcie->nAcieIndex);
            break;
         }

         case P_SECSTACK_AID_TYPE_SPECIFIC:
         {
            char szAid[48];
            static_PSecurityStackAclFormatBytes(pAcie->aAid, pAcie->nAidLength, szAid);
            PDebugTrace("ACIE #%d: AID = %s", pAcie->nAcieIndex, szAid);
            break;
         }

         default:
         {
            CDebugAssert(W_FALSE);
            break;
         }
      }

      PDebugTrace("--> ACE #%d: File='%04X', index=%d, length=%d", pAce->nAceIndex, pAce->nFid, pAce->nIndex, pAce->nLength);

      switch(pAce->nPrincipalType)
      {
         case P_SECSTACK_PRINCIPAL_TYPE_ALWAYS:
         {
            PDebugTrace("    --> Principals: All terminal applications are allowed");
            break;
         }

         case P_SECSTACK_PRINCIPAL_TYPE_NEVER:
         {
            PDebugTrace("    --> Principals: All terminal applications are forbidden");
            break;
         }

         case P_SECSTACK_PRINCIPAL_TYPE_SPECIFIC:
         {
            uint32_t nPrincipal;
            PDebugTrace("    --> Principals: The following terminal application hashes are allowed:");
            for(nPrincipal = 0; nPrincipal < pAce->nPrincipals; nPrincipal++)
            {
               char szHash[20*3];
               static_PSecurityStackAclFormatBytes(pAce->pPrincipals[nPrincipal].id, P_SECSTACK_PRINCIPALID_LENGTH, szHash);
               PDebugTrace("        #%d = %s", nPrincipal, szHash);
            }
            break;
         }

         default:
         {
            CDebugAssert(W_FALSE);
            break;
         }
      }

      switch(pAce->nPermissionType)
      {
         case P_SECSTACK_PERMISSION_TYPE_ALWAYS:
         {
            PDebugTrace("    --> Permissions: All APDU commands are allowed");
            break;
         }

         case P_SECSTACK_PERMISSION_TYPE_NEVER:
         {
            PDebugTrace("    --> Permissions: All APDU commands are forbidden");
            break;
         }

         case P_SECSTACK_PERMISSION_TYPE_SPECIFIC:
         {
            uint32_t nPermission;
            PDebugTrace("    --> Permissions: The following APDU commands are allowed:");
            for(nPermission = 0; nPermission < pAce->nPermissions; nPermission++)
            {
               char szApdu[4*3], szMask[4*3];
               static_PSecurityStackAclFormatBytes(pAce->pPermissions[nPermission].apduHeader, P_SECSTACK_APDU_PERMISSION_LENGTH, szApdu);
               static_PSecurityStackAclFormatBytes(pAce->pPermissions[nPermission].apduMask, P_SECSTACK_APDU_PERMISSION_LENGTH, szMask);
               PDebugTrace("        #%d = %s (mask = %s)", nPermission, szApdu, szMask);
            }
            break;
         }

         default:
         {
            CDebugAssert(W_FALSE);
            break;
         }
      }
   }

   PDebugTrace("PSecurityStackAclDumpContents: End");
}

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */
