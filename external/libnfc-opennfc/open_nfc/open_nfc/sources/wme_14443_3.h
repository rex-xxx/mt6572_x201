/*
 * Copyright (c) 2007-2012 Inside Secure, All Rights Reserved.
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

#ifndef __WME_14443_3_H
#define __WME_14443_3_H

/*******************************************************************************
   Contains the declaration of the 14443-3 functions
*******************************************************************************/

/* Maximum buffer size for input and output (from reader) as defined in ISO 14443 */
/* this is also the maximum frame size with CRC */
#define P_14443_3_BUFFER_MAX_SIZE          256
#define P_14443_3_FSD_CODE_MAX_VALUE       8
/* Frame buffer maximum size:
   The frame maximum size for ISO 14443-3 A or B is 256 bytes including a 2-bytes CRC */
#define P_14443_3_FRAME_MAX_SIZE     (P_14443_3_BUFFER_MAX_SIZE - 2)

/* Default timeout value used for ISO 14443 A Part 3 */
/* In part 3, no timeout is exchanged, the default value is always used */
#define P_14443_3_A_DEFAULT_TIMEOUT             0x0B    /* 618 ms */

/**
 * Detection Configuration structure for Type A
 *
 * Placed here because referenced in Part 4.
 */
typedef struct __tP14P3DetectionConfigurationTypeA
{
   bool_t bUseCID;
   uint8_t nCID;
   uint32_t nBaudRate;

} tP14P3DetectionConfigurationTypeA;

/**
 * Detection Configuration structure for Type B
 *
 * Placed here because referenced in Part 4.
 */
typedef struct __tP14P3DetectionConfigurationTypeB
{
   bool_t bUseCID;
   uint8_t nCID;
   uint8_t nAFI;
   uint32_t nBaudRate;
   uint8_t nHigherLayerDataLength;
   uint8_t aHigherLayerData[W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH];

} tP14P3DetectionConfigurationTypeB;

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * @brief   Create the connection at 14443-3 level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 *
 * @param[in]  hDriverConnection  The driver connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameterThe callback parameter.
 *
 * @param[in]  nProtocol  The protocol type.
 *
 * @param[in]  pBuffer  The buffer containing the activate result.
 *
 * @param[in]  nLength  The length of the buffer.
 **/
void P14P3UserCreateConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol,
            const uint8_t* pBuffer,
            uint32_t nLength );

/* Same as PReaderExchangeData()
   with an flag to activate the CRC checking in the response */
/**
 * @brief Exchanges data with the card at the level 3 of the protocol.
 *
 * The buffer sizes should respect the sizes given by the function W14Part3GetConnectionInfo().
 * If the reader-to-card length \a nReaderToCardBufferLength is larger than the card's input buffer,
 * the error \ref W_ERROR_BUFFER_TOO_LARGE is returned.
 * If the card-to-reader length \a nCardToReaderBufferMaxLength is smaller than the card's output buffer,
 * the error \ref W_ERROR_BUFFER_TOO_SHORT may be returned if the data sent by the card is larger than the buffer.
 *
 * The CRC is automatically added to the data sent to the card.
 * Thus the CRC bytes must not be present at the end of the buffer \a pReaderToCardBuffer.
 *
 * The parameters \a bCheckCRC and \b bCheckAckOrNack allow to specify specific processing of data received from the card.
 * They are needed for NFC tags Type 2 compliants cards such as Mifare UL, My-D move,...  that use proprietary
 * answer format to acknowledge or reject some commands.
 *
 * \a bCheckCRC allows to specify if the CRC is automatically checked when processing the data sent by the card.
 * If set to W_TRUE, the CRC is automatically checked and removed from the data received from the card. If set to W_FALSE, no
 * CRC check is performed. When set to W_TRUE, the \a bCheckAckOrNack parameter must be set to W_FALSE.
 *
 * The parameter \a bCheckAckOrNack allows to specify that the expected data received from the card is either a ACK or a NACK
 * (as defined in the NFC Forum NDEF Tag Type 2 Specification). When set to W_TRUE, the \b ChechCRC parameter must be set to W_FALSE.
 *
 * The callback function returns the actual length in bytes of the data received and stored in the reception buffer.
 * The value is zero in case of error.
 *
 * When an ACK is received, the callback functions returns W_SUCCESS, and the received buffer length is set to zero.
 * When a NACK is received, the callback function returns W_ERROR_PROTOCOL_ERROR.
 *
 * The callback function is always called whether the exchange operation is completed or if an error occurred.
 * WBasicCancelOperation() is used with the registry handle to cancel the operation.
 * If WBasicCancelOperation() is called when the operation is not yet completed,
 * the callback function is called with the error code \ref W_ERROR_CANCEL.
 *
 * W14Part3ExchangeData() does not return any error.
 * The following error codes are returned in the callback function:
 *   - \ref W_SUCCESS  The operation completed successfully.
 *   - \ref W_ERROR_BAD_HANDLE If the value of \a hConnection given to the exchange function is not valid.
 *   - \ref W_ERROR_BAD_STATE An operation is already pending on this connection.
 *   - \ref W_ERROR_BAD_PARAMETER  Illegal value for the parameters of the read function.
 *   - \ref W_ERROR_CONNECTION_COMPATIBILITY  The card is not compliant with ISO 14443 part 3.
 *   - \ref W_ERROR_BUFFER_TOO_LARGE  The reader-to-card buffer is larger than the card input buffer.
 *   - \ref W_ERROR_BUFFER_TOO_SHORT  The card-to-reader buffer is smaller than the data sent by the card.
 *   - \ref W_ERROR_CANCEL The operation is cancelled by the caller.
 *   - \ref W_ERROR_TAG_DATA_INTEGRITY An error is detected in the integrity of the data sent by the card.
 *   - \ref W_ERROR_RF_COMMUNICATION An error is detected in the protocol used by the card.
 *   - \ref W_ERROR_TIMEOUT  A timeout occurred during the communication with the card.
 *   - other if any other error occurred.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback  A pointer on the callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter transmitted to the callback function.
 *
 * @param[in]  pReaderToCardBuffer  A pointer on the buffer containing the data to send to the card.
 *             If this value is null, the callback function returns the error \ref W_ERROR_BAD_PARAMETER.
 *
 * @param[in]  nReaderToCardBufferLength  The length in bytes of the data to send to the card.
 *             If this value is zero, the callback function returns the error \ref W_ERROR_BAD_PARAMETER.
 *
 * @param[out] pCardToReaderBuffer  A pointer on the buffer receiving the data returned by the card.
 *             Upon error, the content of the buffer is unspecified.
 *
 * @param[in]  nCardToReaderBufferMaxLength  The maximum length in bytes of the buffer \a pCardToReaderBuffer.
 *
 * @param[in]  bCheckCRC  Indicates if a CRC check is done on the data sent by the card.
 *
 * @param[in]  bCheckAckOrNack  Indicates if the expected data sent by the card is a proprietary ACK/NACK answer.
 *
 * @param[out] phOperation  A pointer on a variable valued with the handle of the operation.
 *             Set this value to null if the operation handle is not required.
 *             The return handle should be freed with WBasicCloseHandle() after use.
 *
 * @post  The caller must not modify the content or free the buffers
 *        \a pReaderToCardBuffer or \a pCardToReaderBuffer until the callback function is called.
 *
 * @see   WBasicCancelOperation(), WBasicCloseHandle().
 *
 * @since Open NFC 4.3
 **/
