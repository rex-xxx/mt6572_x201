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

/* -----------------------------------------------------------------------------

  This file contains the NFC Controller Hardware Abstraction Layer (NFC HAL).

  NFC HAL is the abstraction layer used by the NFC stack to access any NFC Controller.
  See the NFC HAL specification for further details on the content of the messages.

----------------------------------------------------------------------------- */

#ifndef __NFC_HAL_H
#define __NFC_HAL_H

/*******************************************************************************
   Value for the NFC HAL version
 *******************************************************************************/
#define  NAL_VERSION              0x07

/* the maximum length of a message */
#define NAL_MESSAGE_MAX_LENGTH    300

/*******************************************************************************
   Service Codes
*******************************************************************************/

#define NAL_SERVICE_ADMIN           0x00  /* The administration service */
#define NAL_SERVICE_READER_14_A_4   0x01  /* The reader service for ISO 14443-A level 4 */
#define NAL_SERVICE_READER_14_B_4   0x02  /* The reader service for ISO 14443-B level 4 */
#define NAL_SERVICE_READER_14_A_3   0x03  /* The reader service for ISO 14443-A level 3 */
#define NAL_SERVICE_READER_14_B_3   0x04  /* The reader service for ISO 14443-B level 3 */
#define NAL_SERVICE_READER_TYPE_1   0x05  /* The reader service for Type 1 tags */
#define NAL_SERVICE_READER_FELICA   0x06  /* The reader service for FeliCa cards */
#define NAL_SERVICE_READER_15_3     0x07  /* The reader service for ISO 15693 level 3 */
#define NAL_SERVICE_CARD_14_A_4     0x08  /* The card service for ISO 14443-A level 4 */
#define NAL_SERVICE_CARD_14_B_4     0x09  /* The card service for ISO 14443-B level 4 */
#define NAL_SERVICE_P2P_INITIATOR   0x0A  /* The P2P service for the initiator side */
#define NAL_SERVICE_P2P_TARGET      0x0B  /* The P2P service for the target side */
#define NAL_SERVICE_UICC            0x0C  /* The UICC service */
#define NAL_SERVICE_SECURE_ELEMENT  0x0D  /* The Secure Element service */
#define NAL_SERVICE_READER_15_2     0x0E  /* The reader service for ISO 15693 level 2 */
#define NAL_SERVICE_READER_B_PRIME  0x0F  /* The reader service for B Prime */
#define NAL_SERVICE_READER_KOVIO    0x10  /* The reader service for KOVIO */

#define NAL_SERVICE_NUMBER          0x11

/*******************************************************************************
   Message Types
*******************************************************************************/

#define NAL_MESSAGE_TYPE_COMMAND    0x00
#define NAL_MESSAGE_TYPE_ANSWER     0x40
#define NAL_MESSAGE_TYPE_EVENT      0x80

/*******************************************************************************
   Command Codes
*******************************************************************************/

#define NAL_CMD_SET_PARAMETER                (NAL_MESSAGE_TYPE_COMMAND | 0x00 )
#define NAL_CMD_GET_PARAMETER                (NAL_MESSAGE_TYPE_COMMAND | 0x01 )
#define NAL_CMD_DETECTION                    (NAL_MESSAGE_TYPE_COMMAND | 0x02 )
#define NAL_CMD_READER_XCHG_DATA             (NAL_MESSAGE_TYPE_COMMAND | 0x03 )
#define NAL_CMD_COM_TRANSFER                 (NAL_MESSAGE_TYPE_COMMAND | 0x04 )
#define NAL_CMD_PRODUCTION_TEST              (NAL_MESSAGE_TYPE_COMMAND | 0x06 )
#define NAL_CMD_SELF_TEST                    (NAL_MESSAGE_TYPE_COMMAND | 0x07 )
#define NAL_CMD_UPDATE_FIRMWARE              (NAL_MESSAGE_TYPE_COMMAND | 0x08 )
#define NAL_CMD_UICC_START_SWP               (NAL_MESSAGE_TYPE_COMMAND | 0x09 )
#define NAL_CMD_MS_READER_TEST               (NAL_MESSAGE_TYPE_COMMAND | 0xF0 ) // for msr3110

