/*
 * Copyright (c) 2007-2011 Inside Secure, All Rights Reserved.
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

#ifndef __NFC_HAL_HCI_SERVICE_H
#define __NFC_HAL_HCI_SERVICE_H

/*******************************************************************************
   Contains the declaration of the HCI service functions
*******************************************************************************/
#define P_EVT_REGISTER_TYPE      0x00
#define P_CMD_REGISTER_TYPE      0x01
/*******************************************************************************
   The service codes
*******************************************************************************/
#define P_UNKNOW_SERVICE_ID            0xFF

/*
* DO NOT modify constants value or order
*/
#define P_HCI_SERVICE_ADMINISTRATION   0x00   /* Administration                 */
#define P_HCI_SERVICE_PIPE_MANAGEMENT  0x01   /* Pipe Management                */
#define P_HCI_SERVICE_FIRMWARE         0x02   /* Firmware (patchs)              */
#define P_HCI_SERVICE_IDENTITY         0x03   /* Identity                       */
#define P_HCI_SERVICE_LINK_MANAGEMENT  0x04   /* Link management                */

#define P_HCI_SERVICE_MREAD_ISO_14_A_4 0x05   /* Mode Reader ISO 14443 A Part 4 */
#define P_HCI_SERVICE_MREAD_ISO_14_B_4 0x06   /* Mode Reader ISO 14443 B Part 4 */
#define P_HCI_SERVICE_MREAD_ISO_14_A_3 0x07   /* Mode Reader ISO 14443 A Part 3 */
#define P_HCI_SERVICE_MREAD_ISO_14_B_3 0x08   /* Mode Reader ISO 14443 B Part 3 */
#define P_HCI_SERVICE_MREAD_NFC_T1     0x09   /* Mode Reader NFC Forum Type 1   */
#define P_HCI_SERVICE_MREAD_FELICA     0x0A   /* Mode Reader FeliCa             */
#define P_HCI_SERVICE_MREAD_ISO_15_3   0x0B   /* Mode Reader ISO 15693 Part 3   */
#define P_HCI_SERVICE_MREAD_ISO_15_2   0x0C   /* Mode Reader ISO 15693 Part 2   */
#define P_HCI_SERVICE_P2P_INITIATOR    0x0D   /* Mode  P2P Initiator            */

#define P_NBR_MAX_READER_PROTOCOLS     0x09

#define P_HCI_SERVICE_MCARD_ISO_14_A_4 0x0E   /* Mode Card ISO 14443 A Part 4 */
#define P_HCI_SERVICE_MCARD_ISO_14_B_4 0x0F   /* Mode Card ISO 14443 B Part 4 */
#define P_HCI_SERVICE_MCARD_BPRIME     0x10   /* Mode Card B  PRIME           */
#define P_HCI_SERVICE_MCARD_ISO_15_3   0x11   /* Mode Card ISO 15693 Part 3   */
#define P_HCI_SERVICE_MCARD_FELICA     0x12   /* Mode Card FeliCa             */
#define P_HCI_SERVICE_P2P_TARGET       0x13   /* Mode  P2P Target             */

#define P_HCI_SERVICE_UICC_CONNECTIVITY 0x14  /* UICC Gate                    */
#define P_HCI_SERVICE_MREAD             0x15  /* generic Reader Service       */
#define P_HCI_SERVICE_SE                0x16  /* SE                           */
#define P_HCI_SERVICE_INSTANCE          0x17  /* Instances                    */
#define P_HCI_SERVICE_TEST_RF           0x18  /* test RF                      */

#define P_HCI_SERVICE_MCARD             0x19  /* generic Card Service         */

#define P_HCI_SERVICE_MREAD_BPRIME      0x1A  /* Mode Reader B PRIME          */
#define P_HCI_SERVICE_LOOPBACK          0x1B  /* Loopback                     */
#define P_HCI_SERVICE_SIM_LOOPBACK      0x1C  /* Sim Loopback                 */
#define P_HCI_SERVICE_MCARD_ISO_15_2    0x1D  /* Mode Card ISO 15693 Part 2   */
#define P_HCI_SERVICE_MCARD_ISO_14_B_2  0x1E  /* Mode Card ISO B Part 2       */
#define P_HCI_SERVICE_MCARD_CUSTOM      0x1F  /* Mode Card Custom             */

