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

#ifndef __WME_READER_REGISTRY_H
#define __WME_READER_REGISTRY_H

/*******************************************************************************
  Contains the declaration of the reader registry implementation
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

typedef void tReaderExecuteQueuedExchange(
         tContext * pContext,
         void* pObject,
         W_ERROR nResult);

/**
 * Returns the pointer on the connection object corresponding to a handle value.
 *
 * This function should be called to check the validity of a handle value and
 * to obtain the pointer on the object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 *
 * @param[in]  pExpectedType  The expected object type.
 *
 * @param[out] ppObject  A pointer on a variable valued with the pointer on the
 *             corresponding object is the handle is valid and the object is of
 *             the expected type. Otherwise, the value is set to null.
 *
 * @return     One of the following value:
 *              - W_SUCCESS The handle is valid and of the specified type.
 *              - W_ERROR_BAD_HANDLE  The handle is not valid, the handle is
 *                W_NULL_HANDLE or the object is not of the specified type and
 *                is not a connection.
 *              - W_ERROR_CONNECTION_COMPATIBILITY The handle is not of the
 *                specified type but is a connection.
 **/
W_ERROR PReaderUserGetConnectionObject(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tHandleType* pExpectedType,
            void** ppObject);

/* See PReaderListenToCardDetection */
W_ERROR PReaderUserListenToCardDetection(
         tContext* pContext,
         tPReaderCardDetectionHandler *pHandler,
         void *pHandlerParameter,
         uint8_t nPriority,
         const uint8_t* pConnectionPropertyArray,
         uint32_t nPropertyNumber,
         const void* pDetectionConfigurationBuffer,
         uint32_t nDetectionConfigurationBufferLength,
         W_HANDLE *phEventRegistry );

/**
 * @brief Converts a pointer into a connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pPointer  The pointer to convert. This value should be created
 *             by PUtilConvertHandleToPointer() with a connection handle.
 *
 * @param[in]  pExpectedType  The expected type.
 *
 * @param[in]  phUserConnection  A pointer on a variable valued with
 *             the connection handle.
 *
 * @return  The connection structure.
 **/
void* PReaderUserConvertPointerToConnection(
                  tContext* pContext,
                  void* pPointer,
                  tHandleType* pExpectedType,
                  W_HANDLE* phUserConnection);

/**
 * @brief Returns a boolean that indicates if a property is visible
 *        from API (WBasicXXX functions) or if it is only internal.
 *
 * @param[in]   nProperty  The identifier of the property to check.
 *
 * @return  W_TRUE if nProperty is visible or W_FALSE if nProperty is only internal.
 **/
bool_t PReaderUserIsPropertyVisible(uint8_t nProperty);

/**
 * @brief Returns a the number of cards detected in the field
 *
 * @param[in]  pContext  The context.
 *
 * @return  [0-255[ the number of cards detected
 **/
uint8_t PReaderUserGetNbCardDetected(tContext * pContext);

/**
 * @brief Called by a specific connection to notify reader registry that an exchange will be done.
 *        If a polling is pending, the exchange can be queued. An error W_ERROR_OPERATION_PENDING is returned
 *        to indicates the caller must save data needed by ExchangeData and pExecuteQueuedExchange hold
 *        the function to execute after the polling completion.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 *
 * @param[in]  pExecuteQueuedExchange  The pointer to execute the queued ExchangeData after polling.
 *
 * @param[in]  pExchangeParameter  A blind parameter transmitted to the queued ExchangeData.
 *
 * @return     One of the following value:
 *              - W_SUCCESS               ExchangeData can be done.
 *              - W_ERROR_OPERATION_PENDING A polling is pending. The ExchangeData can be queued (pExecuteQueuedExchange).
 *              - W_ERROR_BAD_STATE       The connection state is wrong (an exchange is already pending).
 *              - W_ERROR_TIMEOUT         The card has been removed.
 */
W_ERROR PReaderNotifyExchange(
               tContext * pContext,
               W_HANDLE hUserConnection,
               tReaderExecuteQueuedExchange* pExecuteQueuedExchange,
               void* pExchangeParameter);

/**
 * @brief Checks the type of a connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 *
 * @return  W_TRUE if the object is a connection.
 **/
bool_t PReaderCheckConnection(
               tContext* pContext,
               W_HANDLE hUserConnection);

/**
 * @brief Called by a specific connection to notify reader registry that an exchange is completed.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 */
void PReaderNotifyExchangeCompletion(
               tContext * pContext,
               W_HANDLE hUserConnection);

/**
 * @brief   Exchange data. PReaderNotifyExchange is not used here.
 *          This method must be called from another connection, not directly by user.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The user connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter The callback parameter.
 *
 * @param[in]  pReaderToCardBuffer  A pointer on the buffer containing the data to send to the card.
 *
 * @param[in]  nReaderToCardBufferLength  The length in bytes of the data to send to the card.
 *
 * @param[in]  pCardToReaderBuffer  A pointer on the buffer receiving the data returned by the card.
 *
 * @param[in]  nCardToReaderBufferMaxLength  The maximum length in bytes of the buffer pCardToReaderBuffer.
 *
 * @param[in]  phOperation  A pointer on a variable valued with the handle of the operation.
 **/
void PReaderExchangeDataInternal(
         tContext * pContext,
         W_HANDLE hConnection,
         tPBasicGenericDataCallbackFunction* pCallback,
         void* pCallbackParameter,
         const uint8_t* pReaderToCardBuffer,
         uint32_t nReaderToCardBufferLength,
         uint8_t* pCardToReaderBuffer,
         uint32_t nCardToReaderBufferMaxLength,
         W_HANDLE* phOperation);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_READER_REGISTRY_H */
