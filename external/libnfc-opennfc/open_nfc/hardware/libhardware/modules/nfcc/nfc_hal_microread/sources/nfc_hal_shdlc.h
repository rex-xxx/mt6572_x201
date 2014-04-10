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

/*******************************************************************************

  This header file contains the API of the SHDLC stack.

*******************************************************************************/

#ifndef __NFC_HAL_SHDLC_H
#define __NFC_HAL_SHDLC_H

/* The variable P_SHDLC_MAX_WINDOW_SIZE is the maximum window size supported
 * by the implementation. The possible values are 1, 2, 3 or 4.
 * For the current NFC Controller the value 1 is sufficient.
 */
#define P_SHDLC_MAX_WINDOW_SIZE      1

/* The size in bytes of the SHDLC read buffer size is defined by P_SHDLC_READ_BUFFER_SIZE.
 * This value must be a power of 2.
 */
#define P_SHDLC_READ_BUFFER_SIZE  256

/* The variable P_SHDLC_READ_BUFFER_FREE_TRESHOLD defines the threshold in bytes
 * of the free area in the SHDLC read buffer causing the read state machine
 * to send a ACK after an RNR.
 */
#define P_SHDLC_READ_BUFFER_FREE_TRESHOLD   50

/**
 * @brief  Type of the function to implement to receive the write acknoledged events.
 *
 * The write event handler function is called when a frame is acknowledged by the NFC Controller.
 * This call also means that all the previous frames were also acknowledged.
 *
 * The function may not ba called for every frame.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pWriteHandlerParameter  The handler parameter.
 *
 * @param[in]  nSHDLCFrameReceptionCounter  The reception counter of the SHDLC
 *             frame acknowledging a message. 0 means that no message is acknoledged
 *             but the sending buffer is empty.
 *
 * @see  PSHDLCCreate(), PSHDLCWrite().
 **/
typedef void tPSHDLCWriteAcknowledged(
         tNALBindingContext* pBindingContext,
         void* pWriteHandlerParameter,
         uint32_t nSHDLCFrameReceptionCounter);

/**
 * @brief  Type of the function to implement to receive the data ready events.
 *
 * The data ready handler function is called when data is received and acknoledged.
 *
 * The function may not be called for every frame. Several frames may be available.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pDataReadyHandlerParameter  The callback parameter.
 *
 * @see  PSHDLCCreate().
 **/
typedef void tPSHDLCDataReady(
         tNALBindingContext* pBindingContext,
         void* pDataReadyHandlerParameter );

/**
 * @brief Type of the connect callback function.
 *
 * A function of this type receives the connect completion.
 * This function is called when the connect operation is completed.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified to PSHDLCConnect().
 *
 * @see PSHDLCConnect().
 **/
typedef void tPSHDLCConnectCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter  );

/**
 * @brief Type of the SHDLC reset callback function.
 *
 * A function of this type receives the SHDLC reset indication
 * This function is called when the a SHDLC reset is received
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified to PSHDLCConnect().
 *
 * @see PSHDLCConnect().
 **/

typedef void tPSHDLCResetIndication(
         tNALBindingContext* pBindingContext,
         void * pCallbackParameter );

/* The send frame buffer structure - Do not use directly */
typedef struct __tSHDLCSendFrame
{
   uint8_t aData[P_SHDLC_FRAME_MAX_SIZE];
   uint32_t nLength;
} tSHDLCSendFrame;