/*******************************************************************************
   Parameter Codes
*******************************************************************************/

#define NAL_PAR_PERSISTENT_POLICY               0x00
#define NAL_PAR_POLICY                          0x01
#define NAL_PAR_HARDWARE_INFO                   0x02
#define NAL_PAR_FIRMWARE_INFO                   0x03
#define NAL_PAR_DETECT_PULSE                    0x04
#define NAL_PAR_PERSISTENT_MEMORY               0x05
#define NAL_PAR_READER_CONFIG                   0x06
#define NAL_PAR_CARD_CONFIG                     0x07
#define NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS   0x08
#define NAL_PAR_UICC_SWP                        0x09
#define NAL_PAR_UICC_READER_PROTOCOLS           0x0A
#define NAL_PAR_UICC_CARD_PROTOCOLS             0x0B
#define NAL_PAR_RAW_MODE                        0x0D
#define NAL_PAR_LIST_CARDS                      0x0E
#define NAL_PAR_ROUTING_TABLE_CONFIG            0x0F
#define NAL_PAR_ROUTING_TABLE_ENTRIES           0x10

/*******************************************************************************
   Result Codes
*******************************************************************************/

#define NAL_RES_OK               (NAL_MESSAGE_TYPE_ANSWER | 0x00)
#define NAL_RES_TIMEOUT          (NAL_MESSAGE_TYPE_ANSWER | 0x01)
#define NAL_RES_UNKNOWN_COMMAND  (NAL_MESSAGE_TYPE_ANSWER | 0x02)
#define NAL_RES_UNKNOWN_PARAM    (NAL_MESSAGE_TYPE_ANSWER | 0x03)
#define NAL_RES_BAD_LENGTH       (NAL_MESSAGE_TYPE_ANSWER | 0x04)
#define NAL_RES_BAD_DATA         (NAL_MESSAGE_TYPE_ANSWER | 0x05)
#define NAL_RES_BAD_STATE        (NAL_MESSAGE_TYPE_ANSWER | 0x06)
#define NAL_RES_PROTOCOL_ERROR   (NAL_MESSAGE_TYPE_ANSWER | 0x07)
#define NAL_RES_BAD_VERSION      (NAL_MESSAGE_TYPE_ANSWER | 0x08)
#define NAL_RES_FEATURE_NOT_SUPPORTED (NAL_MESSAGE_TYPE_ANSWER | 0x09)

/*******************************************************************************
   Event Codes
*******************************************************************************/

#define NAL_EVT_STANDBY_MODE              (NAL_MESSAGE_TYPE_EVENT | 0x00)
#define NAL_EVT_READER_TARGET_COLLISION   (NAL_MESSAGE_TYPE_EVENT | 0x01)
#define NAL_EVT_READER_TARGET_DISCOVERED  (NAL_MESSAGE_TYPE_EVENT | 0x02)
#define NAL_EVT_UICC_DETECTION_REQUEST    (NAL_MESSAGE_TYPE_EVENT | 0x03)
#define NAL_EVT_CARD_SELECTED             (NAL_MESSAGE_TYPE_EVENT | 0x04)
#define NAL_EVT_CARD_SEND_DATA            (NAL_MESSAGE_TYPE_EVENT | 0x05)
#define NAL_EVT_CARD_END_OF_TRANSACTION   (NAL_MESSAGE_TYPE_EVENT | 0x06)
#define NAL_EVT_SE_CARD_EOT               (NAL_MESSAGE_TYPE_EVENT | 0x08)
#define NAL_EVT_P2P_TARGET_DISCOVERED     (NAL_MESSAGE_TYPE_EVENT | 0x09)
#define NAL_EVT_P2P_INITIATOR_DISCOVERED  (NAL_MESSAGE_TYPE_EVENT | 0x0A)
#define NAL_EVT_P2P_SEND_DATA             (NAL_MESSAGE_TYPE_EVENT | 0x0B)
#define NAL_EVT_UICC_CONNECTIVITY         (NAL_MESSAGE_TYPE_EVENT | 0x0C)
#define NAL_EVT_RF_FIELD                  (NAL_MESSAGE_TYPE_EVENT | 0x0D)
#define NAL_EVT_RAW_MESSAGE               (NAL_MESSAGE_TYPE_EVENT | 0x0F)
#define NAL_EVT_NFCC_ERROR                (NAL_MESSAGE_TYPE_EVENT | 0x10)

