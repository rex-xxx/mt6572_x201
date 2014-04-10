/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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

#ifndef __WME_SECURITY_STACK_ACL_PROCESSOR_H
#define __WME_SECURITY_STACK_ACL_PROCESSOR_H

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

#include "wme_security_stack_acl_manager.h"

/** @brief Opaque data structure for the ACL Processor instance */
typedef struct __tPSecurityStackAclProcessorInstance tPSecurityStackAclProcessorInstance;

/**
 * @brief Callback notifying the result of an APDU exchange.
 *
 * @param[in]  The pExchangeApduCallbackParameter1 parameter passed to tPSecurityStackAclProcessorExchangeApdu.
 *
 * @param[in]  The pExchangeApduCallbackParameter2 parameter passed to tPSecurityStackAclProcessorExchangeApdu.
 *
 * @param[in]  nDataLength  The length of the APDU response.
 *
 * @param[in]  nResult  An error code, among which:
 *               - W_SUCCESS, to indicate that the APDU exchange has succeeded.
 *               - Any other error code specific to the ACL Processor.
 */
typedef void tPSecurityStackAclProcessorExchangeApduCallbackFunction(
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  uint32_t nDataLength,
                  W_ERROR nResult);

/**
 * @brief Exchanges an APDU command with a Secure Element.
 * See tPSecurityStackAclProcessorReadAcl.
 *
 * @param[in]  pCallbackParameter1  The pExchangeApduCallbackParameter1 parameter passed to tPSecurityStackAclProcessorReadAcl.
 *
 * @param[in]  pCallbackParameter2  The pExchangeApduCallbackParameter2 parameter passed to tPSecurityStackAclProcessorReadAcl.
 *
 * @param[in]  pExchangeApduCallback  The callback used to notify the result of the APDU exchange.
 *
 * @param[in]  pExchangeApduCallbackParameter1  The parameter #1 passed to pExchangeApduCallback.
 *
 * @param[in]  pExchangeApduCallbackParameter2  The parameter #2 passed to pExchangeApduCallback.
 *
 * @param[in]  pSendApduBuffer  The APDU command to be sent.
 *
 * @param[in]  nSendApduBufferLength  The length of the APDU command.
 *
 * @param[in]  pReceivedApduBuffer  The buffer receiving the APDU response.
 *
 * @param[in]  nReceivedApduBufferMaxLength  The maximum length of the receiving buffer.
 *
 * @result  An error code, among which:
 *    - W_ERROR_OPERATION_PENDING, to indicate that the APDU exchange has started.
 *    - Any other error code specific to the ACL Processor.
 */
typedef W_ERROR tPSecurityStackAclProcessorExchangeApdu(
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  tPSecurityStackAclProcessorExchangeApduCallbackFunction* pExchangeApduCallback,
                  void* pExchangeApduCallbackParameter1,
                  void* pExchangeApduCallbackParameter2,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength);

/**
 * Creates a new ACL Processor instance.
 *
 * @param[in]  pAnswerToSelectBuffer  The answer to the SELECT command (without status word)
 *                that was used to select the PKCS#15 application.
 *
 * @param[in]  nAnswerToSelectLength  The length of the answer to SELECT command.
 *
 * @param[in]  bIsSelectedByAid  Whether the PKCS#15 application has been selected by AID.
 *
 * @param[in]  bAutoResetTouch  Wheter the touched flag is automatically reset.
 *
 * @param[out]  ppAclProcessorInstance  The returned ACL processor instance.
 *
 * @result  An error code, among which:
 *    - W_SUCCESS, to indicate success.
 *    - W_ERROR_BAD_PARAMETER, if a parameter is invalid.
 *    - W_ERROR_OUT_OF_RESOURCE, in case of an out-of-memory condition.
 *    - W_ERROR_CONNECTION_COMPATIBILITY, if the ACL Processor does not recognize the answer to SELECT.
 */
typedef W_ERROR tPSecurityStackAclProcessorCreateInstance(
                  const uint8_t* pAnswerToSelect,
                  uint32_t nAnswerToSelectLength,
                  bool_t bIsSelectedByAid,
                  bool_t bAutoResetTouch,
                  tPSecurityStackAclProcessorInstance** ppAclProcessorInstance);

/**
 * @brief Checks whether the ACL needs to be updated.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pAnswerToSelectBuffer  The answer to the SELECT command (without status word)
 *                that was used to select the PKCS#15 application.
 *
 * @param[in]  nAnswerToSelectLength  The length of the answer to SELECT command.
 *
 * @param[in]  bIsSelectedByAid  Whether the PKCS#15 application has been selected by AID.
 *
 * @param[out]  ppAclProcessorInstance  The returned ACL processor instance.
 *
 * @result  An error code, among which:
 *    - W_SUCCESS, to indicate success.
 *    - W_ERROR_BAD_PARAMETER, if a parameter is invalid.
 *    - W_ERROR_CONNECTION_COMPATIBILITY, if the ACL Processor does not recognize the answer to SELECT.
 */
typedef W_ERROR tPSecurityStackAclProcessorUpdateInstance(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pAnswerToSelect,
                  uint32_t nAnswerToSelectLength,
                  bool_t bIsSelectedByAid);