/* The SHDLC instance - Do not use directly  */
typedef struct __tSHDLCInstance
{
   /* Connect state-machine */
   uint8_t nConnectStatus;
   uint8_t nConnectNextFrameToSend;
   uint16_t nDummy1; /* for padding */
   tPSHDLCConnectCompleted* pConnectCallbackFunction;
   void* pConnectCallbackParameter;

   tPSHDLCResetIndication * pResetIndicationFunction;
   void * pResetIndicationFunctionParameter;

   /* Write state-machine */
   tPSHDLCWriteAcknowledged* pWriteHandler;
   void* pWriteHandlerParameter;
   tSHDLCSendFrame aWriteFrameArray[P_SHDLC_MAX_WINDOW_SIZE];
   uint32_t nWriteLastAckSentFrameId;
   uint32_t nWriteNextToSendFrameId;
   uint32_t nWriteNextWindowFrameId;
   bool_t bIsWriteFramePending;
   bool_t bIsWriteOtherSideReady;
   bool_t bIsWriteBufferFull;
   bool_t bIsRRFrameAlreadySent;
   uint8_t nRRPiggyBackHeader;

   /* Read state-machine */
   tPSHDLCDataReady* pReadHandler;
   void* pReadHandlerParameter;
   uint8_t aReadBuffer[P_SHDLC_READ_BUFFER_SIZE];
   uint32_t nReadNextPositionToRead;
   uint32_t nReadBufferDataLength;
   bool_t bIsReadBufferFull;
   uint32_t nReadNextToReceivedFrameId;
   uint32_t nReadLastAcknowledgedFrameId;
   uint32_t nReadT1Timeout;

   /* Global variables */
   uint32_t nReadPayload;
   uint32_t nReadFrameLost;
   uint32_t nReadByteErrorCount;
   uint32_t nWritePayload;
   uint32_t nWriteFrameLost;
   uint32_t nWriteByteErrorCount;

   uint8_t aReceptionBuffer[ P_SHDLC_FRAME_MAX_SIZE + 1 ];
   uint8_t aSendBuffer[ P_SHDLC_FRAME_MAX_SIZE ];

   uint8_t nNextCtrlFrameToSend;
   uint8_t nWindowSize;
	bool_t  bInReestablishment;
} tSHDLCInstance;

/**
 * @brief Creates a SHDLC stack instance.
 *
 * @param[out]  pSHDLCStack  The SHDLC stack to initialize.
 *
 * @pre  Only one SHDLC stack instance is initialized at a given time.
 **/
NFC_HAL_INTERNAL void PSHDLCCreate(
         tSHDLCInstance* pSHDLCStack );

/**
 * @brief Destroyes a SHDLC stack instance.
 *
 * If the value of \a pSHDLCStack is null, the function does nothing and returns.
 *
 * @pre   The SHDLC stack is not set when PSHDLCDestroy() is called.
 *
 * @post  PSHDLCDestroy() does not return any error. The caller should always
 *        assume that the SHDLC stack instance is destroyed after this call.
 *
 * @post  The caller should never re-use the SHDLC stack instance value \a pSHDLCStack.
 *
 * @param[in]  pSHDLCStack  The SHDLC stack instance to destroy.
 **/
NFC_HAL_INTERNAL void PSHDLCDestroy(
         tSHDLCInstance* pSHDLCStack );

/**
 * @brief Prepares the stack for a reset of the NFC Controller.
 *
 * After a call to PSHDLCPreReset(), the communication with the NFC Controller is stopped.
 * The data in the read buffer and in the write buffer is erazed.
 *
 * When the NFC Controller is reset, the stack is restarted with a call to PSHDLCConnect().
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PSHDLCPreReset(
         tNALBindingContext* pBindingContext );

/**
 * @brief Initiates the connection of a SHDLC stack.
 *
 * PSHDLCConnect() returns immediatelly and the connect operation is performed asynchronously.
 * When the connect operation completes, the callback function is called.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbackFunction  The completion callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter.

 * @param[in]  pResetIndication  The reset indication callback function.
 *
 * @param[in]  pCallbackParameter  The reset indication callback parameter.
 *
 * @param[in]  pWriteAcknowledgedHandler  The pointer on the write acknowledged event handler function.
 *
 * @param[in]  pWriteHandlerParameter  The blind parameter provided unchanged to the write handler function.
 *
 * @param[in]  pDataReadyHandler  The pointer on the data ready handler function.
 *
 * @param[in]  pDataReadyHandlerParameter  The blind parameter provided unchanged to the data ready callback function.
 *
 * @see tPSHDLCConnectCompleted(), tPSHDLCWriteAcknowledged(), tPSHDLCDataReady().
 **/
NFC_HAL_INTERNAL void PSHDLCConnect(
         tNALBindingContext* pBindingContext,
         tPSHDLCConnectCompleted* pCallbackFunction,
         void* pConnectCallbackParameter,
         tPSHDLCResetIndication* pResetIndication,
         void* pResetIndicationCallbackParameter,
         tPSHDLCWriteAcknowledged* pWriteAcknowledgedHandler,
         void* pWriteHandlerParameter,
         tPSHDLCDataReady* pDataReadyHandler,
         void* pDataReadyHandlerParameter );