/*******************************************************************************
   Protocols
*******************************************************************************/

#define NAL_PROTOCOL_READER_ISO_14443_4_A    0x0001  /* Reader ISO 14443 A level 4 */
#define NAL_PROTOCOL_READER_ISO_14443_4_B    0x0002  /* Reader ISO 14443 B level 4 */
#define NAL_PROTOCOL_READER_ISO_14443_3_A    0x0004  /* Reader ISO 14443 A level 3 */
#define NAL_PROTOCOL_READER_ISO_14443_3_B    0x0008  /* Reader ISO 14443 B level 3 */
#define NAL_PROTOCOL_READER_ISO_15693_3      0x0010  /* Reader ISO 15693 level 3 */
#define NAL_PROTOCOL_READER_ISO_15693_2      0x0020  /* Reader ISO 15693 level 2 */
#define NAL_PROTOCOL_READER_FELICA           0x0040  /* Reader Felica */
#define NAL_PROTOCOL_READER_P2P_INITIATOR    0x0080  /* Reader P2P Initiator */
#define NAL_PROTOCOL_READER_TYPE_1_CHIP      0x0100  /* Reader Type 1 */
#define NAL_PROTOCOL_READER_MIFARE_CLASSIC   0x0200  /* Reader Mifare Classic */
#define NAL_PROTOCOL_READER_BPRIME           0x0400  /* Reader B Prime */
#define NAL_PROTOCOL_READER_KOVIO            0x0800  /* Reader Kovio ID tag */
#define NAL_PROTOCOL_READER_MIFARE_PLUS      0x1000  /* Reader Mifare Plus */

#define NAL_PROTOCOL_CARD_ISO_14443_4_A      0x0001  /* Card ISO 14443 A level 4 */
#define NAL_PROTOCOL_CARD_ISO_14443_4_B      0x0002  /* Card ISO 14443 B level 4 */
#define NAL_PROTOCOL_CARD_ISO_14443_3_A      0x0004  /* Card ISO 14443 A level 3 */
#define NAL_PROTOCOL_CARD_ISO_14443_3_B      0x0008  /* Card ISO 14443 B level 3 */
#define NAL_PROTOCOL_CARD_ISO_15693_3        0x0010  /* Card ISO 15693 level 3 */
#define NAL_PROTOCOL_CARD_ISO_15693_2        0x0020  /* Card ISO 15693 level 2 */
#define NAL_PROTOCOL_CARD_FELICA             0x0040  /* Card Felica */
#define NAL_PROTOCOL_CARD_P2P_TARGET         0x0080  /* Card P2P Target */
#define NAL_PROTOCOL_CARD_TYPE_1_CHIP        0x0100  /* Card Type 1 */
#define NAL_PROTOCOL_CARD_MIFARE_CLASSIC     0x0200  /* Card Mifare Classic */
#define NAL_PROTOCOL_CARD_BPRIME             0x0400  /* Card B Prime */
#define NAL_PROTOCOL_CARD_KOVIO              0x0800  /* Card Kovio ID tag */
#define NAL_PROTOCOL_CARD_MIFARE_PLUS        0x1000  /* Card Mifare Plus */

/*******************************************************************************
   Persistent Memory
*******************************************************************************/

/* Size in bytes of the persistent memory parameter */
#define NAL_PERSISTENT_MEMORY_SIZE  8

/*******************************************************************************
   Policy
*******************************************************************************/
/* Size in bytes of the policy parameter */
#define NAL_POLICY_SIZE  8

