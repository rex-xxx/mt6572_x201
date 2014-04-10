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

#ifndef __WME_7816_4_STATE_MACHINE_H
#define __WME_7816_4_STATE_MACHINE_H

/* ----------------------------------------------------------------------------

      ISO 7816 Constants

   ---------------------------------------------------------------------------- */

/** @brief AID of the MF */
extern const uint8_t g_a7816SmMfAid[];

/** Size of the AID of the MF */
extern const uint32_t g_n7816SmMfAidSize;

/** Masterfile first byte */
#define P_7816SM_MF_FIRST_BYTE  0x3F

/** The CLA byte for ISO/IEC 7816-4 compliant commands */
#define P_7816SM_CLA ((uint8_t)0x00)

/** The INS byte of the MANAGE CHANNEL command */
#define P_7816SM_INS_MANAGE_CHANNEL ((uint8_t)0x70)

/** The P1 byte of the MANAGE CHANNEL[open] command */
#define P_7816SM_P1_MANAGE_CHANNEL_OPEN ((uint8_t)0x00)

/** The P1 byte of the MANAGE CHANNEL[close] command */
#define P_7816SM_P1_MANAGE_CHANNEL_CLOSE ((uint8_t)0x80)

/** The INS byte of the SELECT command */
#define P_7816SM_INS_SELECT ((uint8_t)0xA4)

/** The P1 byte of the SELECT[AID] command */
#define P_7816SM_P1_SELECT_AID ((uint8_t)0x04)

/** The P2 byte of the SELECT[AID] command : FCI template in answer */
#define P_7816SM_P2_SELECT_AID ((uint8_t)0x00)

/** The P1 byte of the SELECT[FILE] command */
#define P_7816SM_P1_SELECT_FILE ((uint8_t)0x00)

/** The P2 byte of the SELECT[FILE] command : FCI template in answer */
#define P_7816SM_P2_SELECT_FILE_WITH_FCI ((uint8_t)0x00)

/** The P2 byte of the SELECT[FILE] command : FCP template in answer */
#define P_7816SM_P2_SELECT_FILE_WITH_FCP ((uint8_t)0x04)

/** The P2 byte of the SELECT[FILE] command : No response data if Le field absent, or proprietary if Le field present */
#define P_7816SM_P2_SELECT_FILE_NO_RESPONSE_DATA ((uint8_t)0x0C)

/** The INS byte of the GET RESPONSE command */
#define P_7816SM_INS_GET_RESPONSE ((uint8_t)0xC0)

/** The INS byte of the READ RECORD command */
#define P_7816SM_INS_READ_RECORD ((uint8_t)0xB2)

/** The INS byte of the READ BINARY command */
#define P_7816SM_INS_READ_BINARY ((uint8_t)0xB0)

/** The INS byte of the UPDATE BINARY command */
#define P_7816SM_INS_UPDATE_BINARY ((uint8_t)0xD6)

/* The specific SW1 response command */
#define P_7816SM_SW1_61              0x61
#define P_7816SM_SW1_6C              0x6C

/** The minimum length (in bytes) of an AID */
#define P_7816SM_MIN_AID_LENGTH 5
/** The maximum length (in bytes) of an AID */
#define P_7816SM_MAX_AID_LENGTH 16

/* ----------------------------------------------------------------------------

      Raw APDU Interface

   ---------------------------------------------------------------------------- */

/**
 * @brief Opaque data type representing an instance of the ISO-7816 card
 **/
typedef struct __tP7816SmRawInstance tP7816SmRawInstance;

/**
 * @brief Sends an APDU command to an ISO-7816 card.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  pCallback  The callback used to notify the APDU exchange completion.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
  *
 * @param[in]  pSendApduBuffer  The APDU command.
 *
 * @param[in]  nSendApduBufferLength  The length of the APDU command.
 *
 * @param[out] pReceivedApduBuffer  The buffer used to receive the APDU response.
 *
 * @param[in]  nReceivedApduBufferMaxLength  The size of the APDU response buffer.
 *
 **/
typedef void tP7816SmRawExchangeApdu(
                  tContext* pContext,
                  tP7816SmRawInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength);

/**
 * @brief Data type containing the communication interface with an ISO-7816 card.
 */
typedef const struct __tP7816SmRawInterface
{
   /** The function that is called to perform an APDU exchange */
   tP7816SmRawExchangeApdu* pExchange;
} tP7816SmRawInterface;

/* ----------------------------------------------------------------------------

      Logical Channel Interface

   ---------------------------------------------------------------------------- */

