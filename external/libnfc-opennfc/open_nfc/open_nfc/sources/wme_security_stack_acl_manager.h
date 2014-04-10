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

#ifndef __WME_SECURITY_STACK_ACL_MANAGER_H
#define __WME_SECURITY_STACK_ACL_MANAGER_H
#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

/** length of Master File ID */
#define P_SECSTACK_MFID_LENGTH 2

/** Minimum length of an AID */
#define P_SECSTACK_AID_MIN_LENGTH 5

/** Maximum length of an AID */
#define P_SECSTACK_AID_MAX_LENGTH 16

/** Length of a Principal ID */
#define P_SECSTACK_PRINCIPALID_LENGTH 20

/** Length of an APDU permission */
#define P_SECSTACK_APDU_PERMISSION_LENGTH 4

/** Structure for a Principal identifier */
typedef struct __tPrincipal
{
   /** The 20-byte SHA-1 hash of the Principal */
   uint8_t id[P_SECSTACK_PRINCIPALID_LENGTH];
}  tPrincipal;

/** Structure for an APDU permission */
typedef struct __tPermission
{
   /** The APDU header (4 bytes) */
   uint8_t apduHeader[P_SECSTACK_APDU_PERMISSION_LENGTH];
   /** The APDU mask (4 bytes) */
   uint8_t apduMask[P_SECSTACK_APDU_PERMISSION_LENGTH];
}  tPermission;

/** Opaque structure representing an Access Control Entry */
typedef struct __tACE tACE;

/** Opaque structure representing an Access Control List */
typedef struct __tSecurityStackACL tSecurityStackACL;

/** Constant used to specific "all AID" in the call to PSecurityStackAclCreateAcie */
#define P_SECSTACK_BUFFER_AID_ALL ((const uint8_t*)null)
/** Constant used to specific "all AID" in the call to PSecurityStackAclCreateAcie */
#define P_SECSTACK_LENGTH_AID_ALL ((uint32_t)(-1))

/** All terminal applications are granted */
#define P_SECSTACK_PRINCIPAL_TYPE_ALWAYS 0
/** All terminal applications are denied */
#define P_SECSTACK_PRINCIPAL_TYPE_NEVER 1
/** Only specific terminal applications are granted */
#define P_SECSTACK_PRINCIPAL_TYPE_SPECIFIC 2

/** All APDU commands are allowed */
#define P_SECSTACK_PERMISSION_TYPE_ALWAYS 0
/** All APDU commands are forbidden */
#define P_SECSTACK_PERMISSION_TYPE_NEVER 1
/** Only specific APDU commands are allowed */
#define P_SECSTACK_PERMISSION_TYPE_SPECIFIC 2

/**
 * @brief Creates an instance of an ACL
 *
 * @param[in]  bManageDefaultSelectFci  Whether default AID rules are managed.
 *
 * @param[out]  ppAcl  The newly created ACL.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The ACL has been successfully created and returned.
 *          - W_ERROR_BAD_PARAMETER  ppAcl is null.
 */
W_ERROR PSecurityStackAclCreateInstance(
   bool_t bManageDefaultSelectFci,
   tSecurityStackACL** ppAcl);

/**
 * @brief Indicates whether the ACL can manage default AID.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  Default AID are managed.
 *          - W_ERROR_BAD_STATE  Default AID are not managed.
 *          - W_ERROR_BAD_PARAMETER  pAcl is null.
 */
W_ERROR PSecurityStackIsDefaultAidSupported(
   tSecurityStackACL* pAcl);

