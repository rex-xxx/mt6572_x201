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

#ifndef __NFC_HAL_HCI_FRAME_H
#define __NFC_HAL_HCI_FRAME_H

/*******************************************************************************
   Contains the declaration of the HCI frame functions
*******************************************************************************/

/**
 * Type of the function to implement to receive the write completion events.
 *
 * @param[in]   pBindingContext  The context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PHCIFrameWritePrepareContext().
 *
 * @param[in]   nReceptionCounter  The reception counter of the frame
 *              acknowledging the last frame of the message.
 *
 * @see  PHCIFrameWrite().
 **/
typedef void tPHCIFrameWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter );

/**
 * Type of the function to implement to retrieve the data to write.
 *
 * Such a function is called by the implementation of PHCIFrameWriteStream().
 *
 * @pre  The size of the data to write is superior or equal to one.
 *
 * @pre  The value nMessageOffset + nLength is inferior or equal to the message
 *       length specified by PHCIFrameWritePrepareContext().
 *
 * @param[in]   pBindingContext  The context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PHCIFrameWritePrepareContext().
 *
 * @param[in]   pDestinationBuffer  The location of the buffer where to write the data.
 *
 * @param[in]   nMessageOffset  The offset in bytes in the message of the first byte to copy.
 *
 * @param[in]   nLength  The length in byte of the data to write.
 *
 * @see  PHCIFrameWriteStream().
 **/
typedef void tPHCIFrameGetData(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pDestinationBuffer,
         uint32_t nMessageOffset,
         uint32_t nLength );

/**
 * Type of the function to implement to receive the read completion events.
 *
 * @pre  The buffer length nLength is in the range [1, nBufferLength].
 *       Where nBufferLength is the buffer length specified by PHCIFrameRead().
 *
 * A length value of zero means that the reception buffer was too short to receive
 * the message.
 *
 * @param[in]   pBindingContext  The context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PHCIFrameRead().
 *
 * @param[in]   pBuffer  The buffer pointer specified by PHCIFrameRead().
 *
 * @param[in]   nLength  The length in bytes of the data actually read.
 *
 * @param[in]   nHCIMessageReceptionCounter  The reception counter of the first
 *              frame of the message.
 *
 * @see  PHCIFrameRead().
 **/
typedef void tPHCIFrameReadCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

/**
 * Type of the function to implement to receive chunks of data.
 *
 * The chunk of data are sent by the implementation PHCIFrameReadStream().
 *
 * @param[in]   pBindingContext  The context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PHCIFrameReadStream().
 *
 * @param[in]   The offset in bytes from the beginning of the message.
 *              A value of zero means that the function is called for the first chunk of the message.
 *
 * @param[in]   pBuffer  The pointer on the buffer containing the chunk of the message.
 *
 * @param[in]   nLength  The length in bytes of the data actually read.
 *
 * @param[in]   nHCIMessageReceptionCounter  The reception counter of the first frame of the message.
 *              If not zero, the chunk received is the last chunk of the message.
 *              Oteherwise, the value is zero.
 *
 * @see  PHCIFrameReadStream().
 **/
typedef void tPHCIFrameReadDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nOffset,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

/**
 * Describes a HCI write context
 * -----------------------------
 * (!) Do not use directly (!)
 **/
struct __tHCIFrameWriteContext;

typedef struct __tHCIFrameWriteContext
{
   uint8_t                         nPipeIdentifier;
   uint8_t*                        pBuffer;
   uint32_t                        nLength;
   uint32_t                        nCounter;

   tPHCIFrameWriteCompleted*       pCallbackFunction;
   void*                           pCallbackParameter;

   tPHCIFrameGetData*              pSreamCallbackFunction;
   void*                           pStreamCallbackParameter;

   struct __tHCIFrameWriteContext* pNext;
} tHCIFrameWriteContext;

/**
 * Describes a HCI read context
 * ****************************
 * (!) Do not use directly (!)
 ***/
struct __tHCIFrameReadContext;