#define NAL_POLICY_FLAG_READER_LOCK             0x0001
#define NAL_POLICY_FLAG_CARD_LOCK               0x0002

/* SE positions */
#define NAL_POLICY_FLAG_SE_MASK                 0x000C
#define NAL_POLICY_FLAG_SE_OFF                  0x0000
#define NAL_POLICY_FLAG_RF_INTERFACE            0x0004
#define NAL_POLICY_FLAG_FORCED_HOST_INTERFACE   0x0008
#define NAL_POLICY_FLAG_HOST_INTERFACE          0x000C

#define NAL_POLICY_FLAG_SE_ID_MASK              0x0030

#define NAL_POLICY_FLAG_ENABLE_UICC_IN_BATT_OFF 0X0040
#define NAL_POLICY_FLAG_ENABLE_SE_IN_BATT_OFF   0X0080
#define NAL_POLICY_FLAG_BATT_OFF_MASK           0X00C0

/*******************************************************************************
   Pulse Value
*******************************************************************************/

/* Default value of the delay between two pulses */
#define NAL_PAR_DETECT_PULSE_SIZE                    2
#define NAL_PAR_DETECT_PULSE_DEFAULT_VALUE           700
#define NAL_PAR_DETECT_PULSE_DEFAULT_VALUE_CD_ON     430

/*******************************************************************************
   Secure Element
*******************************************************************************/

/* SE Capability Flags */
#define NAL_SE_FLAG_END_OF_TRANSACTION_NOTIFICATION   0x0001

/* The event detection flags */
#define NAL_SE_FLAG_DETECTION_EOT  0x01

/*******************************************************************************
   UICC
*******************************************************************************/

#define NAL_UICC_SWP_NO_SE       0x00
#define NAL_UICC_SWP_BOOTING     0x01
#define NAL_UICC_SWP_ERROR       0x02
#define NAL_UICC_SWP_ACTIVE      0x03
#define NAL_UICC_SWP_DOWN        0x04

/*******************************************************************************
   Firmware Capabilities
*******************************************************************************/

#define NAL_CAPA_BATTERY_OFF                       0x0001
#define NAL_CAPA_BATTERY_LOW                       0x0002
#define NAL_CAPA_STANDBY_MODE                      0x0004
#define NAL_CAPA_CARD_ISO_14443_A_CID              0x0008
#define NAL_CAPA_CARD_ISO_14443_A_NAD              0x0010
#define NAL_CAPA_CARD_ISO_14443_B_CID              0x0020
#define NAL_CAPA_CARD_ISO_14443_B_NAD              0x0040
#define NAL_CAPA_READER_ISO_14443_A_CID            0x0080
#define NAL_CAPA_READER_ISO_14443_A_NAD            0x0100
#define NAL_CAPA_READER_ISO_14443_B_CID            0x0200
#define NAL_CAPA_READER_ISO_14443_B_NAD            0x0400
#define NAL_CAPA_READER_ISO_14443_B_PICO           0x0800
#define NAL_CAPA_READER_ISO_14443_A_BIT            0x1000
#define NAL_CAPA_ROUTING_TABLE					      0x2000


/*******************************************************************************
   Reader exchange Timeout Activation
*******************************************************************************/

#define NAL_TIMEOUT_READER_XCHG_DATA_ENABLE       0x10
#define NAL_TIMEOUT_READER_XCHG_DATA_BITS_MASK    0x0F
#define NAL_TIMEOUT_READER_XCHG_DATA_BITS_OFFSET  0
#define NAL_ISO_14_A_3_CRC_CHECK                  0x20
#define NAL_ISO_14_A_3_ADD_FIXED_BIT_NUMBER       0x40
#define NAL_ISO_14_A_3_T2T_ACK_NACK_CHECK         0x80
#define NAL_ISO_14_A_3_USE_MIFARE                 0xE0
#define NAL_ISO_14_A_4_NAD_ENABLE                 0x20
#define NAL_ISO_15_3_SEND_EOF_ONLY                0x20

/*******************************************************************************
   Firmware Format
*******************************************************************************/

