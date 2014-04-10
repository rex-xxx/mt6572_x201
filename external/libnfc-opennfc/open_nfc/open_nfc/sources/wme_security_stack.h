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

#ifndef __WME_SECURITY_STACK_H
#define __WME_SECURITY_STACK_H

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

/** A Security Stack instance is actually an ISO-7816 State Machine instance */
#define __tSecurityStackInstance __tP7816SmInstance

/** Opaque data structure for a Security Stack Instance */
typedef struct __tSecurityStackInstance tSecurityStackInstance;

/* The policy update strategies */
#define P_SECSTACK_UPDATE_MASTER                      0
#define P_SECSTACK_UPDATE_SLAVE_NO_NOTIFICATION       1
#define P_SECSTACK_UPDATE_SLAVE_WITH_NOTIFICATION     2

/** @brief AID of the PKCS#15 application */
extern const uint8_t g_aSecurityStackPkcs15Aid[];

/** @brief Size of the AID of the PKCS#15 application */
extern const uint32_t g_nSecurityStackPkcs15AidSize;

/**
 * @brief   Creates a security stack instance.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nHalSlotIdentifier  The Secure Element slot identifier.
 *
 * @param[in]  pSeSmInterface  The interface to the Secure Element ISO-7816 State Machine.
 *
 * @param[in]  pSeSmInstance  The instance of the Secure Element ISO-7816 State Machine.
 *
 * @param[in]  pDefaultPrincipalList  A pointer on the list of default values for the principal.
 *             This value may be null if no default value is defined.
 *
 * @param[in]  nDefaultPrincipalNumber  The number of default principal value in the list.
 *             This value is zero if pDefaultPrincipalList is null.
 *
 * @param[in]  nUpdateStrategy  The update strategy:
 *               - P_SECSTACK_UPDATE_MASTER,
 *               - P_SECSTACK_UPDATE_SLAVE_NO_NOTIFICATION,
 *               - P_SECSTACK_UPDATE_SLAVE_WITH_NOTIFICATION.
 *
 * @param[in]  bIsUicc  Flag indicating if the secure element is a UICC.
 *
 * @return   The security stack instance or null in case of error.
 **/
tSecurityStackInstance* PSecurityStackCreateInstance(
            tContext* pContext,
            uint32_t nHalSlotIdentifier,
            tP7816SmInterface* pSeSmInterface,
            tP7816SmInstance* pSeSmInstance,
            const tCSecurityDefaultPrincipal* pDefaultPrincipalList,
            uint32_t nDefaultPrincipalNumber,
            uint32_t nUpdateStrategy,
            bool_t bIsUicc );

/**
 * @brief   Destroys a security stack instance.
 *
 * @post  PSecurityStackDestroyInstance() does not return any error.
 *        The caller should always assume that the security stack instance
 *        is destroyed after this call.
 *
 * @post  The caller should never re-use the security stack instance value.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance to destroy.
 *             If null, the function does nothing and return.
 **/
void PSecurityStackDestroyInstance(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance );

/**
 * @brief The default instance of the Security Stack State Machine interface.
 *
 * All pointer-to-function data fields are guaranteed to be non-null pointers.
 **/
extern tP7816SmInterface g_PSecurityStackSmInterface;

/**
 * @brief Sets the current user instance.
 *
 * The user instance must be set before each call to the APDU exchange function exposed
 * by the Security Stack.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance.
 *
 * @param[in]  pUserInstance  The user instance.
 *
 * @return One of the following error codes:
 *         - W_SUCCESS  The function completed successfully.
 *         - W_ERROR_BAD_PARAMETER  A passed parameter is invalid.
 */
W_ERROR PSecurityStackSetCurrentUserInstance(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance,
            tUserInstance* pUserInstance);