void P14P3UserExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation,
            bool_t bCheckResponseCRC,
            bool_t bCheckAckOrNack);

/* Same as PReaderExchangeData()
   with an flag to activate the CRC checking in the response */
void P14Part3UserExchangeDataEx(
            tContext* pContext,
            void* pObject,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation,
            bool_t bCheckResponseCRC,
            bool_t bCheckAckOrNack);

/* Same as PReaderExchangeData() But used for Mifare Classic*/
void P14Part3ExchangeRawMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation );

/**
 * @brief   Checks if a card is compliant with 14443-4.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 **/
W_ERROR P14P3UserCheckPart4(
            tContext* pContext,
            W_HANDLE hConnection );

/**
 * @brief   Checks if a card is compliant with Mifare.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pUID  The Mifare UID.
 *
 * @param[in]  pnUIDLength  The Mifare UID length.
 *
 * @param[in]  nType  The Mifare type (UL, 1K, 4K, Desfire).
 **/
W_ERROR P14P3UserCheckMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength,
            uint8_t* pnType );

/**
 * @brief   Checks if a card is compliant with My-d.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pUID  The My-d UID.
 *
 * @param[in]  pnUIDLength  The My-d UID length.
 *
 * @param[in]  nType  The My-d type (Move, NFC).
 **/
W_ERROR P14P3UserCheckMyD(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength,
            uint8_t* pnType );


/**
 * @brief   Checks if a card is compliant with Kovio RFID.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pUID  The Kovio RFID UID.
 *
 * @param[in]  pnUIDLength  The Kovio RFID UID length.
 **/
W_ERROR P14P3UserCheckKovioRFID(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength);

/**
 * @brief   Checks if a card is propably compliant with Type2 generic (ATQA = 0044 and SAK = 0).
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pnSAK  The SAK.
 *
 * @param[in]  pUID  The Mifare UID.
 *
 * @param[in]  pnUIDLength  The Mifare UID length.
 **/
W_ERROR P14P3UserCheckNDEF2Gen(
            tContext* pContext,
            W_HANDLE hConnection,
#if 0 /* RFU */
            uint8_t* pnATQA,
            uint8_t* pnSAK,
#endif /* 0 */
            uint8_t* pUID,
            uint8_t* pnUIDLength);

/**
 * @brief   Gets the card serial number.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pSerialNumber  The card serial number.
 *
 * @param[in]  pnLength  The Mifare UID length.
 **/
W_ERROR P14P3UserCheckPico(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pSerialNumber,
            uint8_t* pnLength );

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */


#ifdef P_READER_14P4_STANDALONE_SUPPORT

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

W_ERROR P14P3DriverSetDetectionConfigurationTypeB(
                     tContext* pContext,
                     uint8_t* pCommandBuffer,
                     uint32_t* pnCommandBufferLength,
                     const uint8_t* pDetectionConfigurationBuffer,
                     uint32_t nDetectionConfigurationLength);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#endif /* P_READER_14P4_STANDALONE_SUPPORT */

#endif /* __WME_14443_3_H */
