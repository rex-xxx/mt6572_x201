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

#ifndef __WME_NFC_HAL_SERVICE_H
#define __WME_NFC_HAL_SERVICE_H

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*******************************************************************************
   Contains the declaration of the NFC HAL Service functions
*******************************************************************************/

/* forward declaration */
struct __tNALServiceOperation;
struct __tNALServiceInstance;

/*******************************************************************************
   Callback declaration
*******************************************************************************/

/**
 * Type of the function to implement to receive the connect completion events.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PNALServiceConnect().
 *
 * @param[in]   nError  The error code.
 *
 * @see  PNALServiceConnect().
 **/
typedef void tPNALServiceConnectCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError );

/**
 * @brief   Type of the fucntion to implement to be notified of the completion of
 * a command initiated by PNALServiceExecuteCommand().
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nLength  The actual length in bytes of the data received and stored
 *             in the reception buffer. This value is different from zero in case of success.
 *             The value is zero in case of error.
 *
 * @param[in]  nError  The error code:
 *               - W_SUCCESS in case of success.
 *               - W_ERROR_NFCC_COMMUNICATION  the command is not supported or an error of protocol occured.
 *               - W_ERROR_CANCEL if the operation is cancelled.
 *               - W_ERROR_FUNCTION_NOT_SUPPORTED if the command is not supported.
 *               - W_ERROR_BUFFER_TOO_SHORT if the buffer is too short to receive
 *                 the command response.
 *               - W_ERROR_TIMEOUT if a timeout is detected by the NFC Controller.
 *
 * @param[in]  nReceptionCounter  The reception counter of the frame
 *             containing the command answer.
 *
 * @see  PNALServiceExecuteCommand
 **/
typedef void tPNALServiceExecuteCommandCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         W_ERROR nError,
         uint32_t nReceptionCounter);

/**
 * @brief   Type of the fucntion to implement to be notified of the completion of
 * a command initiated by PNALServiceGetParameter().
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nLength  The actual length in bytes of the parameter value.
 *             The value is zero in case of error.
 *
 * @param[in]  nError  The error code:
 *               - W_SUCCESS in case of success.
 *               - W_ERROR_NFCC_COMMUNICATION  the command is not supported or an error of protocol occured.
 *               - W_ERROR_CANCEL if the operation is cancelled.
 *               - W_ERROR_ITEM_NOT_FOUND if the parameter is not found.
 *               - W_ERROR_BUFFER_TOO_SHORT if the buffer is too short to receive
 *                 the parameter value.
 *               - W_ERROR_TIMEOUT if a timeout is detected by the NFC Controller.
 *
 * @see  PNALServiceGetParameter
 **/
typedef void tPNALServiceGetParameterCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         W_ERROR nError);

/**
 * @brief   Type of the fucntion to implement to be notified of the completion of
 * a command initiated by PNALServiceSetParameter().
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nError  The error code:
 *               - W_SUCCESS in case of success.
 *               - W_ERROR_NFCC_COMMUNICATION  the command is not supported or an error of protocol occured.
 *               - W_ERROR_CANCEL if the operation is cancelled.
 *               - W_ERROR_ITEM_NOT_FOUND if the parameter is not found.
 *               - W_ERROR_BUFFER_TOO_LARGE if the value is too large for the parameter.
 *               - W_ERROR_TIMEOUT if a timeout is detected by the NFC Controller.
 *
 * @see  PNALServiceSetParameter
 **/
typedef void tPNALServiceSetParameterCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError);

/**
 * @brief   Type of the fucntion to implement to be notified of the completion of
 * a send event operation initiated by PNALServiceSendEvent().
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nError  The error code:
 *               - W_SUCCESS in case of success.
 *               - W_ERROR_NFCC_COMMUNICATION  An error of protocol occured.
 *               - W_ERROR_CANCEL if the operation is cancelled.
 *
 * @param[in]  nReceptionCounter  The reception counter of the frame
 *             acknowledging the event message.
 *
 * @see  PNALServiceSendEvent()
 **/
typedef void tPNALServiceSendEventCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError,
         uint32_t nReceptionCounter );

/**
 * Type of the function to implement to receive an event.
 *
 * The event data is sent by the implementation PNALServiceRegisterForEvent().
 *
 * @param[in]   pContext  The current context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PNALServiceRegisterForEvent().
 *
 * @param[in]   nEventIdentifier  The event identifier.
 *
 * @param[in]   pBuffer  The pointer on the buffer containing event data.
 *
 * @param[in]   nLength  The length in bytes of the event data.
 *
 * @param[in]   nNALMessageReceptionCounter  The reception counter of the
 *              first frame of the event message.
 *
 * @see  PNALServiceRegisterForEvent().
 **/