#define P_HCI_SERVICE_MREAD_NFC_A       0x20  /* Mode Reader NFC A            */
#define P_HCI_SERVICE_MREAD_NFC_F       0x21  /* Mode Reader NFC F            */
#define P_HCI_SERVICE_MREAD_KOVIO       0x22  /* Mode Reader KOVIO            */

/*************************** ADD ENTRY HERE BPRIME *******************************/

#ifdef HCI_SWP
/* Add a specific service for SWP HCI Test */
#define P_HCI_SERVICE_MAX_ID            0x1B
#else
#define P_HCI_SERVICE_MAX_ID            0x22
#endif

/*******************************************************************************
   The command codes
*******************************************************************************/

#define P_HCI_SERVICE_CMD_GET_PROPERTIES    2
#define P_HCI_SERVICE_CMD_SET_PROPERTIES    1

/*******************************************************************************
   The status codes
*******************************************************************************/

/*******************************************************************************
   The event codes
*******************************************************************************/

/* forward declaration */
struct __tHCIServiceOperation;
struct __tHCIServiceContext;
struct __tHCIServiceInstance;

/*******************************************************************************
   Operation States
*******************************************************************************/
#define P_HCI_OPERATION_STATE_SCHEDULED      0x00
#define P_HCI_OPERATION_STATE_CANCELLED      0x01
#define P_HCI_OPERATION_STATE_READ_PENDING   0x02
#define P_HCI_OPERATION_STATE_WRITE_PENDING  0x03
#define P_HCI_OPERATION_STATE_COMPLETED      0x04
#define P_HCI_OPERATION_STATE_RESERVED       0x05

/*******************************************************************************
   Callback declaration
*******************************************************************************/

/**
 * Type of the function to implement to receive the service open completion events.
 *
 * @param[in]   pBindingContext  The context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PHCIServiceOpen().
 *
 * @param[in]   nError  The error code:
 *                - W_SUCCESS If the service is opened,
 *                - W_ERROR_NFCC_COMMUNICATION If the service is unknown or is not supported by the NFC Controller,
 *                - W_ERROR_EXCLUSIVE_REJECTED If the service is already open on the NFC Controller,
 *                - W_ERROR_NFCC_COMMUNICATION If the NFC Controller returns another error.
 *
 * @see  PHCIServiceOpen().
 **/
typedef void tPHCIServiceOpenCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         W_ERROR nError );

/**
 * @brief   Type of the fucntion to implement to be notified of the completion of
 * a command initiated by PHCIServiceExecuteCommand().
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  pBuffer  The reception buffer provided to the functions PHCIServiceExecuteCommand().
 *             The content of the buffer is left unchanged in case of error.
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
 *               - W_ERROR_TIMEOUT if a timeout is detected by the NFC Controller.
 *               - the other error codes depends on the command code.
 *
 * @param[in]  nStatusCode The status code returned by the service.
 *             If \a nError is not W_SUCCESS, the value is ETSI_ERR_ANY_OK.
 *
 * @param[in]  nHCIMessageReceptionCounter  The reception counter of the frame
 *             acknowledging the execute command
 *
 * @see  PHCIServiceExecuteCommand
 **/
typedef void tPHCIServiceExecuteCommandCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter);

/**
 * @brief   Type of the function to implement to be notified of the completion of
 * a send event operation initiated by PHCIServiceSendEvent().
 *
 * @param[in]  pBindingContext  The context.
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
 * @see  PHCIServiceSendEvent()
 **/
typedef void tPHCIServiceSendEventCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         W_ERROR nError,
         uint32_t nReceptionCounter );

/**
 * @brief   Type of the fucntion to implement to be notified of the completion of
 * a send event operation initiated by PHCIServiceSendAnswer().
 *
 * @param[in]  pBindingContext  The context.
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
 * @see  PHCIServiceSendAnswer()
 **/
typedef void tPHCIServiceSendAnswerCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         W_ERROR nError,
         uint32_t nReceptionCounter );

