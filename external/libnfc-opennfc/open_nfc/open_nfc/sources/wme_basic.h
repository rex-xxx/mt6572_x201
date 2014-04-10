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

#ifndef __WME_BASIC_H
#define __WME_BASIC_H

/**
 * Returns the string describing the library version.
 *
 * @return  A constant string describing the library version.
 **/
const char16_t* PBasicGetLibraryVersion( void );

/**
 * Returns the string describing the library implementation.
 *
 * @return  A constant string describing the library implementation.
 **/
const char16_t* PBasicGetLibraryImplementation( void );

/* The generic operation state (started must be 0) */
#define P_OPERATION_STATE_STARTED      0
#define P_OPERATION_STATE_CANCELLED    1
#define P_OPERATION_STATE_COMPLETED    2

/**
 * Type of the cancel operation callback.
 *
 * @param[in] pContext The current context.
 *
 * @param[in]  pCancelParameter  The cancel callback parameter.
 *
 * @param[in]  bIsClosing  The operation is cancelled because of a close handle.
 **/
typedef void tHandleCancelOperation(
         tContext* pContext,
         void* pCancelParameter,
         bool_t bIsClosing);

/* The maximum number of sub-operations */
#define P_OPERATION_SUB_NUMBER 2

/* The Generic operation structure */
typedef struct __tOperationInfo
{
   /* Operation header */
   tHandleObjectHeader  sHeader;

   /* Operation state */
   uint8_t  nOperationState;

   /* The enclosing object, null if the operation is allocated */
   void* pEnclosingObject;

   /* The sub-operation array */
   struct __tOperationInfo* aSubOperationArray[P_OPERATION_SUB_NUMBER];

   /* The super-operation pointer */
   struct __tOperationInfo* pSuperOperation;

   /* Callback called on WBasicCancelOperation, if different from null */
   tHandleCancelOperation*  pCancelCallback;

   /* Cancel callback parameter */
   void* pCancelParameter;
} tOperationInfo;

/**
 * Creates an operation object.
 *
 * @param[in]  pContext The current context.
 *
 * @param[in]  pCancelCallback  The cancel callback function. May be null.
 *
 * @param[in]  pCancelParameter  The cancel callback parameter.
 *
 * @return The operation handle or W_NULL_HANDLE in case of error.
 **/
W_HANDLE PBasicCreateOperation(
         tContext* pContext,
         tHandleCancelOperation* pCancelCallback,
         void* pCancelParameter);

/**
 * Creates an operation object.
 *
 * @param[in]  pContext The current context.
 *
 * @param[in]  pEnclosingObject  The object including the operation structure.
 *
 * @param[in]  pOperationInfo  The operation structure.
 *
 * @param[in]  pCancelCallback  The cancel callback function. May be null.
 *
 * @param[in]  pCancelParameter  The cancel callback parameter.
 *
 * @return The operation handle or W_NULL_HANDLE in case of error.
 **/
W_HANDLE PBasicCreateEmbeddedOperation(
         tContext* pContext,
         void* pEnclosingObject,
         tOperationInfo* pOperationInfo,
         tHandleCancelOperation* pCancelCallback,
         void* pCancelParameter);

/**
 * Adds a sub-opeartion to an operation and close the sub-operation handle.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  hOperation  The operation handle.
 *
 * @param[in]  hSubOperation  The sub-opeartion handle.
 *
 * @return  The result code.
 **/
W_ERROR PBasicAddSubOperationAndClose(
         tContext* pContext,
         W_HANDLE hOperation,
         W_HANDLE hSubOperation);

/**
 * Sets an operation to the completed state.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  hOperation  The operation handle.
 **/
void PBasicSetOperationCompleted(
         tContext* pContext,
         W_HANDLE hOperation);

/**
 * Gets the opeation state.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  hOperation  The operation handle.
 *
 * @return  The operation state.
 **/
uint32_t PBasicGetOperationState(
         tContext* pContext,
         W_HANDLE hOperation);

/**
 * @brief Generic type for an exchange data function.
 *
 * If the reception buffer length nReceptionBufferMaxLength is smaller than the data received,
 * the error W_ERROR_BUFFER_TOO_SHORT is returned.
 *
 * The callback function is always called whether the exchange operation is completed or if an error occurred.
 *
 * The callback function returns the actual length in bytes of the data received and stored in the reception buffer.
 * This value is different from zero in case of success. The value is zero in case of error.
 *
 * tPBasicExchangeData() does not return any error.
 * The following error codes are returned in the callback function:
 *   - W_SUCCESS  The operation completed successfully.
 *   - W_ERROR_BAD_STATE An operation is already pending on this connection.
 *   - W_ERROR_BAD_PARAMETER  Illegal value for the parameters of the function.
 *   - W_ERROR_CONNECTION_COMPATIBILITY  The target is not compliant with this operation.
 *   - W_ERROR_BUFFER_TOO_SHORT  The reception buffer length is smaller than the data received.
 *   - W_ERROR_RF_COMMUNICATION An error is detected in the protocol.
 *   - W_ERROR_TIMEOUT  A timeout occurred during the communication.
 *   - other if any other error occurred.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  hConnection  The connection.
 *
 * @param[in]  pCallback  A pointer on the callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter transmitted to the callback function.
 *
 * @param[in]  pSendBuffer  A pointer on the buffer containing the data to send.
 *             This value may be null if nSendBufferLength is set to zero.
 *
 * @param[in]  nSendBufferLength  The length in bytes of the data to send.
 *             This value may be zero.
 *
 * @param[out] pReceptionBuffer  A pointer on the buffer receiving the response data.
 *             Upon error, the content of the buffer is left unchanged.
 *
 * @param[in]  nReceptionBufferMaxLength  The maximum length in bytes of the buffer pReceptionBuffer.
 *
 * @param[out] phOperation  A pointer on a variable valued with the handle of the operation.
 *             Set this value to null if the operation handle is not required.
 *             The return handle should be freed with PHandleClose() after use.
 *
 * @post  The caller must not modify the content or free the buffers
 *        pSendBuffer or pReceptionBuffer until the callback function is called.
 **/