typedef void tPNALServiceEventReceived(
         tContext* pContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         const uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nNALMessageReceptionCounter);

/*******************************************************************************
   Structures
*******************************************************************************/

/* Internal function type - Do not use directly */
typedef void tNALServiceOperationStart(
                  tContext* pContext,
                  struct __tNALServiceOperation* pOperation );

/* Internal function type - Do not use directly */
typedef void tNALServiceOperationCancel(
                  tContext* pContext,
                  struct __tNALServiceOperation* pOperation,
                  bool_t bGracefull );

typedef bool_t tNALServiceOperationReadDataReceived(
                  tContext* pContext,
                  struct __tNALServiceOperation* pOperation,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nNALMessageReceptionCounter);

/**
 * NFC HAL Operation type structure
 * -------------------------------/
 * (!) Do not use directly
 **/
typedef const struct __tNALServiceOperationType
{
   tNALServiceOperationStart* pStartFunction;
   tNALServiceOperationCancel* pCancelFunction;
   tNALServiceOperationReadDataReceived* pReadFunction;
} tNALServiceOperationType;

/**
 * NFC HAL Service OPERATION structure
 * -------------------------------/
 * (!) Do not use directly
 **/
typedef struct __tNALServiceOperation
{
   struct __tNALServiceOperation* pNext;

   tNALServiceOperationType* pType;

   uint8_t nServiceIdentifier;

   uint32_t nState;

   union __op
   {
      struct __s_execute
      {
         tPNALServiceExecuteCommandCompleted* pCallbackFunction;
         void* pCallbackParameter;
         const uint8_t* pInputBuffer;
         uint32_t nInputBufferLength;
         uint8_t* pOutputBuffer;
         uint32_t nOutputBufferLengthMax;
         uint8_t nCommandCode;
      } s_execute;
      struct __s_get_parameter
      {
         tPNALServiceGetParameterCompleted* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t nParameterCode;
         uint8_t* pValueBuffer;
         uint32_t nValueBufferLengthMax;
      } s_get_parameter;
      struct __s_set_parameter
      {
         tPNALServiceSetParameterCompleted* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t nParameterCode;
         const uint8_t* pValueBuffer;
         uint32_t nValueBufferLength;
      } s_set_parameter;
      struct __s_send_event
      {
         tPNALServiceSendEventCompleted* pCallbackFunction;
         void* pCallbackParameter;
         const uint8_t* pEventDataBuffer;
         uint32_t nEventDataBufferLength;
         uint8_t nEventCode;
      } s_send_event;
      struct __s_recv_event
      {
         tPNALServiceEventReceived* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t nEventFilter;
      } s_recv_event;
   } op;

} tNALServiceOperation;

typedef void tNALServiceStartWriteCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nReceptionCounter );

/**
 * NFC HAL Service INSTANCE structure
 * ------------------------------/
 * (!) Do not use directly
 **/
typedef struct __tNALServiceInstance
{
   uint8_t nState;

   bool_t bWritePending;
   bool_t bInNALCallback;

   uint32_t nReadBytesLost;
   uint32_t nReadMessageLostCount;

   tPNALServiceConnectCompleted* pCallbackFunction;
   void* pCallbackParameter;

   tNALServiceStartWriteCompleted* pWriteCallbackFunction;

   tNALServiceOperation* aOperationListHeadArray[NAL_SERVICE_NUMBER];

   bool_t aOperationLockedArray[NAL_SERVICE_NUMBER];

   uint8_t aReceptionBuffer[NAL_MESSAGE_MAX_LENGTH];

   uint8_t aSendBuffer[NAL_MESSAGE_MAX_LENGTH];

   tNALVoidContext* pNALContext;
   tNALBinding* pNALBinding;

} tNALServiceInstance;

/*******************************************************************************
   Functions
*******************************************************************************/

/**
 * @brief Creates a NFC HAL Service instance.
 *
 * @param[out]  pNALService  The NFC HAL instance to initialize.
 *
 * @param[in]   pNALBinding  The NFC HAL Binding.
 *
 * @param[in]   pPortingConfig  The porting configuration.
 *
 * @param[in]   pContext  The context.
 **/
W_ERROR PNALServiceCreate(
         tNALServiceInstance* pNALService,
         tNALBinding* pNALBinding,
         void* pPortingConfig,
         tContext* pContext );