typedef struct __tHCIFrameReadContext
{
   uint8_t                        nPipeIdentifier;
   uint8_t*                       pBuffer;
   uint32_t                       nBufferLength;
   uint32_t                       nCounter;

   uint32_t nHCIMessageReceptionCounter;

   tPHCIFrameReadCompleted*       pCallbackFunction;
   void*                          pCallbackParameter;

   struct __tHCIFrameReadContext* pNext;
} tHCIFrameReadContext;

/**
 * Describes a HCI frame instance
 * ******************************
 * (!) Do not use directly (!)
 **/
typedef struct __tHCIFrameInstance
{
   tHCIFrameWriteContext*      pWriteContextListHead;
   tHCIFrameWriteContext* pSentWriteContextListHead;
   tHCIFrameReadContext*       pReadContextListHead;

   uint32_t                    nState;

   uint8_t                     aRxPacket[ PSHDLC_PAYLOAD_MAX_SIZE ];
   uint8_t                     aTxPacket[ PSHDLC_PAYLOAD_MAX_SIZE ];

   tPNALGenericCompletion* pCallbackFunction;
   void*                       pCallbackParameter;

   tPNALGenericCompletion* pReconnectIndicationFunction;
   void*                       pReconnectIndicationCallbackParameter;

   uint32_t                    nReadMessageLost;
   uint32_t                    nReadByteErrorCount;
} tHCIFrameInstance;

/**
 * @brief Creates a HCI frame instance.
 *
 * @param[out]  pHCIFrame  The frame instance to initialize.
 **/
NFC_HAL_INTERNAL void PHCIFrameCreate(
         tHCIFrameInstance* pHCIFrame );

/**
 * @brief Destroyes a HCI frame instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every pending read or write operation is cancelled.
 *
 * @post  PHCIFrameDestroy() does not return any error. The caller should always
 *        assume that the instance is destroyed after this call.
 *
 * @post  The caller should never re-use the instance value.
 *
 * @param[in]  pHCIFrame  The HCI frame instance to destroy.
 **/
NFC_HAL_INTERNAL void PHCIFrameDestroy(
         tHCIFrameInstance* pHCIFrame );

/**
 * @brief Prepares the stack for a reset of the NFC Controller.
 *
 * After a call to PHCIFramePreReset(), the communication with the NFC Controller is stopped.
 * The data in the read buffer and in the write buffer is erazed.
 *
 * When the NFC Controller is reset, the stack is restarted with a call to PHCIFrameConnect().
 *
 * @param[in]   pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PHCIFramePreReset(
         tNALBindingContext* pBindingContext );

/**
 * @brief  Connects the stack to the NFC Controller.
 *
 * The callback function is called when the connection is established.
 *
 * @pre  PHCIFrameConnect() is called after the creation of the stack or after
 *       a reset of the NFC Controller.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.

 * @param[in]  pReconnectIndicationFunction  The reconnect indication function
 *
 * @param[in]  pReconnectIndicationCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALGenericCompletion.
 **/
NFC_HAL_INTERNAL void PHCIFrameConnect(
         tNALBindingContext* pBindingContext,
         tPNALGenericCompletion* pCallbackFunction,
         void* pCallbackParameter,
         tPNALGenericCompletion* pReconnectIndicationFunction,
         void* pReconnectIndicationCallbackParameter);

/**
 * @brief  Prepares a context to write a HCI message.
 *
 * The context is used with the functions PHCIFrameWrite() and PHCIFrameWriteStream().
 *
 * @pre  The buffer length nLength is not zero.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[out] pWriteContext  The HCI write context.
 *
 * @param[in]  nPipeIdentifier  The pipe identifier for the message.
 *
 * @param[in]  nLength  The length in bytes of the message (excluding the pipe identifier).
 *
 * @param[in]  pCallbackFunction  The completion callback function (may be null if not needed).
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback functions.
 *
 * @see  tPHCIFrameWriteCompleted, PHCIFrameWrite(), PHCIFrameWriteStream().
 **/
