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

#ifndef __WME_DRIVER_H
#define __WME_DRIVER_H

/*******************************************************************************
   Contains the driver function
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

typedef W_ERROR tDriverFunction(
            tContext* pContext,
            void* pBuffer);


#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_USER)

/**
 * @brief  Resets the memory statistics of the driver.
 *
 * @param[in]  pContext  The current context.
 *
 * @keyword  DRIVER_API
 **/
void PContextDriverResetMemoryStatistics(
            tContext* pContext);

/** The memory statistics for the driver memory */
typedef struct __tContextDriverMemoryStatistics
{
   uint32_t nCurrentMemory;
   uint32_t nPeakMemory;
} tContextDriverMemoryStatistics;

/**
 * @brief  Gets the memory statistics of the driver.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pStatistics  The structure receiving the statistics.
 *
 * @param[in]  nSize  The size of the structure in bytes.
 *
 * @keyword  DRIVER_API
 **/
void PContextDriverGetMemoryStatistics(
            tContext* pContext,
            OPEN_NFC_BUF1_O tContextDriverMemoryStatistics* pStatistics,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize );

/**
 * @brief  Stops the event loop.
 *
 * @param[in]  pContext  The current context.
 *
 * @keyword  DRIVER_API
 **/
void PDFCDriverStopEventLoop(
            tContext * pContext );

/**
 * @brief  Interrupts the driver function waiting for a driver event.
 *
 * @param[in]  pContext  The current context.
 *
 * @keyword  DRIVER_API
 **/
void PDFCDriverInterruptEventLoop(
            tContext* pContext );

/**
 * @brief  Cancel an operation.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hOperation  The operation handle.
 *
 * @keyword  DRIVER_API
 **/
void PBasicDriverCancelOperation(
         tContext* pContext,
         W_HANDLE hOperation );

/**
 * @brief  Closes the handle on an object.
 *
 * The handle is removed from the registry.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The handle on the object.
 *
 * @keyword  DRIVER_API
 **/
void PHandleCloseDriver(
            tContext* pContext,
            W_HANDLE hObject );


/**
 * @brief  Closes the handle on an object.
 *
 * The handle is removed from the registry.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The handle on the object.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @keyword  DRIVER_API
 **/
void PHandleCloseSafeDriver(
            tContext * pContext,
            W_HANDLE hObject,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);

/**
 * @brief  Returns the number of driver handles.
 *
 * @param[in]  pContext  The context.
 *
 * @return   The number of driver handles.
 *
 * @keyword  DRIVER_API
 **/
uint32_t PHandleGetCountDriver(
            tContext* pContext );

/**
 * @brief  Returns the number of handle properties
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The driver handle.
 *
 * @param[out] pnPropertyNumber A pointer on a variable valued with the number
 *             of properties.
 *
 * @return    W_SUCCESS on success
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PHandleGetPropertyNumberDriver(
            tContext* pContext,
            W_HANDLE hObject,
            OPEN_NFC_BUF1_OW uint32_t* pnPropertyNumber);

/**
 * @brief  Returns the handle properties
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The driver handle.
 *
 * @param[out] pPropertyArray A pointer on an array receiving the properties.
 *
 * @param[in]  nPropertyArrayLength The size in bytes of the property array.
 *
 * @return    W_SUCCESS on success
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PHandleGetPropertiesDriver(
            tContext* pContext,
            W_HANDLE hObject,
            OPEN_NFC_BUF1_O uint8_t* pPropertyArray,
            OPEN_NFC_BUF1_LENGTH uint32_t nPropertyArrayLength );

/**
 * @brief  Check the handle properties
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hObject  The driver handle.
 *
 * @param[in]  nPropertyValue The property to check.
 *
 * @return    W_SUCCESS on success
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PHandleCheckPropertyDriver(
            tContext* pContext,
            W_HANDLE hObject,
            uint8_t nPropertyValue);

/**
 * @brief  Returns the driver information.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pBuffer  The buffer receiving the information.
 *
 * @param[in]  nBufferSize  The buffer size in bytes.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PNFCControllerDriverReadInfo(
            tContext* pContext,
            OPEN_NFC_BUF1_O void* pBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nBufferSize);

/**
 * @brief  Returns the driver version.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pBuffer  The buffer receiving the driver version.
 *
 * @param[in]  nBufferSize  The buffer size in bytes.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PBasicDriverGetVersion(
            tContext* pContext,
            OPEN_NFC_BUF1_O void* pBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nBufferSize);

/**
 * @brief  Returns the RF lock value (see WNFCControllerGetRFLock).
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nLockSet  The lock set.
 *
 * @return  The lock set value.
 *
 * @keyword  DRIVER_API
 **/