#define NAL_FIRMWARE_HEADER_SIZE      0x1B

#define NAL_FIRMWARE_FORMAT_MAGIC_NUMBER  0x23D61F9B
#define NAL_FIRMWARE_FORMAT_VERSION       0x10

/*******************************************************************************
   Data Size
*******************************************************************************/

#define NAL_PAR_HARDWARE_INFO_SIZE                 0xFB

#define NAL_HARDWARE_TYPE_STRING_SIZE              0x20
#define NAL_HARDWARE_SERIAL_NUMBER_STRING_SIZE     0x20
#define NAL_LOADER_DESCRIPTION_STRING_SIZE         0x20
#define NAL_SE_DESCRIPTION_STRING_SIZE             0x20

#define NAL_PAR_FIRMWARE_INFO_SIZE                 0x2F

#define NAL_FIRMWARE_DESCRIPTION_STRING_SIZE       0x20

/* Maximum number of SE connected with a proprietary link */
#define NAL_MAXIMUM_SE_PROPRIETARY_LINK_NUMBER             4

/* Maximum number of SE connected with a SWP link */
#define NAL_MAXIMUM_SE_SWP_LINK_NUMBER                     4

#define NAL_CARD_14_B_4_HIGHER_LAYER_RESPONSE_MAX_LENGTH   253
#define NAL_CARD_14_A_4_APPLICATION_DATA_MAX_LENGTH        252
#define NAL_READER_14_B_4_HIGHER_LAYER_DATA_MAX_LENGTH     245
#define NAL_READER_FELICA_DETECTION_MESSAGE_SIZE           18

#define NAL_ROUTING_TABLE_CONFIG_SIZE                      2

/*******************************************************************************
   NFC HAL Binding API
*******************************************************************************/

/**
 *  The context used by the NFC HAL
 **/

typedef void * tNALVoidContext;

/**
 * @brief Type of the function to implement to receive the connect completion events.
 *
 * @param[in]   pCallbackContext  The callback context specified by tNALBindingCreate().
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by tNALBindingConnect().
 *
 * @param[in]   nResultCode  The result code:
 *                - 0  if the NFC Controller is initialized
 *                - 1  An error occured during the firmware boot,
 *                  the hardware information are avaliable.
 *                - 2  An error occured during the hardware boot
 *
 * @see  tNALBindingConnect().
 **/
typedef void tNALBindingConnectCompleted(
         void* pCallbackContext,
         void* pCallbackParameter,
         uint32_t nResultCode );

/**
 * @brief Type of the function to implement to receive the write completion events.
 *
 * @param[in]   pCallbackContext  The callback context specified by tNALBindingCreate().
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by tNALBindingWrite().
 *
 * @param[in]   nReceptionCounter  The reception counter of the frame
 *              acknowledging the last frame of the message.
 *
 * @see  tNALBindingWrite().
 **/
typedef void tNALBindingWriteCompleted(
         void* pCallbackContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter );

/**
 * @brief Type of the function to implement to receive the read completion events.
 *
 * The read operation starts when tNALBindingConnect() is called.
 *
 * @param[in]   pCallbackContext  The callback context specified by tNALBindingCreate().
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by tNALBindingCreate().
 *
 * @param[in]   nLength  The length in bytes of the data actually read.
 *
 * @param[in]   nReceptionCounter  The reception counter of the first frame of the message.
 *
 * @see  tNALBindingReset(), tNALBindingConnect().
 **/
typedef void tNALBindingReadCompleted(
         void* pCallbackContext,
         void* pCallbackParameter,
         uint32_t nLength,
         uint32_t nReceptionCounter);

/**
 * @brief Type of the function to implement to receive the timer completion events.
 *
 * @param[in]   pCallbackContext  The callback context specified by tNALBindingCreate().
 **/
typedef void tNALBindingTimerHandler(
         void* pCallbackContext);

/**
 * @brief Type of the function to implement to receive events from a source of entropy.
 *
 * @param[in]   pCallbackContext  The callback context specified by tNALBindingCreate().
 *
 * @param[in]   nValue  The entropy value.
 **/