NFC_HAL_INTERNAL void PHCIFrameWritePrepareContext(
         tNALBindingContext* pBindingContext,
         tHCIFrameWriteContext* pWriteContext,
         uint8_t nPipeIdentifier,
         uint32_t nLength,
         tPHCIFrameWriteCompleted* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Writes a HCI message.
 *
 * The completion callback function is called when the message is written.
 *
 * Several write operations may be started at a given time. The order of completion is not specified.
 * If sevral write operations are performed on the same pipe, the order of the operations is unspecified.
 *
 * @post The content of the buffer should be left unchanged while the message is written.
 *
 * @post The write context should be left unchanged until the completion callback is called.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pWriteContext  The write context initialized with PHCIFrameWritePrepareContext().
 *
 * @param[in]  pBuffer  The pointer on the message buffer to write.
 *
 * @see  PHCIFrameWritePrepareContext().
 **/
NFC_HAL_INTERNAL void PHCIFrameWrite(
         tNALBindingContext* pBindingContext,
         tHCIFrameWriteContext* pWriteContext,
         uint8_t* pBuffer );

/**
 * @brief  Writes a HCI message chunk by chunk.
 *
 * The get data callback function pGetDataCallbackFunction is called to read the data chunks.
 *
 * The completion callback function is called when the message is written.
 *
 * The get data callback function may be called during the call to PHCIFrameWriteStream().
 *
 * Several write operations may be started at a given time. The order of completion is not specified.
 * If sevral write operations are performed on the same pipe, the order of the operations is unspecified.
 *
 * @post The write context should be left unchanged until the completion callback is called.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pWriteContext  The write context initialized with PHCIFrameWritePrepareContext().
 *
 * @param[in]  pBuffer  The pointer on the message buffer to write.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPHCIFrameGetData, PHCIFrameWritePrepareContext().
 **/
NFC_HAL_INTERNAL void PHCIFrameWriteStream(
         tNALBindingContext* pBindingContext,
         tHCIFrameWriteContext* pWriteContext,
         tPHCIFrameGetData*     pGetDataCallbackFunction ,
         void*                  pCallbackParameter );

/**
 * @brief  Reads a HCI stream chunk by chunk.
 *
 * The callback function is called for the reception of each chunk.
 *
 * Several read operations may be started at a given time. The order of completion is not specified.
 * If sevral read operations are performed on the same pipe, the order of the operations is unspecified.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pReadContext  The read context.
 *
 * @param[in]  nPipeIdentifier  The pipe identifier.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPHCIFrameReadDataReceived.
 **/
NFC_HAL_INTERNAL void PHCIFrameReadStream(
         tNALBindingContext* pBindingContext,
         tHCIFrameReadContext* pReadContext,
         uint8_t nPipeIdentifier,
         tPHCIFrameReadDataReceived* pCallbackFunction,
         void* pCallbackParameter );

/**
 * Cancels the read stream function started with PHCIFrameReadStream().
 *
 * The callback function is no longer called when PHCIFrameCancelReadStream() returns.
 *
 * If pReadContext is null or if no read operation is pending on this context,
 * the function does nothing and returns.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pReadContext  The read context.
 **/
NFC_HAL_INTERNAL void PHCIFrameCancelReadStream(
         tNALBindingContext* pBindingContext,
         tHCIFrameReadContext* pReadContext );

/**
 * Returns the statistic counters.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[out] pnOSI5ReadMessageLost  A pointer on a variable valued with
 *             the number of HCI messages lost in reception.
 *
 * @param[out] pnOSI5ReadByteErrorCount  A pointer on a variable valued with
 *             the number of bytes in the HCI messages lost in reception.
 **/
NFC_HAL_INTERNAL void PHCIFrameGetStatistics(
         tNALBindingContext* pBindingContext,
         uint32_t* pnOSI5ReadMessageLost,
         uint32_t* pnOSI5ReadByteErrorCount );

/**
 * Resets the statistic counters.
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PHCIFrameResetStatistics(
         tNALBindingContext* pBindingContext);

#endif /* __NFC_HAL_HCI_FRAME_H */