uint32_t PNFCControllerDriverGetRFLock(
            tContext* pContext,
            uint32_t nLockSet );

#endif /* P_CONFIG_DRIVER || P_CONFIG_USER */


/*******************************************************************************
   Contains the NFC controller functions
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 **/
bool_t PNFCControllerIsActive(
            tContext* pContext);

/**
 * See WNFCControllerGetRFActivity()
 *
 * @keyword  DRIVER_API
 **/
uint32_t PNFCControllerDriverGetRFActivity(
            tContext* pContext);

/**
 * @keyword  DRIVER_API
 **/

void PNFCControllerProductionTestDriver(
      tContext * pContext,
      OPEN_NFC_BUF1_IA const uint8_t* pParameterBuffer,
      OPEN_NFC_BUF1_LENGTH uint32_t nParameterBufferLength,
      OPEN_NFC_BUF2_OA uint8_t* pResultBuffer,
      OPEN_NFC_BUF2_LENGTH uint32_t nResultBufferLength,
      OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
      void* pCallbackParameter);

/**
 * @keyword  DRIVER_API
 **/

void PNFCControllerResetDriver(
      tContext * pContext,
      OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
      void* pCallbackParameter,
      uint32_t nMode );

/**
 * @keyword  DRIVER_API
 **/

void PNFCControllerSelfTestDriver(
      tContext * pContext,
      OPEN_NFC_USER_CALLBACK tPNFCControllerSelfTestCompleted* pCallback,
      void* pCallbackParameter );


/**
 * @keyword  DRIVER_API
 **/

void PNFCControllerSetRFLockDriver(
                  tContext * pContext,
                  uint32_t nLockSet,
                  bool_t bReaderLock,
                  bool_t bCardLock,
                  OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter);

/**
 * @keyword  DRIVER_API
 **/

void PNFCControllerSwitchToRawModeDriver(
                  tContext * pContext,
                  OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter);

/**
 * @keyword  DRIVER_API
 **/

void PNFCControllerWriteRawMessageDriver(
                  tContext * pContext,
                  OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  OPEN_NFC_BUF1_I const uint8_t* pBuffer,
                  OPEN_NFC_BUF1_LENGTH uint32_t nLength);

/**
 * @brief  Gets the protocol statistics of the driver.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pStatistics  The structure receiving the statistics.
 *
 * @param[in]  nSize  The size of the structure in bytes.
 *
 * @keyword  DRIVER_API
 **/
void PNALServiceDriverGetProtocolStatistics(
            tContext* pContext,
            OPEN_NFC_BUF1_O tNALProtocolStatistics* pStatistics,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize );

/**
 * @brief  Resets the protocol statistics of the driver.
 *
 * @param[in]  pContext  The current context.
 *
 * @keyword  DRIVER_API
 **/
void PNALServiceDriverResetProtocolStatistics(
            tContext* pContext);

/**
 * @brief  Gets the current time in ms.
 *
 * @param[in]  pContext  The current context.
 *
 * @return     The absolute time value.
 *
 * @keyword  DRIVER_API
 **/
uint32_t PNALServiceDriverGetCurrentTime(
            tContext* pContext);

/*******************************************************************************
   Contains the 14443-3 functions
*******************************************************************************/

/**
 * @brief   Sends a command to the 14443-3 driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverConnection  The connection handle.
 *
 * @param[in]  pCallback   The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  pReaderToCardBuffer   The buffer to send to store the card.
 *
 * @param[in]  nReaderToCardBufferLength   The length of the buffer sent.
 *
 * @param[out] pCardToReaderBuffer   The buffer to use to store the card answer.
 *
 * @param[in]  nCardToReaderBufferMaxLength   The maximum number of bit the reader should receive.
 *
 * @param[in]  bCheckResponseCRC  The check CRC flag, if set to W_TRUE, the response
 *             includes a CRC to be checked by the NFC Controller.
 *
 * @param[in]  bCheckAckOrNack  If set to W_TRUE, the response
 *             may be an ACK or a NACK (as defined in NDEF Type 2 specification)

 * @return     The operation handle
 *
 * @keyword  DRIVER_API
 **/
W_HANDLE P14P3DriverExchangeData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const uint8_t* pReaderToCardBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReaderToCardBufferLength,
            OPEN_NFC_BUF2_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nCardToReaderBufferMaxLength,
            bool_t bCheckResponseCRC,
            bool_t bCheckAckOrNack);