/**
 * @brief Requests the Security Stack to load the Access Control List from the Secure Element.
 *
 * This function is called once during the initialization of the stack.
 *
 * The callback function is always called whether the operation is completed or if an error occurred.
 *
 * If the security stack does no find the PKCS#15 applet, the function returns W_SUCCESS.
 * Then the security control is deactivated.
 *
 * If any error is returned, the Security Stack will lock every access to the Secure Element,
 * except for the application(s) matching the default principal.
 *
 * PSecurityStackLoadAcl() does not return any error.
 * The following error codes are returned in the callback function:
 *   - W_SUCCESS  The operation completed successfully.
 *   - W_ERROR_BAD_STATE An operation is already pending on this connection.
 *   - W_ERROR_CONNECTION_COMPATIBILITY  The card is not compliant with ISO 7816 part 4.
 *   - W_ERROR_SECURITY  The version of the PKCS#15 applet is not compliant with the stack.
 *   - W_ERROR_RF_COMMUNICATION An error is detected in the protocol used by the card.
 *   - W_ERROR_TIMEOUT  A timeout occurred during the communication with the card.
 *   - other if any other error occurred.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance.
 *
 * @param[in]  pCallback  A pointer on the callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter transmitted to the callback function.
 **/
void PSecurityStackLoadAcl(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter );

/**
 * @brief Notifies that the Secure Element has been reset.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance.
 **/
void PSecurityStackNotifySecureElementReset(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance);

/**
 * @brief Checks the access rights for a given AID.
 *
 * The callback function is always called whether the operation is completed or if an error occurred.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance.
 *
 * @param[in]  pUserInstance  The current user instance.
 *
 * @param[in]  pAidBuffer  The buffer containing the application AID. This value should not be  null.
 *
 * @param[in]  nAidLength  The application AID length in bytes (between 5 and 16 bytes inclusive).
 *             This value should not be zero.
 *
 * @param[in]  pImpersonationDataBuffer  The buffer containing the impersonation data
 *             for the authentication. Null if impersonation is not requested.
 *
 * @param[in]  nImpersonationDataBufferLength  The length in bytes of the impersonation data.
 *             Zero if impersonation is not requested.
 *
 * @return W_SUCCESS if the access is granted, W_ERROR_SECURITY if access is denied.
 **/
W_ERROR PSecurityStackCheckAidAccess(
            tContext* pContext,
            tSecurityStackInstance* pSecurityStackInstance,
            tUserInstance* pUserInstance,
            const uint8_t* pAidBuffer,
            uint32_t nAidLength,
            const uint8_t* pImpersonationDataBuffer,
            uint32_t nImpersonationDataBufferLength );

/**
 * Notifies the reception of a STK REFRESH command.
 *
 * The format of the file list is the following (defined in the Files parameter
 * in section 8.18 File List of the ETSI specification):
 *  - full paths are given to files. Each of these shall be at least 4 octets in length (e.g. '3F002FE2' or '3F007F106F3A').
 *    Each entry in the file description is composed of two bytes, where the first byte identifies
 *    the type of file (see TS 102 221 [1] or TS 151 011 [8]);
 *  - an entry in the file description shall therefore always begin with '3FXX'.
 *    There can be any number of Dedicated File entries between the Master File and Elementary File.
 *    There shall be no delimiters between files, as this is implied by the fact that the full path
 *    to any EF starts with '3FXX' and ends with an Elementary type file.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance.
 *
 * @param[in]  nCommand The STK REFRESH command code.
 *
 * @param[in]  pRefreshFileList  The buffer containing the refresh file list of the STK REFRESH command.
 *
 * @param[in]  nRefreshFileListLength  The file list length in bytes. May be null if no file list is included in the command.
 **/
void PSecurityStackNotifyStkRefresh(
         tContext* pContext,
         tSecurityStackInstance* pSecurityStackInstance,
         uint32_t nCommand,
         const uint8_t* pRefreshFileList,
         uint32_t nRefreshFileListLength);

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY*/
#endif /* __WME_SECURITY_STACK_H */