/**
 * @brief Creates an Access Control Index Entry.
 * The new ACIE is stored in the ACL.
 *
 * The pAid/nAidLength parameters can take the values null/0 to indicate the default AID,
 * or P_SECSTACK_BUFFER_AID_ALL/P_SECSTACK_LENGTH_AID_ALL to indicate all AID.
 *
 * @param[in]  pAcl  The ACL.
 *
 * @param[in]  pAid  The AID buffer.
 *
 * @param[in]  nAidLength  The length of the AID.
 *
 * @param[in]  pAce  The ACE that is associated with the ACIE.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The ACIE has been successfully created.
 *          - W_ERROR_BAD_PARAMETER  A passed parameter is invalid.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
W_ERROR PSecurityStackAclCreateAcie(
   tSecurityStackACL* pAcl,
   const uint8_t* pAid,
   uint32_t nAidLength,
   tACE* pAce);

/**
 * @brief Creates a new or returns an existing Access Control Entry.
 * An ACE is fully identified by its FID/index/length. The new ACE, if created, is stored in the ACL.
 *
 * By default, all terminal applications associated with the ACE are denied. To grant access to all
 * or specific applications, the PSecurityStackAclCreateAcePrincipals function must be used.
 *
 * By default, and all APDU commands associated with the ACE are forbidden. To allow all or specific
 * APDU commands, the PSecurityStackAclCreateAcePermissions function must be used.
 *
 * @param[in]  pAcl  The ACL.
 *
 * @param[in]  nAceFid  The FID of the file containing the ACE.
 *
 * @param[in]  nAceIndex  The starting offset within the ACE file.
 *
 * @param[in]  nAceLength  The length of the ACE.
 *
 * @param[out]  ppAce  The newly created ACE.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The existing ACE has been successfully returned.
 *          - W_ERROR_ITEM_NOT_FOUND  The ACE has been successfully created and returned.
 *          - W_ERROR_BAD_PARAMETER  A passed parameter is invalid.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
W_ERROR PSecurityStackAclCreateAce(
   tSecurityStackACL* pAcl,
   uint32_t nAceFid,
   uint32_t nAceIndex,
   uint32_t nAceLength,
   tACE** ppAce);

/**
 * @brief Associate Principal hashes to an ACE.
 *
 * This function creates the buffer used to store the Principal hashes of the terminal
 * applications for which access must be granted.
 *
 * If the nPrincipals parameter is 0 (and ppPrincipals is null), then access shall be granted
 * to all terminal applications.
 * If the nPrincipals parameter is greater than 0 (and ppPrincipals is not null), then access
 * shall be granted to the terminal applications whose hashes are to be written to the buffer
 * returned in ppPrincipals and that can hold "nPrincipals" 20-byte hashes (this buffer shall
 * initially contain zeroes).
 *
 * @param[in]  pAcl  The ACL.
 *
 * @param[in]  pAce  The ACE.
 *
 * @param[in]  nPrincipals  The number of Principals.
 *
 * @param[in]  ppPrincipals  The returned buffer returned to store Principal hashes.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The function succeeded.
 *          - W_ERROR_BAD_PARAMETER  A passed parameter is invalid.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
W_ERROR PSecurityStackAclCreateAcePrincipals(
   tSecurityStackACL* pAcl,
   tACE* pAce,
   uint32_t nPrincipals,
   tPrincipal** ppPrincipals);

/**
 * @brief Associate APDU Permissions to an ACE.
 *
 * This function creates the buffer used to store the APDU Permissions of the APDU commands
 * which must be allowed.
 *
 * If the nPermissions parameter is 0 (and ppPermissions is null), then all APDU commands
 * shall be allowed.
 * If the nPermissions parameter is greater than 0 (and ppPermissions is not null), then it
 * shall be allowed to send only the APDU commands that are to be written to the buffer
 * returned in ppPermissions and that can hold "nPermissions" entries (this buffer shall
 * initially contain zeroes).
 *
 * @param[in]  pAcl  The ACL.
 *
 * @param[in]  pAce  The ACE.
 *
 * @param[in]  nPermissions  The number of APDU permissions.
 *
 * @param[in]  ppPermissions  The returned buffer returned to store APDU permissions.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The function succeeded.
 *          - W_ERROR_BAD_PARAMETER  A passed parameter is invalid.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
W_ERROR PSecurityStackAclCreateAcePermissions(
   tSecurityStackACL* pAcl,
   tACE* pAce,
   uint32_t nPermissions,
   tPermission** ppPermissions);

/**
 * @brief Checks an AID and an APDU against the ACL.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier.
 *
 * @param[in]  pUserInstance  The current user instance.
 *
 * @param[in]  pAcl  The ACL.
 *
 * @param[in]  pAid  The AID of the current or being selected application in the SE.
 *
 * @param[in]  nAidLength  The length in bytes of the AID.
 *
 * @param[in]  pApduHeader  A pointer to the first four bytes of the APDU to be checked.
 *             May be null in case only the AID needs to be checked.
 *
 * @param[in]  bIsAnswerApdu  Whether the AID returned in the answer to SELECT is to be
 *             checked. Parameter pApduHeader must be null.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The access if granted.
 *          - W_ERROR_BAD_PARAMETER  A passed parameter is invalid.
 *          - W_ERROR_SECURITY  The access is denied.
 */
W_ERROR PSecurityStackCheckAcl(
   tContext* pContext,
   uint32_t nHalSlotIdentifier,
   tUserInstance* pUserInstance,
   tSecurityStackACL* pAcl,
   const uint8_t* pAid,
   uint32_t nAidLength,
   const uint8_t* pApduHeader,
   bool_t bIsAnswerApdu);

/**
 * @brief Checks an AID access against the ACL.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier.
 *
 * @param[in]  pUserInstance  The current user instance.
 *
 * @param[in]  pAcl  The ACL.
 *
 * @param[in]  pAid  The AID of the current or being selected application in the SE.
 *
 * @param[in]  nAidLength  The length in bytes of the AID.
 *
 * @param[in]  pImpersonationDataBuffer  The buffer containing the impersonation data
 *             for the authentication. Null if impersonation is not requested.
 *
 * @param[in]  nImpersonationDataBufferLength  The length in bytes of the impersonation data.
 *             Zero if impersonation is not requested.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The access if granted.
 *          - W_ERROR_BAD_PARAMETER  A passed parameter is invalid.
 *          - W_ERROR_SECURITY  The access is denied.
 */
W_ERROR PSecurityStackCheckAidAccessInternal(
   tContext* pContext,
   uint32_t nHalSlotIdentifier,
   tUserInstance* pUserInstance,
   tSecurityStackACL* pAcl,
   const uint8_t* pAid,
   uint32_t nAidLength,
   const uint8_t* pImpersonationDataBuffer,
   uint32_t nImpersonationDataBufferLength);

/**
 * @brief Frees an ACL.
 *
 * @param[in]  pAcl  The ACL.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The ACL has been freed.
 *          - W_ERROR_BAD_PARAMETER  The pAcl parameter is null.
 */
W_ERROR PSecurityStackAclDestroyInstance(
   tSecurityStackACL* pAcl);

/**
 * @brief Dumps the contents of an ACL (for debugging purpose).
 *
 * @param[in]  pAcl  The ACL.
 */
void PSecurityStackAclDumpContents(
   tSecurityStackACL* pAcl);

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */
#endif /* __WME_SECURITY_STACK_ACL_MANAGER_H */