/**
 * @brief   Sends a command to the 14443-3 driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverConnection  The connection handle.
 *
 * @param[in]  pCallback  A pointer on the callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter transmitted to the callback function.
 *
 * @param[in]  pReaderToCardBuffer  A pointer on the buffer containing the data to send to the card.
 *
 * @param[in]  nReaderToCardBufferLength  The number of bytes in the \a pReaderToCardBuffer

 * @param[in]  nReaderToCardBufferLastByteBitNumber  The number of bits to be sent from last byte of \a pReaderToCardBuffer
 *
 * @param[out] pCardToReaderBuffer  A pointer on the buffer receiving the data returned by the card.
 *
 * @param[in]  nCardToReaderBufferMaxLength  The maximum length in bytes of the buffer \a pCardToReaderBuffer.
 *
 * @param[in]  nExpectedBits When the user knows that the expected answer will not contain at least 8 bits of data,
 *             this parameter must be set to the number of bits to be received (valid values are 1 - 7)
 *             In any other case, this parameter must be set to 0.
 *
 * @return     The operation handle
 *
 * @keyword  DRIVER_API
 **/
W_HANDLE P14P3DriverExchangeRawBits(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const uint8_t* pReaderToCardBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReaderToCardBufferLength,
            uint8_t nReaderToCardBufferLastByteBitNumber,
            OPEN_NFC_BUF2_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nCardToReaderBufferMaxLength,
            uint8_t nExpectedBits);
/**
 * @keyword  DRIVER_API
 **/
W_HANDLE P14P3DriverExchangeRawMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const uint8_t* pReaderToCardBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReaderToCardBufferLength,
            OPEN_NFC_BUF2_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nCardToReaderBufferMaxLength);

/**
 * @brief   Sets the timeout value at the 14443-3 driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  nTimeout   The timeout value.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR P14P3DriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout );

/*******************************************************************************
   Contains the 14443-4 functions
*******************************************************************************/

/**
 * @brief   Sends a command to the 14443-3 driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverConnection  The connection handle.
 *
 * @param[in]  pCallback   The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  pReaderToCardBuffer   The buffer to send tostore the card.
 *
 * @param[in]  nReaderToCardBufferLength   The length of the buffer sent.
 *
 * @param[in]  pCardToReaderBuffer   The buffer to use to store the card answer.
 *
 * @param[in]  nCardToReaderBufferMaxLength   The maximum card to reader buffer length.
 *
 * @param[in]  bSendNAD  The NAD is present flag. If W_TRUE, NAD is present in the outgoing RF frame
 *
 * @param[in]  nNAD  The NAD value to be put in the outgoing RF frame
 *
 * @param[in]  bCreateOperation  The flag requestion the creation of an operation.
 *
 * @keyword  DRIVER_API
 **/
W_HANDLE P14P4DriverExchangeData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const uint8_t* pReaderToCardBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReaderToCardBufferLength,
            OPEN_NFC_BUF2_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nCardToReaderBufferMaxLength,
            bool_t     bSendNAD,
            uint8_t  nNAD,
            bool_t bCreateOperation);

/**
 * @brief   Sets the timeout value at the 14443-4 driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  nTimeout   The timeout value.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR P14P4DriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout );



/*******************************************************************************
   Contains the BPrime driver's functions
*******************************************************************************/

/**
 * @brief   Sends a command to the B Prime driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverConnection  The connection handle.
 *
 * @param[in]  pCallback   The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  pReaderToCardBuffer   The buffer to send tostore the card.
 *
 * @param[in]  nReaderToCardBufferLength   The length of the buffer sent.
 *
 * @param[in]  pCardToReaderBuffer   The buffer to use to store the card answer.
 *
 * @param[in]  nCardToReaderBufferMaxLength   The maximum card to reader buffer length.
 *
 * @keyword  DRIVER_API
 **/
W_HANDLE PBPrimeDriverExchangeData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const uint8_t* pReaderToCardBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReaderToCardBufferLength,
            OPEN_NFC_BUF2_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nCardToReaderBufferMaxLength);

/**
 * @brief   Sets the timeout value at the BPrime driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  nTimeout   The timeout value.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PBPrimeDriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout );

/*******************************************************************************
   Contains the 15693-3 functions
*******************************************************************************/

/**
 * @brief   Receives the completion of a process initiated by P15P3DriverExchangeData().
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
 *               - W_ERROR_RF_COMMUNICATION  the command is not supported or an error of protocol occured.
 *               - W_ERROR_CANCEL if the operation is cancelled.
 *               - the other error codes depends on the command code.
 *
 * @see  P15P3DriverExchangeData
 **/