typedef void tNALBindingAntropySourceHandler(
         void* pCallbackContext,
         uint32_t nValue);

/**
 * @brief Initializes a NFC HAL Binding instance.
 *
 * @param[in]  pPortingConfig  The blind parameter given to PDriverCreate().
 *
 * @param[in]  pCallbackContext  The callback context returned to the callback functions.
 *
 * @param[in]  pReceptionBuffer  The reception buffer.
 *
 * @param[in]  nReceptionBufferLength  The reception buffer length in bytes.
 *
 * @param[in]  pReadCallbackFunction  The read callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @param[in]  nAutoStandbyTimeout  Defines the minimum inactivity duration in milliseconds
 *             prior the NFC controller may enter spontaneously in standby mode.
 *             When set to zero, the NFC controller won’t enter in standby mode spontaneously,
 *             but only when requested by user.
 *
 * @param[in]  nStandbyTimeout  Defines the minimum inactivity duration in milliseconds
 *             prior the NFC controller may enter in standby mode after a standby request.
 *
 * @param[in]  pTimerHandlerFunction  The timer handler function.
 *
 * @param[in]  pEntropySourceHandlerFunction  The entropy source handler function.
 *
 * @return  The context of the NFC HAL or null if an error occurs.
 *
 * @see  tNALBindingReadCompleted.
 **/
typedef tNALVoidContext* tNALBindingCreate(
         void* pPortingConfig,
         void* pCallbackContext,
         uint8_t* pReceptionBuffer,
         uint32_t nReceptionBufferLength,
         tNALBindingReadCompleted* pReadCallbackFunction,
         void* pCallbackParameter,
         uint32_t nAutoStandbyTimeout,
         uint32_t nStandbyTimeout,
         tNALBindingTimerHandler* pTimerHandlerFunction,
         tNALBindingAntropySourceHandler* pEntropySourceHandlerFunction);

/**
 * @brief Destroys a NFC HAL Binding instance.
 *
 * @param[in]  pNALContext  The context of the NFC HAL.
 *
 * @see  tNALBindingCreate.
 **/
typedef void tNALBindingDestroy(
         tNALVoidContext* pNALContext);

/**
 * @brief Resets a NFC HAL Binding instance.
 *
 * @param[in]  pNALContext  The context of the NFC HAL.
 *
 * @see  tNALBindingReadCompleted.
 **/
typedef void tNALBindingReset(
         tNALVoidContext* pNALContext);

/** The connection type: Reset the NFC controller and connect */
#define NAL_SIGNAL_RESET   0x00

/** The connection type: Wakeup the NFC controller and connect */
#define NAL_SIGNAL_WAKEUP  0x01

/**
 * @brief  Connects the stack to the NFC Controller.
 *
 * The callback function is called when the connection is established.
 *
 * @pre  tNALBindingConnect() is called after the creation of the stack or after
 *       a reset of the NFC Controller.
 *
 * @param[in]  pNALContext  The context of the NFC HAL.
 *
 * @param[in]  nType  The type of connection:
 *               - @ref NAL_SIGNAL_RESET  Reset and connect
 *               - @ref NAL_SIGNAL_WAKEUP  Wakeup and connect
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tNALBindingConnectCompleted.
 **/
typedef void tNALBindingConnect(
         tNALVoidContext* pNALContext,
         uint32_t nType,
         tNALBindingConnectCompleted* pCallbackFunction,
         void* pCallbackParameter);

/**
 * @brief  Writes a message.
 *
 * The completion callback function is called when the message is written.
 *
 * @pre only one write operation is executed at a given time.
 *
 * @post The content of the buffer should be left unchanged while the message is written.
 *
 * @param[in]  pNALContext  The context of the NFC HAL.
 *
 * @param[in]  pBuffer  The pointer on the message buffer to write.
 *
 * @param[in]  nLength  The length in bytes of the message.
 *
 * @param[in]  pCallbackFunction  The completion callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback functions.
 *
 * @see  tNALBindingWriteCompleted.
 **/