/**
 * Type of the function to implement to receive chunks of event data.
 *
 * The chunk of data are sent by the implementation PHCIServiceRegister().
 *
 * @param[in]   pBindingContext  The current context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PHCIServiceRegister().
 *
 * @param[in]   nEventIdentifier  The event identifier.
 *
 * @param[in]   nOffset  The offset in bytes from the beginning of the message.
 *              A value of zero means that the function is called for the first chunk of the message.
 *
 * @param[in]   pBuffer  The pointer on the buffer containing the chunk of the message.
 *
 * @param[in]   nLength  The length in bytes of the data actually read.
 *
 * @param[in]   nHCIMessageReceptionCounter  The reception counter of the first frame of the message.
 *              If non-zero, the chunk received is the last chunk of the message.
 *              Oteherwise, the value is zero.
 *
 * @see  PHCIServiceRegister().
 **/
typedef void tPHCIServiceEventDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

/**
 * Type of the function to implement to receive  data.
 *
 * The  data are sent by the implementation PHCIServiceRegister().
 *
 * @param[in]   pBindingContext  The current context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PHCIServiceRegister().
 *
 * @param[in]   nMsgIdentifier  The message type identifier.
 *
 * @param[in]   nOffset  The offset in bytes from the beginning of the message.
 *              A value of zero means that the function is called for the first chunk of the message.
 *
 * @param[in]   pBuffer  The pointer on the buffer containing the chunk of the message.
 *
 * @param[in]   nLength  The length in bytes of the data actually read.
 *
 * @param[in]   nHCIMessageReceptionCounter  The reception counter of the first frame of the message.
 *              If non-zero, the chunk received is the last chunk of the message.
 *              Oteherwise, the value is zero.
 *
 * @see  PHCIServiceRegister().
 **/
typedef void tPHCIServiceDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t nMsgIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

/*******************************************************************************
   Structures
*******************************************************************************/

/* Internal function type - Do not use directly */
typedef void tHCIServiceOperationStart(
                  tNALBindingContext* pBindingContext,
                  struct __tHCIServiceOperation* pOperation );

/* Internal function type - Do not use directly */
typedef void tHCIServiceOperationCancel(
                  tNALBindingContext* pBindingContext,
                  struct __tHCIServiceOperation* pOperation );

typedef void tHCIServiceOperationReadDataReceived(
                  tNALBindingContext* pBindingContext,
                  struct __tHCIServiceOperation* pOperation,
                  uint32_t nOffset,
                  uint8_t* pBuffer,
                  uint32_t nLength,
                  uint32_t nHCIMessageReceptionCounter);

/**
 * HCI Operation type structure
 * -------------------------------/
 * (!) Do not use directly
 **/
typedef const struct __tHCIServiceOperationType
{
   tHCIServiceOperationStart* pStartFunction;
   tHCIServiceOperationCancel* pCancelFunction;
   tHCIServiceOperationReadDataReceived* pReadFunction;
} tHCIServiceOperationType;

/**
 * HCI Service OPERATION structure
 * -------------------------------/
 * (!) Do not use directly
 **/