typedef void tP15P3DriverExchangeDataCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nLength,
            W_ERROR nError );

/**
 * @brief   Sends a command to a 15693-3 device.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pCallback   The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  pReaderToCardBuffer   The buffer to send to the 15693-3 device.
 *
 * @param[in]  nReaderToCardBufferLength   The length of the buffer sent.
 *
 * @param[in]  pCardToReaderBuffer   The buffer used to store the card answers.
 *
 * @param[in]  nCardToReaderBufferMaxLength   The maximum card to reader buffer length.
 *
 * @keyword  DRIVER_API
 **/
void P15P3DriverExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            OPEN_NFC_USER_CALLBACK tP15P3DriverExchangeDataCompleted* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const uint8_t* pReaderToCardBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReaderToCardBufferLength,
            OPEN_NFC_BUF2_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nCardToReaderBufferMaxLength );

/**
 * @brief   Sets the timeout value at the 15693-3 driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  nTimeout   The timeout value.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR P15P3DriverSetTimeout(
            tContext* pContext,
            W_HANDLE hConnection,
            uint32_t nTimeout );

/*******************************************************************************
   Contains the Type 1 Function
*******************************************************************************/

/**
 * @brief   Sends a command to the Type 1 driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverConnection  The connection handle.
 *
 * @param[in]  pCallback   The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  pReaderToCardBuffer   The buffer to send tostore the card.
 *
 * @param[in]  nReaderToCardBufferLength   The length of the buffer sent.
 *
 * @param[in]  pCardToReaderBuffer   The buffer to use to store the card answer.
 *
 * @param[in]  nCardToReaderBufferMaxLength   The maximum card to reader buffer length.
 *
 * @keyword  DRIVER_API
 **/
W_HANDLE PType1ChipDriverExchangeData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const uint8_t* pReaderToCardBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReaderToCardBufferLength,
            OPEN_NFC_BUF2_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nCardToReaderBufferMaxLength);

/*******************************************************************************
   Contains the FeliCa Function
*******************************************************************************/

/**
 * @brief   Sends a command to the FeliCa driver layer.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverConnection  The connection handle.
 *
 * @param[in]  pCallback   The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  pReaderToCardBuffer   The buffer to send tostore the card.
 *
 * @param[in]  nReaderToCardBufferLength   The length of the buffer sent.
 *
 * @param[in]  pCardToReaderBuffer   The buffer to use to store the card answer.
 *
 * @param[in]  nCardToReaderBufferMaxLength   The maximum card to reader buffer length.
 *
 * @keyword  DRIVER_API
 **/
void PFeliCaDriverExchangeData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const uint8_t* pReaderToCardBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReaderToCardBufferLength,
            OPEN_NFC_BUF2_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nCardToReaderBufferMaxLength );


/**
 * @brief Ask the NAL the list of FeliCa card connected attribute
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverConnection  The connection handle.
 *
 * @param[in]  pCallback   The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  pCardToReaderBuffer   The buffer to use to store the card answer.
 *
 * @param[in]  nCardToReaderBufferMaxLength   The maximum card to reader buffer length.
 *
 * @keyword  DRIVER_API
 **/
void PFeliCaDriverGetCardList(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_OA uint8_t* pCardToReaderBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nCardToReaderBufferMaxLength );

/*******************************************************************************
   Contains the Reader Registry functions
*******************************************************************************/

/**
 * @brief   Receives the completion of a card detection initiated by PReaderDriverRegister().
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nDriverProtocol  The driver protocol(s) for the connection.
 *
 * @param[in]  hDriverConnection  The driver connection handle.
 *
 * @param[in]  nLength  The length of the anticollision result.
 *
 * @param[in]  bCardApplicationMatch  The current value for the card application match flag.
 *
 * @see  PReaderDriverRegister
 **/
typedef void tPReaderDriverRegisterCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nDriverProtocol,
            W_HANDLE hDriverConnection,
            uint32_t nLength,
            bool_t bCardApplicationMatch );