typedef void tPBasicExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pSendBuffer,
            uint32_t nSendBufferLength,
            uint8_t* pReceptionBuffer,
            uint32_t nReceptionBufferMaxLength,
            W_HANDLE* phOperation );

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Declare a parameter structure for simple synchronous functions */
typedef struct _tPBasicGenericSyncParameters
{
   P_SYNC_WAIT_OBJECT  hWaitObject;

   W_ERROR  nResult;

   union
   {
      W_HANDLE hHandle;
      uint32_t nValue;
      uint8_t aByteValues[2];
   } res;

} tPBasicGenericSyncParameters;

/**
 * @brief   Receives the completion of simple synchronous functions.
 *
 * @param[in]  pCallbackParameter  The callback blind parameter.
 **/
void PBasicGenericSyncCompletionSimple(
            void *pCallbackParameter );

/**
 * @brief   Receives the completion of simple synchronous functions.
 *
 * @param[in]  pCallbackParameter  The callback blind parameter.
 *
 * @param[in]  nResult  The result code.
 **/
void PBasicGenericSyncCompletion(
            void *pCallbackParameter,
            W_ERROR nResult );

/**
 * @brief   Receives the completion of simple synchronous functions.
 *
 * @param[in]  pCallbackParameter  The callback blind parameter.
 *
 * @param[in]  hHandle  The result handle.
 *
 * @param[in]  nResult  The result code.
 **/
void PBasicGenericSyncCompletionHandle(
            void *pCallbackParameter,
            W_HANDLE hHandle,
            W_ERROR nResult );

/**
 * @brief   Receives the completion of simple synchronous functions.
 *
 * @param[in]  pCallbackParameter  The callback blind parameter.
 *
 * @param[in]  nValue  The result value.
 *
 * @param[in]  nResult  The result code.
 **/
void PBasicGenericSyncCompletionUint32(
            void *pCallbackParameter,
            uint32_t nValue,
            W_ERROR nResult );

/**
 * @brief   Receives the completion of simple synchronous functions.
 *
 * @param[in]  pCallbackParameter  The callback blind parameter.
 *
 * @param[in]  nByteValue  The byte value.
 *
 * @param[in]  nResult  The result code.
 **/
void PBasicGenericSyncCompletionUint8WError(
            void *pCallbackParameter,
            uint8_t nByteValue,
            W_ERROR nResult);

/**
 * @brief   Receives the completion of simple synchronous functions.
 *
 * @param[in]  pCallbackParameter  The callback blind parameter.
 *
 * @param[in]  nResult  The result code.
 *
 * @param[in]  nByteValue1  The first byte value.
 *
 * @param[in]  nByteValue2  The second byte value.
 **/
void PBasicGenericSyncCompletionUint8Uint8(
            void *pCallbackParameter,
            W_ERROR nResult,
            uint8_t nByteValue1,
            uint8_t nByteValue2);

/**
 * @brief Returns the result of a generic synchronous function.
 *
 * @param[in]  pParam  The parameter structure.
 *
 * @return  The result code.
 **/
W_ERROR PBasicGenericSyncWaitForResult(
            tPBasicGenericSyncParameters* pParam);

/**
 * @brief Returns the result of a generic synchronous function.
 *
 * @param[in]  pParam  The parameter structure.
 *
 * @param[out] phHandle  A pointer on a variable valued with the result handle.
 *             This value may be null.
 *
 * @return  The result code.
 **/
W_ERROR PBasicGenericSyncWaitForResultHandle(
            tPBasicGenericSyncParameters* pParam,
            W_HANDLE* phHandle);

/**
 * @brief Returns the result of a generic synchronous function.
 *
 * @param[in]  pParam  The parameter structure.
 *
 * @param[out] pnValue  A pointer on a variable valued with the result value.
 *             This value may be null.
 *
 * @return  The result code.
 **/
W_ERROR PBasicGenericSyncWaitForResultUint32(
            tPBasicGenericSyncParameters* pParam,
            uint32_t* pnValue);

/**
 * @brief Returns the result of a generic synchronous function.
 *
 * @param[in]  pParam  The parameter structure.
 *
 * @param[out] pnByteValue1  A pointer on a variable valued with the first byte value.
 *             This value may be null.
 *
 * @param[out] pnByteValue2  A pointer on a variable valued with the second byte value.
 *             This value may be null.
 *
 * @return  The result code.
 **/
W_ERROR PBasicGenericSyncWaitForResultUint8Uint8(
            tPBasicGenericSyncParameters* pParam,
            uint8_t* pnByteValue1,
            uint8_t* pnByteValue2);

/**
 * @brief Returns the result of a generic synchronous function.
 *
 * @param[in]  pParam  The parameter structure.
 *
 * @param[out] pnByteValue  A pointer on a variable valued with the first byte value.
 *             This value may be null.
 *
 * @return  The result code.
 **/
W_ERROR PBasicGenericSyncWaitForResultUint8WError(
            tPBasicGenericSyncParameters* pParam,
            uint8_t* pnByteValue);

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* #ifdef __WME_BASIC_H */