typedef struct __tHCIServiceOperation
{
   struct __tHCIServiceOperation* pNext;
   struct __tHCIServiceContext* pService;
   struct __tHCIServiceOperation* pNextPending;

   tHCIServiceOperationType* pType;
   uint32_t nState;
   uint32_t nHCIRetryCount;
   bool_t     bIsSpecialModeCommand;        /**< If W_TRUE, this operation contains an operation dealing with boot, entering / exiting from standby mode */

   tHCIFrameWriteContext sWriteContext;

   struct __mutiservice
   {
      uint8_t nCommandCode;
      uint8_t nCurrentService;
      const uint8_t* pInputBuffer;
      uint32_t nInputBufferLength;
      uint8_t* pOutputBuffer;
      uint32_t nOutputBufferLength;
      tPHCIServiceExecuteCommandCompleted* pCallbackFunction;
      void* pCallbackParameter;
      bool_t  bIsSpecialModeCommand;

      uint32_t  nInputBufferIndex;
      uint32_t  nOutputBufferIndex;
   } sMultiService;

   union __info
   {
      struct __open
      {
         tPHCIServiceOpenCompleted* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t aWriteBuffer[1];
      } open;
      struct __close
      {
         tPNALGenericCompletion* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t aWriteBuffer[1];
      } close;
      struct __execute
      {
         tPHCIServiceExecuteCommandCompleted* pCallbackFunction;
         void* pCallbackParameter;
         const uint8_t* pInputBuffer;
         uint32_t nInputBufferLength;
         uint8_t* pOutputBuffer;
         uint32_t nOutputBufferLengthMax;
         uint32_t nOutputBufferLength;
         uint8_t nCommandCode;
         uint8_t nResultCode;
      } execute;
      struct __get_properties
      {
         tPHCIServiceExecuteCommandCompleted* pCallbackFunction;
         void* pCallbackParameter;
         const uint8_t* pInputBuffer;
         uint32_t nInputBufferLength;
         uint8_t* pOutputBuffer;
         uint32_t nOutputBufferLengthMax;
         uint32_t nOutputBufferLength;
         uint32_t nPropertiesIndex;
      } get_properties;
      struct __set_properties
      {
         tPHCIServiceExecuteCommandCompleted* pCallbackFunction;
         void* pCallbackParameter;
         const uint8_t* pInputBuffer;
         uint32_t nPropertiesIndex;
         uint32_t nInputBufferLength;
         uint32_t nInputBufferOffset;
      } set_properties;
      struct __send_event
      {
         tPHCIServiceSendEventCompleted* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t* pInputBuffer;
         uint32_t nInputBufferLength;
         uint8_t nEventCode;
      } send_event;
      struct __send_answer
      {
         tPHCIServiceSendAnswerCompleted* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t* pInputBuffer;
         uint32_t nInputBufferLength;
         uint8_t nAnswerCode;
      } send_answer;
      struct __recv_event
      {
         tPHCIServiceEventDataReceived* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t nEventIdentifier;
      } recv_event;
      struct __recv_cmd
      {
         tPHCIServiceDataReceived* pCallbackFunction;
         void* pCallbackParameter;
         uint8_t nCommandIdentifier;
         bool_t bAutomaticAnswer;
      } recv_cmd;
   } info;
} tHCIServiceOperation;

/**
 * HCI Service received frame structure
 * ------------------------------/
 * (!) Do not use directly
 **/

typedef struct __tHCIServiceReceivedFrameDescriptor tHCIServiceReceivedFrameDescriptor;

struct __tHCIServiceReceivedFrameDescriptor
{
   tNALBindingContext* pBindingContext;
   void     * pCallbackParameter;
   uint32_t   nOffset;
   uint8_t  * pBuffer;
   uint32_t   nLength;
   uint32_t   nHCIMessageReceptionCounter;

   tHCIServiceReceivedFrameDescriptor * pNext;
};

/**
 * HCI Service CONTEXT structure
 * -----------------------------/
 * (!) Do not use directly
 **/
typedef struct __tHCIServiceContext
{
   uint8_t nPipeIdentifier;

   uint8_t nState;

   uint8_t nSendAnyOkState;

   uint8_t nPadding1;

   tHCIServiceOperation sInnerOperation;
   tHCIFrameReadContext sReadContext;

   tHCIFrameWriteContext sWriteAnyOkContext;

   tHCIServiceOperation* pCurrentReadOperation;

   tHCIServiceOperation* pOperationListHead;
} tHCIServiceContext;

/**
 * HCI Service INSTANCE structure
 * ------------------------------/
 * (!) Do not use directly
 **/

typedef struct __tHCIServiceInstance
{
   uint8_t nState;

/*    uint8_t nLock; */

   uint8_t nPadding1;
   uint8_t nPadding2;

   uint32_t nReadBytesLost;
   uint32_t nReadMessageLostCount;

   tPNALGenericCompletion* pCallbackFunction;
   void*                       pCallbackParameter;

   tHCIServiceOperation*       pPendingOperationListHead;
   tHCIServiceReceivedFrameDescriptor * pReceivedFrameListHead;
	tHCIServiceReceivedFrameDescriptor * pProcessedFrameListHead;

   tHCIServiceContext aServiceList[P_HCI_SERVICE_MAX_ID + 1];

   uint8_t nCurrentFrameType;

   bool_t    bWaitingForAnswer;
} tHCIServiceInstance;

/*******************************************************************************
   Functions
*******************************************************************************/

/**
 * @brief Creates a HCI service instance.
 *
 * @param[out]  pHCIService  The frame instance to initialize.
 **/
NFC_HAL_INTERNAL void PHCIServiceCreate(
         tHCIServiceInstance* pHCIService );