/**
 * @brief   Registers a reader.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallback   The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nPriority   The connection priority.
 *
 * @param[in]  nRequestedProtocolsBF   The requested protocol(s) bit field.
 *
 * @param[in]  nDetectionConfigurationLength   The size in bytes of
 *             the detection configuration depending on the protocol.
 *
 * @param[in]  pBuffer  The card information buffer.
 *
 * @param[in]  nBufferMaxLength  The card information buffer maximum length.
 *
 * @param[in]  phListenerHandle  The new listener handle.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PReaderDriverRegister(
            tContext* pContext,
            OPEN_NFC_USER_EVENT_HANDLER tPReaderDriverRegisterCompleted* pCallback,
            void* pCallbackParameter,
            uint8_t nPriority,
            uint32_t nRequestedProtocolsBF,
            uint32_t nDetectionConfigurationLength,
            OPEN_NFC_BUF1_IOAL uint8_t* pBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nBufferMaxLength,
            OPEN_NFC_BUF2_OW W_HANDLE* phListenerHandle );

/**
 * @brief   Notifies the driver on the work completed state.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  bGiveToNextListener  Tells the driver if it should continue to poll the registry.
 *
 * @param[in]  bCardApplicationMatch  Tells the driver if the card matches the application.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PReaderDriverWorkPerformed(
            tContext* pContext,
            W_HANDLE hConnection,
            bool_t bGiveToNextListener,
            bool_t bCardApplicationMatch );


/**
 * @brief   Forces redetection of the card. 
 * 
 * The current connection with the card will be destroyed, the current listener will not be marked as called
 * meaning it will be the next one to be called on next detection
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @keyword  DRIVER_API
 **/

W_ERROR PReaderDriverRedetectCard(
            tContext* pContext,
            W_HANDLE hConnection);



/**
 * @brief
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverListener  The driver listener
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PReaderDriverSetWorkPerformedAndClose(
            tContext* pContext,
            W_HANDLE  hDriverListener );

/**
 * @brief returns the number of card detected
 *
 * @param[in]  pContext  The context.
 *
 * @keyword  DRIVER_API
 **/
uint8_t PReaderDriverGetNbCardDetected(tContext * pContext);


/**
 * Get the last reference Time
 *
 * @param[in]  pContext  The context.
 *
 * @return  <i>the last exchange time</i>
 *
 * @keyword  DRIVER_API
 **/
uint32_t PReaderDriverGetLastReferenceTime(tContext* pContext);

/*******************************************************************************
   Contains the Secure Element functions
*******************************************************************************/

/**
 * @brief  Opens the secure element connection at the drive level
 *
 * PSEDriverOpenConnection() does not return any error. The result and the errors
 * are returned in the callback function.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nSlotIdentifier  The slot identifier.
 *
 * @param[in]  bForce  The force flag.
 *
 * @param[in]  pCallback  The callback function receiving the result.
 *
 * @param[in]  pCallbackParameter  The blind parameter for the callback function.
 *
 * @keyword  DRIVER_API
 **/
void PSEDriverOpenConnection(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            bool_t bForce,
            OPEN_NFC_USER_CALLBACK tPBasicGenericHandleCallbackFunction* pCallback,
            void* pCallbackParameter);

/**
 * Gets the ATR of the Secure Element.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  hDriverConnection  The SE connection.
 *
 * @param[out] pAtrBuffer  A pointer on the buffer receiving the ATR value.
 *
 * @param[in]  nAtrBufferLength  The length in bytes of the ATR buffer.
 *
 * @param[out] pnAtrLength  The length in bytes of the ATR stored in the buffer.
 *
 * @return The result code.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PSEDriverGetAtr(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_BUF1_O uint8_t* pAtrBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nAtrBufferLength,
            OPEN_NFC_BUF2_OW uint32_t* pnAtrLength );

/**
 * @keyword  DRIVER_API
 **/
W_ERROR PSEDriverGetInfo(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            OPEN_NFC_BUF1_O tWSEInfoEx* pSEInfo,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize );

/**
 * @keyword  DRIVER_API
 **/
void PSEDriverGetStatus(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            OPEN_NFC_USER_CALLBACK tPSEGetStatusCompleted* pCallback,
            void* pCallbackParameter );

/**
 * @keyword  DRIVER_API
 **/
void PSEDriverSetPolicy(
            tContext * pContext,
            uint32_t nSlotIdentifier,
            uint32_t nStorageType,
            uint32_t nProtocols,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);

/**
 * @keyword  DRIVER_API
 **/
W_ERROR PSeDriver7816SmOpenChannel(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint32_t nType,
            OPEN_NFC_BUF1_IA const uint8_t* pAID,
            OPEN_NFC_BUF1_LENGTH uint32_t nAIDLength);

/**
 * @keyword  DRIVER_API
 **/
W_ERROR PSeDriver7816SmCloseChannel(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            uint32_t nChannelReference,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);

/**
 * @keyword  DRIVER_API
 **/