/** The maximum number of managed logical channels */
#define P_7816SM_MAX_LOGICAL_CHANNEL 20

/** The null channel reference */
#define P_7816SM_NULL_CHANNEL  0

/**
 * @brief Opaque data type representing an instance of the ISO-7816 State Machine.
 **/
typedef struct __tP7816SmInstance tP7816SmInstance;

/**
 * @brief Pointer-to-function data type that is used to open the basic channel or a logical channel.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  pCallback  The callback used to notify the channel opening completion.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  nType  The type of the channel to open:
 *                - \ref W_7816_CHANNEL_TYPE_RAW for a raw channel,
 *                - \ref W_7816_CHANNEL_TYPE_BASIC for a basic channel, or
 *                - \ref W_7816_CHANNEL_TYPE_LOGICAL for a logical channel.
 *
 * @param[in]  pAid The buffer containing the application AID or the master file MF identifier 0x3F00.
 *             This value should be null for a raw channel.
 *
 * @param[in]  nAidLength  The application AID length in bytes:
 *               - 0 if pAid is null,
 *               - 2 for the master file MF identifier 0x3F00, or
 *               - between 5 and 16 bytes inclusive for an AID.
 *
 * @result     An error code, among which:
 *             - W_ERROR_OPERATION_PENDING, to indicate that the channel opening operation
 *                  is pending, and that completion shall be later notified using the callback.
 *             - W_ERROR_BAD_PARAMETER, to indicate that a passed parameter is incorrect.
 *             - W_ERROR_BAD_STATE, to indicate that an operation with the ISO-7816 card
 *                  is already ongoing.
 *             - W_ERROR_SECURITY if the opening a basic or logical channel is not allowed.
 *             - W_ERROR_EXCLUSIVE_REJECTED, if the raw channel is already opened.
 *             - W_ERROR_EXCLUSIVE_REJECTED, for the basic channel if the basic channel is already opened.
 *             - W_ERROR_FEATURE_NOT_SUPPORTED, if logical channels are not supported
 *                  by the ISO-7816 card.
 **/
typedef W_ERROR tP7816SmOpenChannel(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint32_t nType,
                  const uint8_t* pAID,
                  uint32_t nAIDLength);

/**
 * @brief Pointer-to-function data type that is used to close a channel. This may be
 *    the raw channel, the basic logical channel or any of the supplementary logical channels.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  nChannelReference  The reference of the channel to be closed.
 *             If nChannelReference is P_7816SM_NULL_CHANNEL, all open channels shall be closed.
 *
 * @param[in]  pCallback  The callback used to notify the channel opening completion.
 *
 * @param[in]  pCallbackParameter  The callback parameter number.
 *
 * @result     An error code, among which:
 *             - W_SUCCESS, to indicate that the raw channel or the basic logical channel
 *                  has been successfully closed.
 *             - W_ERROR_OPERATION_PENDING, to indicate that the channel closing operation
 *                  is pending, and that completion shall be later notified using the callback.
 *             - W_ERROR_BAD_PARAMETER, to indicate that a passed parameter is incorrect.
 *             - W_ERROR_BAD_STATE, to indicate that an operation with the ISO-7816 card
 *                  is already ongoing.
 *             - W_ERROR_ITEM_NOT_FOUND, to indicate that the channel is not associated
 *                  with the instance.
 *             - W_ERROR_BAD_STATE, to indicate that the channel is already closed.
 **/
typedef W_ERROR tP7816SmCloseChannel(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter);

/**
 * @brief Pointer-to-function data type that is used to exchange an APDU command on a channel.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  nChannelReference  The reference of the channel to be used to send the APDU command.
 *
 * @param[in]  pCallback  The callback used to notify the channel opening completion.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  pSendApduBuffer  The buffer containing the APDU command to send.
 *
 * @param[in]  nSendApduBufferLength The length of the APDU command.
 *
 * @param[in]  pReceivedApduBuffer  The buffer that shall receive the APDU response.
 *
 * @param[in]  nReceivedApduBufferMaxLength  The length of the APDU response buffer.
 *
 * @result     An error code, among which:
 *             - W_ERROR_OPERATION_PENDING, to indicate that the APDU exchange operation
 *                  is pending, and that completion shall be later notified using the callback.
 *             - W_ERROR_BAD_PARAMETER, to indicate that a passed parameter is incorrect.
 *             - W_ERROR_BAD_STATE, to indicate that an operation with the ISO-7816 card
 *                  is already ongoing.
 *             - W_ERROR_ITEM_NOT_FOUND, to indicate that the channel is not associated
 *                  with the instance.
 *             - W_ERROR_BAD_STATE, to indicate that the channel is already closed.
 *             - W_ERROR_SECURITY if the specified APDU is not allowed.
 **/