/**
 * @brief Destroyes a HCI service instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every pending read or write operation is cancelled.
 *
 * @post  PHCIServiceDestroy() does not return any error. The caller should always
 *        assume that the instance is destroyed after this call.
 *
 * @post  The caller should never re-use the instance value.
 *
 * @param[in]  pHCIService  The HCI service instance to destroy.
 **/
NFC_HAL_INTERNAL void PHCIServiceDestroy(
         tHCIServiceInstance* pHCIService );

/**
 * @brief Prepares the stack for a reset of the NFC Controller.
 *
 * After a call to PHCIServicePreReset(), the communication with the NFC Controller is stopped.
 * The data in the read buffer and in the write buffer is erazed.
 *
 * When the NFC Controller is reset, the stack is restarted with a call to PHCIServiceConnect().
 *
 * @param[in]   pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PHCIServicePreReset(
         tNALBindingContext* pBindingContext );

/**
 * @brief  Connects the stack to the NFC Controller.
 *
 * The callback function is called when the connection is established.
 *
 * @pre  PHCIServiceConnect() is called once after the creation of the stack
 *       or after a reset of the NFC Controller.
 *
 * @param[in]   pBindingContext  The context.
 *
 * @param[in]   pCallbackFunction  The callback function.
 *
 * @param[in]   pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALGenericCompletion.
 **/