W_ERROR PSeDriver7816SmExchangeApdu(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            uint32_t nChannelReference,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_IA const uint8_t* pSendApduBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nSendApduBufferLength,
            OPEN_NFC_BUF2_OA uint8_t* pReceivedApduBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nReceivedApduBufferMaxLength);

/**
 * @keyword  DRIVER_API
 **/
W_ERROR PSeDriver7816SmGetData(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            uint32_t nChannelReference,
            uint32_t nType,
            OPEN_NFC_BUF1_O uint8_t* pBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nBufferMaxLength,
            OPEN_NFC_BUF2_OW uint32_t* pnActualLength);

/**
 * Acivates the SWP line.
 *
 * @param[in] pContext  the current context.
 *
 * @param[in]  nSlotIdentifier  The slot identifier.
 *
 * @return The error code.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PSEDriverActivateSwpLine(
            tContext* pContext,
            uint32_t nSlotIdentifier);

/**
 * @keyword  DRIVER_API
 *
 **/
W_ERROR PSEDriverImpersonateAndCheckAidAccess(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            OPEN_NFC_BUF1_I const uint8_t* pAIDBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nAIDLength,
            OPEN_NFC_BUF2_I const uint8_t* pImpersonationDataBuffer,
            OPEN_NFC_BUF2_LENGTH uint32_t nImpersonationDataBufferLength );

/*******************************************************************************
   Contains the Card Emulation functions, used by Virtual Tag.
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 **/
void PEmulOpenConnectionDriver1Ex(
            tContext* pContext,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pOpenCallback,
            void* pOpenCallbackParameter,
            OPEN_NFC_BUF1_I const tWEmulConnectionInfo* pEmulConnectionInfo,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize,
            OPEN_NFC_BUF2_OW W_HANDLE* phHandle);

/**
 * @keyword  DRIVER_API
 **/
void PEmulOpenConnectionDriver2Ex(
            tContext* pContext,
            W_HANDLE hHandle,
            OPEN_NFC_USER_EVENT_HANDLER tPEmulDriverEventReceived* pEventCallback,
            void* pEventCallbackParameter );

/**
 * @keyword  DRIVER_API
 **/
void PEmulOpenConnectionDriver3Ex(
            tContext* pContext,
            W_HANDLE hHandle,
            OPEN_NFC_USER_EVENT_HANDLER tPEmulDriverCommandReceived* pCommandCallback,
            void* pCommandCallbackParameter );

/**
 * @keyword  DRIVER_API
 **/
void PEmulCloseDriver(
            tContext* pContext,
            W_HANDLE hHandle,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pCallback,
            void * pCallbackParameter );


/*******************************************************************************
   Contains cache connection functions
*******************************************************************************/

/**
 * @brief  updates the virtual cache Memory with kernel cache Memory.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCacheConnection  The virtual cache Connection structure.
 *
 * @param[in]  nSize  The size of the structure in bytes.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PCacheConnectionDriverRead(
            tContext* pContext,
            OPEN_NFC_BUF1_O tCacheConnectionInstance *pCacheConnection,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize );

/**
 * @brief  updates the kernel cache Memory with virtual cache Memory.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCacheConnection  The virtual cache Connection structure.
 *
 * @param[in]  nSize  The size of the structure in bytes.
 *
 * @keyword  DRIVER_API
 **/
W_ERROR PCacheConnectionDriverWrite(
            tContext* pContext,
            OPEN_NFC_BUF1_I const tCacheConnectionInstance *pCacheConnection,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize );

/*******************************************************************************
   The P2P functions
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 **/
W_ERROR PP2PGetConfigurationDriver(
            tContext * pContext,
            OPEN_NFC_BUF1_O tWP2PConfiguration* pConfiguration,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize);

/**
 * @keyword  DRIVER_API
 **/
W_ERROR PP2PSetConfigurationDriver(
            tContext * pContext,
            OPEN_NFC_BUF1_I const tWP2PConfiguration* pConfiguration,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize);

/**
  * WP2PEstablishLink equivalent for calls from Open NFC stack
  *
  * @see WP2PEstablishLink
  */

void PP2PEstablishLinkWrapper(
            tContext * pContext,
            tPBasicGenericHandleCallbackFunction * pEstablishmentCallback,
            void * pEstablishmentCallbackParameter,
            tPBasicGenericCallbackFunction * pReleaseCallback,
            void * pReleaseCallbackParameter,
            W_HANDLE *phOperation);

/**
 * @keyword  DRIVER_API
 **/
