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

#ifndef __NFC_HAL_SHDLC_FRAME_H
#define __NFC_HAL_SHDLC_FRAME_H

/*******************************************************************************
   Contains the declaration of the SHDLC frame function
*******************************************************************************/

/**
 * Type of the function to implement to receive the read completion events.
 *
 * @pre  The buffer length nLength is in the range [1, P_SHDLC_FRAME_MAX_SIZE].
 *
 * @param[in]   pBindingContext  The context.
 *
 * @param[in]   pCallbackParameter  The callback parameter specified by PSHDLCFrameRead().
 *
 * @param[in]   pBuffer  The buffer pointer specified by PSHDLCFrameRead().
 *
 * @param[in]   nLength  The length in bytes of the data read.
 *
 * @param[in]   nSHDLCFrameReceptionCounter  The SHDLC frame reception counter.
 *
 * @see  PSHDLCFrameRead().
 **/
typedef void tPSHDLCFrameReadCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nSHDLCFrameReceptionCounter);

/** The maximum size in bytes of a SHDLC frame. This is a protocol constant */
#define P_SHDLC_FRAME_MAX_SIZE 30

/* The frame buffer maximum length. This is a protocol constant
 *  Header byte + escaped SHDLC frame + escaped LR + Trailer byte
 */
#define P_SHDLC_FRAME_MAX_BUFFER_SIZE (4 + (2*P_SHDLC_FRAME_MAX_SIZE))

/** Describes a SHDLC frame instance - Do not use directly */
typedef struct __tSHDLCFrameInstance
{
   tNALComInstance* pComPort;
   uint32_t nFrameType;
   uint32_t nReadBufferSize;

   /* Write state machine */
   uint8_t nWritePosition;
   uint8_t nWriteLength;
   uint8_t nWriteStatus;
   uint8_t aWriteFrameBuffer[P_SHDLC_FRAME_MAX_BUFFER_SIZE];
   tPNALGenericCompletion* pWriteCallbackFunction;
   void* pWriteCallbackParameter;

   /* Read state machine */
   uint8_t aReadFrameBuffer[P_SHDLC_FRAME_MAX_BUFFER_SIZE];
   uint32_t nReadFrameBufferLength;
   uint8_t nReadCRC;
   uint8_t nReadPosition;
   uint8_t nPadding;
   uint8_t nReadStatus;

   uint32_t nOSI2FrameReadByteErrorCount;
   uint32_t nOSI2FrameReadByteTotalCount;
   uint32_t nOSI2FrameWriteByteTotalCount;
   uint8_t* pReadBuffer;
   tPSHDLCFrameReadCompleted* pReadCallbackFunction;
   void* pReadCallbackParameter;
   uint32_t nSHDLCFrameReceptionCounter;
} tSHDLCFrameInstance;

/**
 * @brief Creates a SHDLC frame instance.
 *
 * @param[out]  pSHDLCFrame  The frame instance to initialize.
 *
 * @param[in]   pComPort  The com port.
 *
 * @param[in]   nFrameType The frame type value returned by the HAL.
 **/
NFC_HAL_INTERNAL void PSHDLCFrameCreate(
         tSHDLCFrameInstance* pSHDLCFrame,
         tNALComInstance* pComPort,
         uint32_t nFrameType );

/**
 * @brief Destroyes a SHDLC Frame instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  Every pending read or write operation is cancelled.
 *
 * @post  PSHDLCFrameDestroy() does not return any error. The caller should always
 *        assume that the instance is destroyed after this call.
 *
 * @post  The caller should never re-use the instance value.
 *
 * @param[in]  pSHDLCFrame  The SHDLC frame instance to destroy.
 **/
NFC_HAL_INTERNAL void PSHDLCFrameDestroy(
         tSHDLCFrameInstance* pSHDLCFrame );

/**
 * @brief Prepares the stack for a reset of the NFC Controller.
 *
 * After a call to PSHDLCFramePreReset(), the communication with the NFC Controller is stopped.
 * The data in the read buffer and in the write buffer is erazed.
 *
 * When the NFC Controller is reset, the stack is restarted with a call to PSHDLCFramePostReset().
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PSHDLCFramePreReset(
         tNALBindingContext* pBindingContext );

/**
 * @brief  Prepares the stack to be restarted after a reset of the NFC Controller.
 *
 * The function PSHDLCFramePostReset() should be called after
 * PSHDLCFramePreReset() when the NFC Controller is reset.
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PSHDLCFramePostReset(
         tNALBindingContext* pBindingContext );

/**
 * @brief  Writes a frame.
 *
 * The callback function is called when the frame is written.
 *
 * @pre Only one frame is written at a given time.
 *
 * @pre  The buffer length nLength is in the range [1, P_SHDLC_FRAME_MAX_SIZE].
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pBuffer  The pointer on the frame buffer to write.
 *
 * @param[in]  nLength  The length in bytes of the frame buffer.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPNALGenericCompletion().
 **/
NFC_HAL_INTERNAL void PSHDLCFrameWrite(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         tPNALGenericCompletion* pCallbackFunction,
         void* pCallbackParameter );

/**
 * @brief  Reads a frame.
 *
 * The callback function is called when a frame is read.
 *
 * @pre Only one frame is read at a given time.
 *
 * @pre  The buffer length is of at least P_SHDLC_FRAME_MAX_SIZE + 1 bytes.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pBuffer  The pointer on the frame buffer receiving the frame.
 *
 * @param[in]  pCallbackFunction  The callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter provided to the callback function.
 *
 * @see  tPSHDLCFrameReadCompleted().
 **/
NFC_HAL_INTERNAL void PSHDLCFrameRead(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         tPSHDLCFrameReadCompleted* pCallbackFunction,
         void* pCallbackParameter );

/**
 * Returns the statistic counters.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[out] pnOSI2FrameReadByteErrorCount  A pointer on a variable valued with
 *             the number of bytes lost in reception.
 *
 * @param[out] pnOSI2FrameReadByteTotalCount  A pointer on a variable valued with
 *             the total number of bytes received.
 *
 * @param[out] pnOSI2FrameWriteByteTotalCount  A pointer on a variable valued with
 *             the total number of bytes sent.
 **/
NFC_HAL_INTERNAL void PSHDLCFrameGetStatistics(
         tNALBindingContext* pBindingContext,
         uint32_t* pnOSI2FrameReadByteErrorCount,
         uint32_t* pnOSI2FrameReadByteTotalCount,
         uint32_t* pnOSI2FrameWriteByteTotalCount);

/**
 * Resets the statistic counters.
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PSHDLCFrameResetStatistics(
         tNALBindingContext* pBindingContext);

/**
 * Polls to send bytes to the port or to receive bytes from the port.
 *
 * This function is called by the event pump.
 *
 * @param[in]  pBindingContext  The context.
 **/
NFC_HAL_INTERNAL void PSHDLCFramePoll(
         tNALBindingContext* pBindingContext );

#endif /* __NFC_HAL_SHDLC_FRAME_H */