/**
 * @brief Callback used to notify the result of an ACL reading. See tPSecurityStackAclProcessorReadAcl.
 *
 * If nResult is W_SUCCESS and pAcl is null, the ACL did not change in the Secure Element since the last ACL reading.
 * If nResult is W_SUCCESS and pAcl is not null, the ACL changed and the new ACL is returned.
 * If nResult is not W_SUCCESS, the ACL reading failed (and pAcl is null).
 *
 * @param[in]  pCallbackParameter1  The parameter #1 passed to tPSecurityStackAclProcessorReadAcl.
 *
 * @param[in]  pCallbackParameter2  The parameter #2 passed to tPSecurityStackAclProcessorReadAcl.
 *
 * @param[in]  pAcl  The read ACL.
 *
 * @param[in]  nResult  An error code.
 *
 * @result  An error code, among which:
 *    - W_SUCCESS, to indicate success.
 *    - Any other error code returned by the ACL reading function.
 */
typedef void tPSecurityStackAclProcessorReadAclCallbackFunction(
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  tSecurityStackACL* pAcl,
                  W_ERROR nResult);

/**
 * @brief Reads the ACL from a Secure Element.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pExchangeApdu  The function used to exchange an APDU command with the Secure Element.
 *
 * @param[in]  pExchangeApduCallbackParameter1  The parameter #1 to be passed to pExchangeApdu.
 *
 * @param[in]  pExchangeApduCallbackParameter2  The parameter #2 to be passed to pExchangeApdu.
 *
 * @param[in]  pCallback  The function used to notify the result of the ACL reading.
 *
 * @param[in]  pCallbackParameter1  The parameter #1 to be passed to pCallback.
 *
 * @param[in]  pCallbackParameter2  The parameter #2 to be passed to pCallback.
 *
 * @param[in]  nNow  The current time in ms.
 *
 * @result  An error code, among which:
 *    - W_SUCCESS, to indicate success.
 *    - W_ERROR_BAD_PARAMETER, if a parameter is invalid.
 */
typedef W_ERROR tPSecurityStackAclProcessorReadAcl(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  tPSecurityStackAclProcessorExchangeApdu pExchangeApdu,
                  void* pExchangeApduCallbackParameter1,
                  void* pExchangeApduCallbackParameter2,
                  tPSecurityStackAclProcessorReadAclCallbackFunction pCallback,
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  uint32_t nNow);

/**
 * @brief Filters APDU commands sent to the Secure Element. This function may be used
 * to intercept commands sent to the PKCS#15 application and thus determine whether
 * the ACL has been updated and should be read again.
 * The error code returned by this function should be ignored by the caller.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pAid  The AID.
 *
 * @param[in]  nAidLength  The length of the AID.
 *
 * @param[in]  pApdu  The APDU command.
 *
 * @param[in]  nApduLength  The length of the APDU command.
 *
 * @result  An error code, among which:
 *    - W_SUCCESS, to indicate success.
 *    - Any other error code specific to the ACL Processor.
 */
typedef W_ERROR tPSecurityStackAclProcessorFilterApdu(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pAid,
                  uint32_t nAidLength,
                  const uint8_t* pApdu,
                  uint32_t nApduLength);

/**
 * @brief Processes the STK REFRESH command.
 * See PSecurityStackNotifyStkRefresh
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  nCommand The STK REFRESH command code.
 *
 * @param[in]  pRefreshFileList  The buffer containing the refresh file list of the STK REFRESH command.
 *
 * @param[in]  nRefreshFileListLength  The file list length in bytes. May be null if no file list is included in the command.
 *
 * @result  An error code, among which:
 *    - W_SUCCESS, to indicate success.
 *    - W_ERROR_BAD_PARAMETER, if a parameter is invalid.
 */
typedef W_ERROR tPSecurityStackAclProcessorNotifyStkRefresh(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  uint32_t nCommand,
                  const uint8_t* pRefreshFileList,
                  uint32_t nRefreshFileListLength);

/**
 * @brief Destroys an ACL Processor instance.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @result  An error code, among which:
 *    - W_SUCCESS, to indicate success.
 *    - W_ERROR_BAD_PARAMETER, if pAclProcessorInstance is null.
 */
typedef W_ERROR tPSecurityStackAclProcessorDestroyInstance(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance);

/** @brief Data structure for the ACL Processor interface */
typedef struct __tPSecurityStackAclProcessorInterface
{
   /** The function used to create an instance of the ACL processor */
   tPSecurityStackAclProcessorCreateInstance* pCreateInstance;
   /** The function used to update an instance of the ACL processor (refresh) */
   tPSecurityStackAclProcessorUpdateInstance* pUpdateInstance;
   /** The function used to read the ACL from the Secure Element */
   tPSecurityStackAclProcessorReadAcl* pReadAcl;
   /** The function used to hook APDU commands sent to the SE (may be null) */
   tPSecurityStackAclProcessorFilterApdu* pFilterApdu;
   /** The function used to process the STK REFRESH command (may be null) */
   tPSecurityStackAclProcessorNotifyStkRefresh* pNotifyStkRefresh;
   /** The function used to destroy the ACL Processor */
   tPSecurityStackAclProcessorDestroyInstance* pDestroyInstance;
}  tPSecurityStackAclProcessorInterface;

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */
#endif /* __WME_SECURITY_STACK_ACL_PROCESSOR_H */