W_HANDLE PP2PEstablishLinkDriver1Wrapper(
            tContext * pContext,
            OPEN_NFC_USER_CALLBACK tPBasicGenericHandleCallbackFunction* pEstablishmentCallback,
            void* pEstablishmentCallbackParameter
 );

/**
 * @keyword  DRIVER_API
 **/
void PP2PEstablishLinkDriver2Wrapper(
            tContext * pContext,
            W_HANDLE hLink,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pReleaseCallback,
            void* pReleaseCallbackParameter,
            OPEN_NFC_BUF1_OW W_HANDLE * phOperation);

/**
 * @keyword  DRIVER_API
 **/

W_ERROR PP2PGetLinkPropertiesDriver(
            tContext * pContext,
            W_HANDLE hLink,
            OPEN_NFC_BUF1_O tWP2PLinkProperties * pProperties,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize);

/**
 * @keyword DRIVER_API
 **/

W_ERROR PP2PCreateSocketDriver(
            tContext * pContext,
            uint8_t nType,
            OPEN_NFC_BUF1_I const char16_t * pServiceURI,
            OPEN_NFC_BUF1_LENGTH uint32_t  nSize,
            uint8_t nSAP,
            OPEN_NFC_BUF2_OW W_HANDLE * phSocket
);

/**
 * @keyword DRIVER_API
 **/
W_ERROR PP2PGetSocketParameterDriver(
            tContext * pContext,
            W_HANDLE   hSocket,
            uint32_t   nParameter,
            OPEN_NFC_BUF1_OW uint32_t * pnValue);

/**
 * @keyword DRIVER_API
 **/
void PP2PConnectDriver(
            tContext* pContext,
            W_HANDLE hSocket,
            W_HANDLE hLink,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pEstablishmentCallback,
            void * pEstablishmentCallbackParameter);


/**
 * @keyword DRIVER_API
 **/
void PP2PShutdownDriver(
            tContext* pContext,
            W_HANDLE  hSocket,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pReleaseCallback,
            void * pReleaseCallbackParameter
);

/**
  * @keyword DRIVER_API
 **/
void PP2PReadDriver (
            tContext* pContext,
            W_HANDLE hConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_OA uint8_t* pReceptionBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReceptionBufferLength,
            OPEN_NFC_BUF2_OW W_HANDLE* phOperation);

 /**
  * @keyword DRIVER_API
 **/
void PP2PWriteDriver (
            tContext* pContext,
            W_HANDLE hConnection,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_IA const uint8_t* pSendBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nSendBufferLength,
            OPEN_NFC_BUF2_OW W_HANDLE* phOperation);

/**
  * @keyword DRIVER_API
 **/
void PP2PSendToDriver(
            tContext* pContext,
            W_HANDLE hSocket,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nSAP,
            OPEN_NFC_BUF1_IA const uint8_t* pSendBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nSendBufferLength,
            OPEN_NFC_BUF2_OW W_HANDLE* phOperation);

/**
  * @keyword DRIVER_API
 **/
void PP2PRecvFromDriver(
            tContext* pContext,
            W_HANDLE hSocket,
            OPEN_NFC_USER_CALLBACK tPP2PRecvFromCompleted* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_OA uint8_t * pReceptionBuffer,
            OPEN_NFC_BUF1_LENGTH uint32_t nReceptionBufferLength,
            OPEN_NFC_BUF2_OW W_HANDLE* phOperation);

/**
 * @keyword  P_FUNCTION, DRIVER_API
 **/
void PP2PURILookupDriver(
            tContext * pContext,
            W_HANDLE hLink,
            OPEN_NFC_USER_CALLBACK tPP2PURILookupCompleted* pCallback,
            void* pCallbackParameter,
            OPEN_NFC_BUF1_I const char16_t* pServiceURI,
            OPEN_NFC_BUF1_LENGTH uint32_t nSize);

/*******************************************************************************
   The Timer functions
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 **/
void PMultiTimerSetDriver(
            tContext* pContext,
            uint32_t nTimerIdentifier,
            uint32_t nAbsoluteTimeout,
            OPEN_NFC_USER_CALLBACK tPBasicGenericCompletionFunction* pCallbackFunction,
            void* pCallbackParameter );

/**
 * @keyword  DRIVER_API
 **/
void PMultiTimerCancelDriver(
            tContext* pContext,
            uint32_t nTimerIdentifier );

/*******************************************************************************
   The Random function
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 **/
uint32_t PContextDriverGenerateRandom(
            tContext* pContext);