/**
 * @brief The maximum size in bytes of a SHDLC payload.
 **/
#define PSHDLC_PAYLOAD_MAX_SIZE  (P_SHDLC_FRAME_MAX_SIZE - 1)

/**
 * @brief Initiates a write operation.
 *
 * PSHDLCWrite() does not return an error. The SHDLC protocol is such that the
 * frame are sent until they are acknowledged.
 *
 * @pre  the length \a nFrameBufferLength should be included in the range [1, \ref PSHDLC_PAYLOAD_MAX_SIZE].
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pFrameBuffer  The frame buffer with the data to send.
 *
 * @param[in]  nFrameBufferLength  The length in bytes of the frame buffer.
 *
 * @return  <i>W_TRUE</i> if the frame is recorded to be sent, <i>W_FALSE</i> if the send buffer is full.
 *          The caller should wait for the acknoledgement of some frames before trying again to send this frame.
 *
 * @see  tPSHDLCWriteAcknowledged(), PSHDLCCreate().
 **/
NFC_HAL_INTERNAL bool_t PSHDLCWrite(
         tNALBindingContext* pBindingContext,
         uint8_t* pFrameBuffer,
         uint32_t nFrameBufferLength );

/**
 * @brief Reads the data received by the stack.
 *
 * PSHDLCRead() reads the data already received by the stack. It does not wait
 * for incoming data and returns immediately.
 * The value returned contains the length of the data actually read.
 * This value is in the range [1, \ref PSHDLC_PAYLOAD_MAX_SIZE].
 * If no data is available, the length is zero. PSHDLCRead() does not return an error.
 *
 * If no data is available, the caller should wait for a call to the data ready
 * handler function before trying to read again data. The data ready handler
 * function is registered with PSHDLCCreate().
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pBuffer  The reception buffer. The buffer should be large enough
 *             to receive \ref PSHDLC_PAYLOAD_MAX_SIZE bytes.
 *
 * @param[in]  pnSHDLCFrameReceptionCounter  A pointer on a variable valued with
 *             the SHDLC frame reception counter.
 *
 * @return  The actual length in bytes of the data read.
 **/
NFC_HAL_INTERNAL uint32_t PSHDLCRead(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t* pnSHDLCFrameReceptionCounter );

/**
 * @brief  Returns the statistics of the stack.
 *
 * The statistic counters are rest with PSHDLCResetStatistics().
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[out]  pnOSI4WindowSize  A pointer on a variable valued with
 *              the current SHDLC window size.
 *
 * @param[out]  pnOSI4ReadPayload  A pointer on a variable valued with
 *              the total number of bytes received in the frame payload.
 *
 * @param[out]  pnOSI4ReadFrameLost  A pointer on a variable valued with
 *              the number of SHDLC frames rejected after reception.
 *
 * @param[out]  pnOSI4ReadByteErrorCount  A pointer on a variable valued with
 *              the number of bytes received in dropped frames.
 *
 * @param[out]  pnOSI4WritePayload  A pointer on a variable valued with
 *              the total number of bytes sent in the frame payload.
 *
 * @param[out]  pnOSI4WriteFrameLost  A pointer on a variable valued with
 *              the number of SHDLC frames sent but not acknowledged.
 *
 * @param[out]  pnOSI4WriteByteErrorCount  A pointer on a variable valued with
 *              the number of bytes sent in dropped frames.
 *
 * @see PSHDLCResetStatistics().
 **/
NFC_HAL_INTERNAL void PSHDLCGetStatistics(
         tNALBindingContext* pBindingContext,
         uint32_t* pnOSI4WindowSize,
         uint32_t* pnOSI4ReadPayload,
         uint32_t* pnOSI4ReadFrameLost,
         uint32_t* pnOSI4ReadByteErrorCount,
         uint32_t* pnOSI4WritePayload,
         uint32_t* pnOSI4WriteFrameLost,
         uint32_t* pnOSI4WriteByteErrorCount );

/**
 * @brief  Resets the statistic counters.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @see  PSHDLCGetStatistics().
 **/
NFC_HAL_INTERNAL void PSHDLCResetStatistics(
         tNALBindingContext* pBindingContext);

#endif /* __NFC_HAL_SHDLC_H */