/**
 * @brief Destroyes a NFC HAL Service instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every pending read or write operation is cancelled.
 *
 * @post  PNALServiceDestroy() does not return any error. The caller should always
 *        assume that the instance is destroyed after this call.
 *
 * @post  The caller should never re-use the instance value.
 *
 * @param[in]  pNALService  The NFC HAL Service instance to destroy.
 **/
void PNALServiceDestroy(
         tNALServiceInstance* pNALService );

/**
 * @brief Prepares the stack for a reset of the NFC Controller.
 *
 * After a call to PNALServicePreReset(), the communication with the NFC Controller is stopped.
 * The data in the read buffer and in the write buffer is erazed.
 *
 * When the NFC Controller is reset, the stack is restarted with a call to PNALServiceConnect().
 *
 * @param[in]   pContext  The context.
 **/
void PNALServicePreReset(
         tContext* pContext );

/**
 * @brief  Connects the stack to the NFC Controller.
 *
 * The callback function is called when the connection is established.
 *
 * @pre  PNALServiceConnect() is called once after the creation of the stack
 *       or after a reset of the NFC Controller.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   nType  The type of connection.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALServiceConnectCompleted.
 **/
void PNALServiceConnect(
         tContext* pContext,
         uint32_t nType,
         tPNALServiceConnectCompleted* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Gets a service parameter.
 *
 * PNALServiceGetParameter() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  nParameterCode  The parameter code.
 *
 * @param[out] pValueBuffer  The buffer receiving the value in case of success.
 *             The content of the buffer is left unchanged in case of error.
 *
 * @param[in]  nValueBufferLength  The value buffer length in bytes.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALServiceGetParameterCompleted.
 **/
void PNALServiceGetParameter(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         tNALServiceOperation* pOperation,
         uint8_t nParameterCode,
         uint8_t* pValueBuffer,
         uint32_t nValueBufferLength,
         tPNALServiceGetParameterCompleted* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Sets a service parameter.
 *
 * PNALServiceSetParameter() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  nParameterCode  The parameter code.
 *
 * @param[in]  pValueBuffer  The value buffer. This value may be null.
 *
 * @param[in]  nValueBufferLength  The value buffer length in bytes.
 *             This value may be zero if \a pInputBuffer is null.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALServiceSetParameterCompleted.
 **/
void PNALServiceSetParameter(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         tNALServiceOperation* pOperation,
         uint8_t nParameterCode,
         const uint8_t* pValueBuffer,
         uint32_t nValueBufferLength,
         tPNALServiceSetParameterCompleted* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Executes a command on the service.
 *
 * PNALServiceExecuteCommand() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * @pre  The value of \a nCommandCode should be in a valid range.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  nCommandCode  The command code.
 *
 * @param[in]  pInputBuffer  The command payload. This pointer value may be null.
 *
 * @param[in]  nInputBufferLength  The input buffer length in bytes.
 *             This value may be zero if \a pInputBuffer is null.
 *
 * @param[out] pOutputBuffer  The output buffer. When the callback function
 *             is called, the buffer contains the command response.This value may be null.
 *
 * @param[in]  nOutputBufferLength  The output buffer length in bytes.
 *             This value may be zero if \a pOutputBuffer is null.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALServiceExecuteCommandCompleted.
 **/
void PNALServiceExecuteCommand(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         tNALServiceOperation* pOperation,
         uint8_t nCommandCode,
         const uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         uint8_t* pOutputBuffer,
         uint32_t nOutputBufferLength,
         tPNALServiceExecuteCommandCompleted* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Sends an event to the service.
 *
 * PNALServiceSendEvent() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * @pre  The value of \a nEventCode should be in a valid range.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  nEventCode  The event code.
 *
 * @param[in]  pInputBuffer  The input buffer. This value may be null.
 *
 * @param[in]  nInputBufferLength  The input buffer length in bytes.
 *             This value may be zero if \a pInputBuffer is null.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALServiceSendEventCompleted.
 **/
void PNALServiceSendEvent(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         tNALServiceOperation* pOperation,
         uint8_t nEventCode,
         const uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         tPNALServiceSendEventCompleted* pCallbackFunction,
         void* pCallbackParameter );

/** The event filter value for any event code */
#define P_NFC_HAL_EVENT_FILTER_ANY  0xFF

/**
 * @brief  Registers to be notified of the event of a service.
 *
 * PNALServiceRegisterForEvent() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * If the operation is cancelled, the event are no longer received.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  nEventFilter  The event filter is the event code used to filter
 *             the events. Set to P_NFC_HAL_EVENT_FILTER_ANY to receive every events.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALServiceEventReceived.
 **/
void PNALServiceRegisterForEvent(
         tContext* pContext,
         uint8_t nServiceIdentifier,
         uint8_t nEventFilter,
         tNALServiceOperation* pOperation,
         tPNALServiceEventReceived* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Cancels a pending operation.
 *
 * If the operation is cancelled, the callback function receives an error W_ERROR_CANCEL.
 * If the operation may not be cancelled if the execution is on going.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 **/
void PNALServiceCancelOperation(
         tContext* pContext,
         tNALServiceOperation* pOperation);

/**
 * Returns the statistic counters.
 *
 * @param[in]  pContext  The context.
 *
 * @param[out] pnNALReadMessageLostCount  A pointer on a variable valued with
 *             the number of NFC HAL Service messages lost in reception.
 *
 * @param[out] pnNALReadBytesLost  A pointer on a variable valued with
 *             the number of bytes in the NFC HAL Service messages lost in reception.
 **/
void PNALServiceGetStatistics(
         tContext* pContext,
         uint32_t* pnNALReadMessageLostCount,
         uint32_t* pnNALReadBytesLost );

/**
 * Polls the NFC HAL.
 *
 * @param[in]  pContext  The context.
 **/
void PNALServicePoll(
         tContext* pContext );

/**
 * @brief Returns a variable value.
 *
 * @param[in]  pContext  The context of the NFC HAL.
 *
 * @param[in]  nType  The variable type.
 *
 * @return  The variable value.
 **/
uint32_t PNALServiceGetVariable(
         tContext* pContext,
         uint32_t nType );

/**
 * @brief Sets a variable value.
 *
 * @param[in]  pContext  The context of the NFC HAL.
 *
 * @param[in]  nType  The variable type.
 *
 * @param[in]  nValue  The variable value.
 **/
void PNALServiceSetVariable(
         tContext* pContext,
         uint32_t nType,
         uint32_t nValue);

/**
 * Resets the statistic counters.
 *
 * @param[in]  pContext  The context.
 **/
void PNALServiceResetStatistics(
         tContext* pContext);

/**
 * @brief  Writes a value into a NFC HAL buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 2 bytes receiving the value.
 *
 * @param[in]   nValue  The value to store.
 **/
#define PNALWriteUint16ToBuffer( nValue, pBuffer ) \
            PUtilWriteUint16ToBigEndianBuffer((nValue), (pBuffer))

/**
 * @brief  Reads a value from a NFC HAL buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 2 bytes containing the value.
 *
 * @return  The corresponding value.
 **/
#define PNALReadUint16FromBuffer( pBuffer ) \
            PUtilReadUint16FromBigEndianBuffer((pBuffer))

/**
 * @brief  Writes a value into a NFC HAL buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 4 bytes receiving the value.
 *
 * @param[in]   nValue  The value to store.
 **/
#define PNALWriteUint32ToBuffer( nValue, pBuffer ) \
            PUtilWriteUint32ToBigEndianBuffer((nValue), (pBuffer))

/**
 * @brief  Reads a value from a NFC HAL buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 4 bytes containing the value.
 *
 * @return  The corresponding value.
 **/
#define PNALReadUint32FromBuffer( pBuffer ) \
            PUtilReadUint32FromBigEndianBuffer((pBuffer))

/**
 * Writes a 16-bit-field card protocol into a NFC HAL message.
 *
 * @param[in]  nCardProtocols  The card protocols.
 *
 * @param[out] pBuffer  The NFC HAL buffer of 2 bytes.
 **/
void PNALWriteCardProtocols(
            uint32_t nCardProtocols,
            uint8_t* pBuffer);

/**
 * Reads a 16-bit-field card protocol from a NFC HAL message.
 *
 * @param[in]  pBuffer  The NFC HAL buffer of 2 bytes.
 *
 * @return  The card protocols.
 **/
uint32_t PNALReadCardProtocols(
            const uint8_t* pBuffer);

/**
 * Writes a 16-bit-field reader protocol into a NFC HAL message.
 *
 * @param[in]  nReaderProtocols  The reader protocols.
 *
 * @param[out] pBuffer  The NFC HAL buffer of 2 bytes.
 **/
void PNALWriteReaderProtocols(
            uint32_t nReaderProtocols,
            uint8_t* pBuffer);

/**
 * Reads a 16-bit-field reader protocol from a NFC HAL message.
 *
 * @param[in]  pBuffer  The NFC HAL buffer of 2 bytes.
 *
 * @return  The reader protocols.
 **/
uint32_t PNALReadReaderProtocols(
            const uint8_t* pBuffer);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#endif /* __WME_NFC_HAL_SERVICE_H */