/*******************************************************************************
   The firmware update
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 **/
void PNFCControllerFirmwareUpdateDriver(
   tContext* pContext,
   OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
   void* pCallbackParameter,
   OPEN_NFC_BUF1_IA const uint8_t* pUpdateBuffer,
   OPEN_NFC_BUF1_LENGTH uint32_t nUpdateBufferLength,
   uint32_t nMode );

/*******************************************************************************
   The pulse period
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 **/
void PReaderSetPulsePeriodDriver(
   tContext * pContext,
   OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction* pCallback,
   void* pCallbackParameter,
   uint32_t nPulsePeriod );


/*******************************************************************************
   Security Functions
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 *
 **/
W_ERROR PSecurityManagerDriverAuthenticate(
   tContext * pContext,
   OPEN_NFC_BUF1_I const uint8_t* pApplicationDataBuffer,
   OPEN_NFC_BUF1_LENGTH uint32_t nApplicationDataBufferLength );


/*******************************************************************************
   SNEP Server
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 *
 **/

W_ERROR PNDEFRegisterSNEPMessageHandlerDriver  (
      tContext * pContext,
      OPEN_NFC_USER_EVENT_HANDLER tPBasicGenericDataCallbackFunction* pHandler,
      void *  pHandlerParameter,
      uint8_t nPriority,
      OPEN_NFC_BUF1_OW W_HANDLE *  phRegistry );

/**
 * @keyword  DRIVER_API
 *
 **/
W_ERROR PNDEFRetrieveSNEPMessageDriver(
      tContext * pContext,
      OPEN_NFC_BUF1_O uint8_t * pBuffer,
      OPEN_NFC_BUF1_LENGTH uint32_t nBufferLength);

/**
 * @keyword  DRIVER_API
 *
 **/
void PNDEFSetWorkPerformedSNEPDriver(
      tContext *pContext,
      bool_t   bGiveToNextListener);

/**
 * @keyword  DRIVER_API
 *
 **/
W_HANDLE PNDEFSendSNEPMessageDriver(
      tContext * pContext,
      OPEN_NFC_BUF1_I uint8_t * pBuffer,
      OPEN_NFC_BUF1_LENGTH  uint32_t nBufferLength,
      OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

/*******************************************************************************
   NPP Server
*******************************************************************************/

/**
 * @keyword  DRIVER_API
 *
 **/
W_ERROR PNDEFRegisterNPPMessageHandlerDriver  (
      tContext * pContext,
      OPEN_NFC_USER_EVENT_HANDLER tPBasicGenericDataCallbackFunction* pHandler,
      void *  pHandlerParameter,
      uint8_t nPriority,
      OPEN_NFC_BUF1_OW W_HANDLE *  phRegistry );

/**
 * @keyword  DRIVER_API
 *
 **/
W_ERROR PNDEFRetrieveNPPMessageDriver(
      tContext * pContext,
      OPEN_NFC_BUF1_O uint8_t * pBuffer,
      OPEN_NFC_BUF1_LENGTH uint32_t nBufferLength);

/**
 * @keyword  DRIVER_API
 *
 **/
void PNDEFSetWorkPerformedNPPDriver(
      tContext *pContext,
      bool_t   bGiveToNextListener);

/**
 * @keyword  DRIVER_API
 *
 **/
W_HANDLE PNDEFSendNPPMessageDriver(
      tContext * pContext,
      OPEN_NFC_BUF1_I uint8_t * pBuffer,
      OPEN_NFC_BUF1_LENGTH  uint32_t nBufferLength,
      OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);


/**
 * @keyword  DRIVER_API
 *
 **/
void PRoutingTableReadDriver(
         tContext* pContext,
         OPEN_NFC_BUF1_OA uint8_t * pBuffer,
         OPEN_NFC_BUF1_LENGTH uint32_t nBufferLength,
         OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction * pCallback,
         void * pCallbackParameter );

/**
 * @keyword  DRIVER_API
 *
 **/
void PRoutingTableApplyDriver(
         tContext* pContext,
         OPEN_NFC_BUF1_I uint8_t * pBuffer,
         OPEN_NFC_BUF1_LENGTH uint32_t nBufferLength,
         OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pCallback,
         void * pCallbackParameter );

/**
 * @keyword  DRIVER_API
 *
 **/
uint32_t PRoutingTableGetConfigDriver(
         tContext* pContext );

/**
 * @keyword  DRIVER_API
 *
 **/
void PRoutingTableSetConfigDriver(
         tContext* pContext,
         uint32_t nConfig,
         OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pCallback,
         void * pCallbackParameter );

#endif /* __WME_DRIVER_H */