NFC_HAL_INTERNAL void PHCIServiceConnect(
         tNALBindingContext* pBindingContext,
         tPNALGenericCompletion* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Opens a service.
 *
 * PHCIServiceOpen() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The service context must be left unchanged until the service is closed.
 *
 * @pre  The HCI stack is connected.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.

 * @param[in]  bIsSpecialModeCommand  If W_TRUE, the command is a command related to boot,
 *             switching from active to low power, and switching from low power to active
 *             state transitions. If W_FALSE, the command is only processed when in active state.
 *
 * @see  tPHCIServiceOpenCompleted().
 **/
NFC_HAL_INTERNAL void PHCIServiceOpen(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tPHCIServiceOpenCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t  bIsSpecialModeCommand);

/**
 * @brief  Closes a service.
 *
 * PHCIServiceClose() does not return an error.
 * When the callback function is called, the service should be considered as closed.
 *
 * The service context may be released when the callback function is called.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.

 * @param[in]  bIsSpecialModeCommand  If W_TRUE, the command is a command related to boot,
 *             switching from active to low power, and switching from low power to active
 *             state transitions. If W_FALSE, the command is only processed when in active state.
 *
 * @see  tPNALGenericCompletion.
 **/
NFC_HAL_INTERNAL void PHCIServiceClose(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tPNALGenericCompletion* pCallbackFunction,
         void* pCallbackParameter,
         bool_t  bIsSpecialModeCommand);

/**
 * @brief  Executes a command on the service.
 *
 * PHCIServiceExecuteCommand() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * The content of the parameters depends on the value of \a nCommandCode :
 *
 * P_HCI_SERVICE_CMD_GET_PROPERTIES
 * Gets the properties of a service.
 *  - pInputBuffer  The  buffer contains the list of property identifier.
 *  - pOutputBuffer  When the callback function is called, the buffer contains
 *    the property values in the order specified by the property identifiers.
 *    The format of each value is the following:
 *        - a byte giving the property identifier,
 *        - a byte giving the length of the value, and
 *        - the bytes of the value.
 *  - nError  The specific error codes are the following:
 *        - W_ERROR_ITEM_NOT_FOUND if one of the property is not found.
 *        - W_ERROR_BUFFER_TOO_SHORT if the buffer is too short to receive
 *          the properties.
 *
 * P_HCI_SERVICE_CMD_SET_PROPERTIES
 * Sets the properties of a service.
 *  - pInputBuffer  The  buffer contains the list of property values
 *    in the order specified by the property identifiers.
 *    The format of each value is the following:
 *        - a byte giving the property identifier,
 *        - a byte giving the length of the value, and
 *        - the bytes of the value.
 *  - pOutputBuffer   null
 *  - nError  The specific error codes are the following:
 *        - W_ERROR_ITEM_NOT_FOUND if one of the property is not found.
 *        - W_ERROR_NFCC_COMMUNICATION illegal value for one of the propeties.
 *
 * P_HCI_SERVICE_CMD_<XXXX>
 * Sends a command to a service.
 *  - pInputBuffer  The command payload.
 *  - pOutputBuffer  When the callback function is called, the buffer contains
 *    the command response.
 *  - nError  The specific error codes are the following:
 *        - W_ERROR_BUFFER_TOO_SHORT if the buffer is too short to receive
 *          the command response.
 *
 * @pre  The value of \a nCommandCode should be in a valid range.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  nCommandCode  The command code.
 *
 * @param[in]  pInputBuffer  The input buffer. This value may be null.
 *
 * @param[in]  nInputBufferLength  The input buffer length in bytes.
 *             This value may be zero if \a pInputBuffer is null.
 *
 * @param[out] pOutputBuffer  The output buffer. This value may be null.
 *
 * @param[in]  nOutputBufferLength  The output buffer length in bytes.
 *             This value may be zero if \a pOutputBuffer is null.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @param[in]  bIsSpecialModeCommand  If W_TRUE, the command is a command related to boot,
 *             switching from active to low power, and switching from low power to active
 *             state transitions. If W_FALSE, the command is only processed when in active state.
 *
 *
 * @see  tPHCIServiceExecuteCommandCompleted.
 **/
NFC_HAL_INTERNAL void PHCIServiceExecuteCommand(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tHCIServiceOperation* pOperation,
         uint8_t nCommandCode,
         const uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         uint8_t* pOutputBuffer,
         uint32_t nOutputBufferLength,
         tPHCIServiceExecuteCommandCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t  bIsSpecialModeCommand);

/**
 * @brief  Executes a command on several services
 *
 * PHCIServiceExecuteCommand() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * The content of the parameters depends on the value of \a nCommandCode :
 *
 * P_HCI_SERVICE_CMD_GET_PROPERTIES
 * Gets the properties of a service.
 *  - pInputBuffer  The  buffer contains the list of property identifier.
 *  - pOutputBuffer  When the callback function is called, the buffer contains
 *    the property values in the order specified by the property identifiers.
 *    The format of each value is the following:
 *        - a byte giving the property identifier,
 *        - a byte giving the length of the value, and
 *        - the bytes of the value.
 *  - nError  The specific error codes are the following:
 *        - W_ERROR_ITEM_NOT_FOUND if one of the property is not found.
 *        - W_ERROR_BUFFER_TOO_SHORT if the buffer is too short to receive
 *          the properties.
 *
 * P_HCI_SERVICE_CMD_SET_PROPERTIES
 * Sets the properties of a service.
 *  - pInputBuffer  The  buffer contains the list of property values
 *    in the order specified by the property identifiers.
 *    The format of each value is the following:
 *        - a byte giving the property identifier,
 *        - a byte giving the length of the value, and
 *        - the bytes of the value.
 *  - pOutputBuffer   null
 *  - nError  The specific error codes are the following:
 *        - W_ERROR_ITEM_NOT_FOUND if one of the property is not found.
 *        - W_ERROR_NFCC_COMMUNICATION illegal value for one of the propeties.
 *
 * @pre  The value of \a nCommandCode should be in a valid range.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  nCommandCode  The command code.
 *
 * @param[in]  pInputBuffer  The input buffer. This value may be null.
 *
 * @param[in]  nInputBufferLength  The input buffer length in bytes.
 *             This value may be zero if \a pInputBuffer is null.
 *
 * @param[out] pOutputBuffer  The output buffer. This value may be null.
 *
 * @param[in]  nOutputBufferLength  The output buffer length in bytes.
 *             This value may be zero if \a pOutputBuffer is null.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @param[in]  bIsSpecialModeCommand  If W_TRUE, the command is a command related to boot,
 *             switching from active to low power, and switching from low power to active
 *             state transitions. If W_FALSE, the command is only processed when in active state.
 *
 *
 * @see  tPHCIServiceExecuteCommandCompleted.
 **/
NFC_HAL_INTERNAL void PHCIServiceExecuteMultiServiceCommand(
         tNALBindingContext* pBindingContext,
         tHCIServiceOperation* pOperation,
         uint8_t nCommandCode,
         const uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         uint8_t* pOutputBuffer,
         uint32_t nOutputBufferLength,
         tPHCIServiceExecuteCommandCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t  bIsSpecialModeCommand);

/**
 * @brief  Sends an event to the service.
 *
 * PHCIServiceSendEvent() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * @pre  The value of \a nEventCode should be in a valid range.
 *
 * @param[in]  pBindingContext  The context.
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
 * @param[in]  bIsSpecialModeCommand  If W_TRUE, the command is a command related to boot,
 *             switching from active to low power, and switching from low power to active
 *             state transitions. If W_FALSE, the command is only processed when in active state.
 *
 * @see  tPHCIServiceSendEventCompleted.
 **/
NFC_HAL_INTERNAL void PHCIServiceSendEvent(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tHCIServiceOperation* pOperation,
         uint8_t nEventCode,
         uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         tPHCIServiceSendEventCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t bIsSpecialModeCommand);

/**
 * @brief  Sends an answer to the service.
 *
 * PHCIServiceSendAnswer() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * @pre  The value of \a nAnswerCode should be in a valid range.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  nAnswerCode  The answer code.
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
 * @param[in]  bIsSpecialModeCommand  If W_TRUE, the command is a command related to boot,
 *             switching from active to low power, and switching from low power to active
 *             state transitions. If W_FALSE, the command is only processed when in active state.
 *
 * @see  tPHCIServiceSendAnswerCompleted.
 **/
NFC_HAL_INTERNAL void PHCIServiceSendAnswer(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tHCIServiceOperation* pOperation,
         uint8_t nAnswerCode,
         uint8_t* pInputBuffer,
         uint32_t nInputBufferLength,
         tPHCIServiceSendAnswerCompleted* pCallbackFunction,
         void* pCallbackParameter,
         bool_t bIsSpecialModeCommand);

/**
 * @brief  Registers to be notified of the event of a service.
 *
 * PHCIServiceRegister() does not return an error. In case of error,
 * the callback function is called with the error code.
 *
 * The operation structure should be left unchanged until the callback function returns.
 *
 * If the operation is cancelled, the message are no longer received.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  nRegisterType  The type of message : command or event.
 *
 * @param[in]  bAutomaticAnswer  if W_TRUE: send automacally the answer to NFCC.
 *                                  W_FALSE: the application (receiver) will send the answer to NFCC
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.

 * @param[in]  bIsSpecialModeCommand  If W_TRUE, the command is a command related to boot,
 *             switching from active to low power, and switching from low power to active
 *             state transitions. If W_FALSE, the command is only processed when in active state.
 *
 * @see  tPHCIServiceDataReceived.
 **/
NFC_HAL_INTERNAL void PHCIServiceRegister(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         uint8_t nRegisterType,
         bool_t bAutomaticAnswer,
         tHCIServiceOperation* pOperation,
         tPHCIServiceDataReceived* pCallbackFunction,
         void* pCallbackParameter,
         bool_t bIsSpecialModeCommand);

/**
 * @brief  Cancels a pending operation.
 *
 * If the operation is cancelled, the callback function receive an error W_ERROR_CANCEL.
 * If the operation may not be cancelled if the execution is on going.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  pOperation  The operation structure to use for this operation.
 **/
NFC_HAL_INTERNAL void PHCIServiceCancelOperation(
         tNALBindingContext* pBindingContext,
         uint8_t nServiceIdentifier,
         tHCIServiceOperation* pOperation);

/**
 * Returns the statistic counters.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[out] pnOSI6ReadMessageLost  A pointer on a variable valued with
 *             the number of HCI service messages lost in reception.
 *
 * @param[out] pnOSI6ReadByteErrorCount  A pointer on a variable valued with
 *             the number of bytes in the HCI service messages lost in reception.
 **/
NFC_HAL_INTERNAL void PHCIServiceGetStatistics(
         tNALBindingContext* pBindingContext,
         uint32_t* pnOSI6ReadMessageLost,
         uint32_t* pnOSI6ReadByteErrorCount );

/**
 * Resets the statistic counters.
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PHCIServiceResetStatistics(
         tNALBindingContext* pBindingContext);

/**
 * "Restart" NFC HAL Service after a wake up
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PHCIServiceKick(
         tNALBindingContext* pBindingContext);

/**
 * Flush processed frames
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PHCIServiceFlushProcessedFrames(
         tNALBindingContext* pBindingContext);


#endif /* __NFC_HAL_HCI_SERVICE_H */