typedef void tNALBindingWrite(
         tNALVoidContext* pNALContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         tNALBindingWriteCompleted* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief Polls the current status of the NFC HAL Binding.
 *
 * The callback function for the connection, the read operations and the write
 * operations are called from this function.
 *
 * @param[in]  pNALContext  The context of the NFC HAL.
 **/
typedef void tNALBindingPoll(
         tNALVoidContext* pNALContext);

/**
 * NFC Controller parameter: The current submode (read-only).
 **/
#define NAL_PARAM_SUB_MODE          0x00

/**
 * NFC Controller parameter: The current mode (read/write).
 **/
#define NAL_PARAM_MODE              0x01

/**
 * NFC Controller parameter: firmware update progress (read-only).
 **/
#define NAL_PARAM_FIRMWARE_UPDATE   0x02

/**
 * NFC Controller parameter: communication statistics (write-only for reset).
 **/
#define NAL_PARAM_STATISTICS       0x03

/**
 * NFC Controller parameter: the current time in ms (read-only).
 **/
#define NAL_PARAM_CURRENT_TIME     0x04

/**
 * NFC Controller parameter: the timeout value in ms for the timer, 0 to cancel the timer (write-only).
 **/
#define NAL_PARAM_CURRENT_TIMER     0x05

/**
 * @brief Returns a variable value.
 *
 * @param[in]  pNALContext  The context of the NFC HAL.
 *
 * @param[in]  nType  The variable type.
 *
 * @return  The variable value.
 **/
typedef uint32_t tNALBindingGetVariable(
         tNALVoidContext* pNALContext,
         uint32_t nType);

/**
 * @brief Sets a variable value.
 *
 * @param[in]  pNALContext  The context of the NFC HAL.
 *
 * @param[in]  nType  The variable type.
 *
 * @param[in]  nValue  The variable value.
 **/
typedef void tNALBindingSetVariable(
         tNALVoidContext* pNALContext,
         uint32_t nType,
         uint32_t nValue);

/** @brief The protocol statistics */
typedef struct __tNALProtocolStatistics
{
   uint32_t nOSI5ReadMessageLost;
   uint32_t nOSI5ReadByteErrorCount;
   uint32_t nOSI6ReadMessageLost;
   uint32_t nOSI6ReadByteErrorCount;
   uint32_t nOSI4WindowSize;
   uint32_t nOSI4ReadPayload;
   uint32_t nOSI4ReadFrameLost;
   uint32_t nOSI4ReadByteErrorCount;
   uint32_t nOSI4WritePayload;
   uint32_t nOSI4WriteFrameLost;
   uint32_t nOSI4WriteByteErrorCount;
   uint32_t nOSI2FrameReadByteErrorCount;
   uint32_t nOSI2FrameReadByteTotalCount;
   uint32_t nOSI2FrameWriteByteTotalCount;
} tNALProtocolStatistics;

/**
 * @brief Gets the protocol statistics.
 *
 * @param[in]  pNALContext  The context of the NFC HAL.
 *
 * @param[out] pStatistics  The statistics structure to fill.
 **/
typedef void tNALBindingGetStatistics(
         tNALVoidContext* pNALContext,
         tNALProtocolStatistics* pStatistics);

/** The magic word for the NFC HAL Binding structure */
#define NAL_BINDING_MAGIC_WORD 0x23B9D34A

/** @brief The NFC HAL Binding structure */
typedef struct __tNALBinding
{
   uint32_t nMagicWord;

   tNALBindingCreate* pCreateFunction;
   tNALBindingDestroy* pDestroyFunction;
   tNALBindingReset* pResetFunction;
   tNALBindingConnect* pConnectFunction;
   tNALBindingWrite* pWriteFunction;
   tNALBindingPoll* pPollFunction;
   tNALBindingGetVariable* pGetVariableFunction;
   tNALBindingSetVariable* pSetVariableFunction;
   tNALBindingGetStatistics* pGetStatisticsFunction;
} tNALBinding;

#endif /* __NFC_HAL_H */