typedef W_ERROR tP7816SmExchangeApdu(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength);


/** Data type: Whether the channel is the raw channel. */
#define P_7816SM_DATA_TYPE_IS_RAW_CHANNEL 0

/** Data type: Whether the channel is the basic channel. */
#define P_7816SM_DATA_TYPE_IS_BASIC_CHANNEL 1

/** Data type: the application has been selected with an empty AID */
#define P_7816SM_DATA_TYPE_IS_DEFAULT_SELECTED 2

/** Data type: the AID of the application currently selected on the channel. */
#define P_7816SM_DATA_TYPE_AID 3

/** Data type: the last response APDU received on the channel, including the status word. */
#define P_7816SM_DATA_TYPE_LAST_RESPONSE_APDU 4

/**
 * @brief Pointer-to-function data type that is used to return some data from a channel.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  nChannelReference  The reference of the channel.
 *
 * @param[in]  nType  The type of the data to retreive:
 *              - P_7816SM_DATA_TYPE_IS_RAW_CHANNEL return W_SUCCESS if the channel is the raw channel, an error code if not
 *              - P_7816SM_DATA_TYPE_IS_BASIC_CHANNEL return W_SUCCESS if the channel is the basic channel, an error code if not
 *              - P_7816SM_DATA_TYPE_AID returns the AID slected on this channel.
 *              - P_7816SM_DATA_TYPE_LAST_RESPONSE_APDU returns the last response APDU received on the channel, including the status word.
 *
 * @param[in]  pBuffer  The buffer receiving the data.
 *
 * @param[in]  nBufferMaxLength  The length in bytes of the buffer.
 *
 * @param[out] pnActualLength  The actual length in bytes of the data stored in the buffer.
 *
 * @result     An error code, among which:
 *             - W_SUCCESS, to indicate that the data is returned. If the data
 *                  is not available, *pnActualLength contains 0.
 *             - W_ERROR_BAD_PARAMETER, to indicate that a parameter is incorrect.
 *             - W_ERROR_ITEM_NOT_FOUND, to indicate that the channel is not associated
 *                  with the instance.
 *             - W_ERROR_BUFFER_TOO_SHORT, to indicate that the buffer is too
 *                  short. The required length is then available in *pnAidActualLength.
 **/
typedef W_ERROR tP7816SmGetData(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  uint32_t nType,
                  uint8_t* pBuffer,
                  uint32_t nBufferMaxLength,
                  uint32_t* pnActualLength);

/**
 * @brief Data type containing the logical channel interface with an ISO-7816 card.
 */
typedef const struct __tP7816SmInterface
{
   /** The function that is called to open the basic channel or a logical channel */
   tP7816SmOpenChannel* pOpenChannel;

   /** The function that is called to close a single or all channels */
   tP7816SmCloseChannel* pCloseChannel;

   /** The function that is called to exchange an APDU command */
   tP7816SmExchangeApdu* pExchangeApdu;

   /** The function that is called to get some channel data */
   tP7816SmGetData* pGetData;

} tP7816SmInterface;

/**
 * @brief The default instance of the State Machine interface.
 *
 * All pointer-to-function data fields are guaranteed to be non-null pointers.
 **/
extern tP7816SmInterface g_sP7816SmInterface;

/**
 * @brief Creates a new instance of the ISO-7816 State Machine.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pRawInterface  The communication interface to the ISO-7816 card.
 *
 * @param[in]  pRawInstance  The instance of the ISO-7816 card.
 *
 * @param[out] ppInstance  The pointer that is used to receive the newly created
 *                 ISO-7816 State Machine instance.
 *
 * @result     An error code, among which:
 *             - W_SUCCESS, to indicate success.
 *             - W_ERROR_BAD_PARAMETER, to indicate that a passed parameter is incorrect.
 *             - W_ERROR_OUT_OF_RESOURCE, in case of an out-of-memory condition.
 **/
W_ERROR P7816SmCreateInstance(
                  tContext* pContext,
                  tP7816SmRawInterface* pRawInterface,
                  tP7816SmRawInstance* pRawInstance,
                  tP7816SmInstance** ppInstance);

/**
 * @brief Destroys an ISO-7816 State Machine.
 *
 * Associated memory is freed and the passed instance must not be used any longer.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 **/
void P7816SmDestroyInstance(
                  tContext* pContext,
                  tP7816SmInstance* pInstance);

#endif /* __WME_7816_4_STATE_MACHINE_H */
